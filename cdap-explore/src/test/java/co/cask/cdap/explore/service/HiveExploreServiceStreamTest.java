/*
  * Copyright © 2014 Cask Data, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */

package co.cask.cdap.explore.service;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.format.FormatSpecification;
import co.cask.cdap.api.data.format.Formats;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.explore.client.ExploreExecutionResult;
import co.cask.cdap.proto.ColumnDesc;
import co.cask.cdap.proto.Id;
import co.cask.cdap.proto.QueryResult;
import co.cask.cdap.proto.StreamProperties;
import co.cask.cdap.test.SlowTests;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 */
@Category(SlowTests.class)
public class HiveExploreServiceStreamTest extends BaseHiveExploreServiceTest {
  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  private static final Gson GSON = new Gson();
  private static final String body1 = "userX,actionA,item123";
  private static final String body2 = "userY,actionB,item123";
  private static final String body3 = "userZ,actionA,item456";
  private static final String streamName = "mystream";
  private static final String streamTableName = getTableName(streamName);
  // headers must be prefixed with the stream name, otherwise they are filtered out.
  private static final Map<String, String> headers = ImmutableMap.of("header1", "val1", "header2", "val2");
  private static final Type headerType = new TypeToken<Map<String, String>>() { }.getType();

  @BeforeClass
  public static void start() throws Exception {
    // use leveldb implementations, since stream input format examines the filesystem
    // to determine input splits.
    initialize(CConfiguration.create(), tmpFolder, true);

    Id.Stream streamId = Id.Stream.from(NAMESPACE_ID, streamName);
    createStream(streamId);
    sendStreamEvent(streamId, headers, Bytes.toBytes(body1));
    sendStreamEvent(streamId, headers, Bytes.toBytes(body2));
    sendStreamEvent(streamId, headers, Bytes.toBytes(body3));
  }

  @Test
  public void testStreamDefaultSchema() throws Exception {
    runCommand(NAMESPACE_ID, "describe " + streamTableName,
               true,
               Lists.newArrayList(
                 new ColumnDesc("col_name", "STRING", 1, "from deserializer"),
                 new ColumnDesc("data_type", "STRING", 2, "from deserializer"),
                 new ColumnDesc("comment", "STRING", 3, "from deserializer")
               ),
               Lists.newArrayList(
                 new QueryResult(Lists.<Object>newArrayList("ts", "bigint", "from deserializer")),
                 new QueryResult(Lists.<Object>newArrayList("headers", "map<string,string>",
                                                            "from deserializer")),
                 new QueryResult(Lists.<Object>newArrayList("body", "string", "from deserializer"))
               )
    );
  }

  @Test
  public void testSelectStarOnStream() throws Exception {
    ExploreExecutionResult results = exploreClient.submit(NAMESPACE_ID, "select * from " + streamTableName).get();
    // check schema
    List<ColumnDesc> expectedSchema = Lists.newArrayList(
      new ColumnDesc(streamTableName + ".ts", "BIGINT", 1, null),
      new ColumnDesc(streamTableName + ".headers", "map<string,string>", 2, null),
      new ColumnDesc(streamTableName + ".body", "STRING", 3, null)
    );
    Assert.assertEquals(expectedSchema, results.getResultSchema());
    // check each result, without checking timestamp since that changes for each test
    // first result
    List<Object> columns = results.next().getColumns();
    // maps are returned as json objects...
    Assert.assertEquals(headers, GSON.fromJson((String) columns.get(1), headerType));
    Assert.assertEquals(body1, columns.get(2));
    // second result
    columns = results.next().getColumns();
    Assert.assertEquals(headers, GSON.fromJson((String) columns.get(1), headerType));
    Assert.assertEquals(body2, columns.get(2));
    // third result
    columns = results.next().getColumns();
    Assert.assertEquals(headers, GSON.fromJson((String) columns.get(1), headerType));
    Assert.assertEquals(body3, columns.get(2));
    // should not be any more
    Assert.assertFalse(results.hasNext());
  }

  @Test
  public void testSelectFieldOnStream() throws Exception {
    runCommand(NAMESPACE_ID, "select body from " + streamTableName,
               true,
               Lists.newArrayList(new ColumnDesc("body", "STRING", 1, null)),
               Lists.newArrayList(
                 new QueryResult(Lists.<Object>newArrayList(body1)),
                 new QueryResult(Lists.<Object>newArrayList(body2)),
                 new QueryResult(Lists.<Object>newArrayList(body3)))
    );

    runCommand(NAMESPACE_ID,
               "select headers[\"header1\"] as h1, headers[\"header2\"] as h2 from " + streamTableName,
               true,
               Lists.newArrayList(new ColumnDesc("h1", "STRING", 1, null),
                                  new ColumnDesc("h2", "STRING", 2, null)),
               Lists.newArrayList(
                 new QueryResult(Lists.<Object>newArrayList("val1", "val2")),
                 new QueryResult(Lists.<Object>newArrayList("val1", "val2")),
                 new QueryResult(Lists.<Object>newArrayList("val1", "val2")))
    );
  }

  @Test
  public void testSelectAndFilterQueryOnStream() throws Exception {
    runCommand(NAMESPACE_ID, "select body from " + streamTableName + " where ts > " + Long.MAX_VALUE,
               false,
               Lists.newArrayList(new ColumnDesc("body", "STRING", 1, null)),
               Lists.<QueryResult>newArrayList());
  }

  @Test
  public void testStreamNameWithHyphen() throws Exception {
    Id.Stream streamId = Id.Stream.from(NAMESPACE_ID, "stream-test");
    createStream(streamId);
    sendStreamEvent(streamId, Collections.<String, String>emptyMap(), Bytes.toBytes("Dummy"));

    // Streams with '-' are replaced with '_'
    String cleanStreamName = "stream_test";

    runCommand(NAMESPACE_ID, "select body from " + getTableName(cleanStreamName), true,
               Lists.newArrayList(new ColumnDesc("body", "STRING", 1, null)),
               Lists.newArrayList(new QueryResult(Lists.<Object>newArrayList("Dummy"))));
  }


  @Test
  public void testJoinOnStreams() throws Exception {
    Id.Stream streamId1 = Id.Stream.from(NAMESPACE_ID, "jointest1");
    Id.Stream streamId2 = Id.Stream.from(NAMESPACE_ID, "jointest2");
    createStream(streamId1);
    createStream(streamId2);
    sendStreamEvent(streamId1, Collections.<String, String>emptyMap(), Bytes.toBytes("ABC"));
    sendStreamEvent(streamId1, Collections.<String, String>emptyMap(), Bytes.toBytes("XYZ"));
    sendStreamEvent(streamId2, Collections.<String, String>emptyMap(), Bytes.toBytes("ABC"));
    sendStreamEvent(streamId2, Collections.<String, String>emptyMap(), Bytes.toBytes("DEF"));

    runCommand(NAMESPACE_ID,
               "select " + getTableName(streamId1) + ".body, " + getTableName(streamId2) + ".body" +
                 " from " + getTableName(streamId1) + " join " + getTableName(streamId2) +
                 " on (" + getTableName(streamId1) + ".body = " + getTableName(streamId2) + ".body)",
               true,
               Lists.newArrayList(new ColumnDesc(getTableName(streamId1) + ".body", "STRING", 1, null),
                                  new ColumnDesc(getTableName(streamId2) + ".body", "STRING", 2, null)),
               Lists.newArrayList(new QueryResult(Lists.<Object>newArrayList("ABC", "ABC")))
    );
  }

  @Test(expected = ExecutionException.class)
  public void testWriteToStreamFails() throws Exception {
    exploreClient.submit(NAMESPACE_ID,
                         "insert into table " + streamTableName + " select * from " + streamTableName).get();
  }

  @Test
  public void testAvroFormattedStream() throws Exception {
    Id.Stream streamId = Id.Stream.from(NAMESPACE_ID, "avroStream");
    createStream(streamId);
    Schema schema = Schema.recordOf(
      "purchase",
      Schema.Field.of("user", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("num", Schema.of(Schema.Type.INT)),
      Schema.Field.of("price", Schema.of(Schema.Type.DOUBLE))
    );
    FormatSpecification formatSpecification = new FormatSpecification(
      Formats.AVRO, schema, Collections.<String, String>emptyMap());
    StreamProperties properties = new StreamProperties(Long.MAX_VALUE, formatSpecification, 1000);
    setStreamProperties(NAMESPACE_ID.getId(), "avroStream", properties);

    // our schemas are compatible
    org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schema.toString());
    sendStreamEvent(streamId, createAvroEvent(avroSchema, "userX", 5, 3.14));
    sendStreamEvent(streamId, createAvroEvent(avroSchema, "userX", 10, 2.34));
    sendStreamEvent(streamId, createAvroEvent(avroSchema, "userY", 1, 1.23));
    sendStreamEvent(streamId, createAvroEvent(avroSchema, "userZ", 50, 45.67));
    sendStreamEvent(streamId, createAvroEvent(avroSchema, "userZ", 100, 98.76));

    Double xPrice = 5 * 3.14 + 10 * 2.34;
    Double yPrice = 1.23;
    Double zPrice = 50 * 45.67 + 100 * 98.76;


    ExploreExecutionResult result = exploreClient.submit(
      NAMESPACE_ID,
      "SELECT user, sum(num) as total_num, sum(price * num) as total_price " +
        "FROM " + getTableName(streamId) + " GROUP BY user ORDER BY total_price DESC").get();

    Assert.assertTrue(result.hasNext());
    Assert.assertEquals(
      Lists.newArrayList(new ColumnDesc("user", "STRING", 1, null),
                         new ColumnDesc("total_num", "BIGINT", 2, null),
                         new ColumnDesc("total_price", "DOUBLE", 3, null)),
      result.getResultSchema());

    // should get 3 rows
    // first row should be for userZ
    List<Object> rowColumns = result.next().getColumns();
    // toString b/c avro returns a utf8 object for strings
    Assert.assertEquals("userZ", rowColumns.get(0).toString());
    Assert.assertEquals(150L, rowColumns.get(1));
    Assert.assertTrue(Math.abs(zPrice - (Double) rowColumns.get(2)) < 0.0000001);

    // 2nd row, should be userX
    rowColumns = result.next().getColumns();
    Assert.assertEquals("userX", rowColumns.get(0).toString());
    Assert.assertEquals(15L, rowColumns.get(1));
    Assert.assertTrue(Math.abs(xPrice - (Double) rowColumns.get(2)) < 0.0000001);

    // 3rd row, should be userY
    rowColumns = result.next().getColumns();
    Assert.assertEquals("userY", rowColumns.get(0).toString());
    Assert.assertEquals(1L, rowColumns.get(1));
    Assert.assertTrue(Math.abs(yPrice - (Double) rowColumns.get(2)) < 0.0000001);

    // shouldn't be any more results
    Assert.assertFalse(result.hasNext());
  }

  private static String getTableName(Id.Stream streamId) {
    return getTableName(streamId.getId());
  }

  private static String getTableName(String streamName) {
    return "stream_" + streamName;
  }

  private byte[] createAvroEvent(org.apache.avro.Schema schema, Object... values) throws IOException {
    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    int i = 0;
    for (org.apache.avro.Schema.Field field : schema.getFields()) {
      builder.set(field.name(), values[i]);
      i++;
    }
    GenericRecord record = builder.build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
    DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);

    writer.write(record, encoder);
    encoder.flush();
    out.close();
    return out.toByteArray();
  }
}
