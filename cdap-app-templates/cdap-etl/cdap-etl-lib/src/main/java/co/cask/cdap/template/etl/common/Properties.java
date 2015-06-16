/*
 * Copyright © 2015 Cask Data, Inc.
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

package co.cask.cdap.template.etl.common;

/**
 * Class to define property names for source and sinks
 */
public final class Properties {

  /**
   * Class to hold properties for DBSource and DBSink
   */
  public static class DB {
    public static final String CONNECTION_STRING = "connectionString";
    public static final String TABLE_NAME = "tableName";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String COLUMNS = "columns";
    public static final String JDBC_PLUGIN_NAME = "jdbcPluginName";
    public static final String JDBC_PLUGIN_TYPE = "jdbcPluginType";
    public static final String IMPORT_QUERY = "importQuery";
    public static final String COUNT_QUERY = "countQuery";
  }

  /**
   * Properties for the StreamBatchSource as well as the real-time StreamSink
   */
  public static class Stream {
    public static final String NAME = "name";
    public static final String SCHEMA = "schema";
    public static final String FORMAT = "format";
    public static final String DELAY = "delay";
    public static final String DURATION = "duration";
    public static final String BODY_FIELD = "body.field";
    public static final String DEFAULT_BODY_FIELD = "body";
    public static final String HEADERS_FIELD = "headers.field";
    public static final String DEFAULT_HEADERS_FIELD = "headers";
  }

  /**
   * Properties for the TimePartitionedFileSetDatasetAvroSink
   */
  public static class TimePartitionedFileSetDataset {
    public static final String TPFS_NAME = "name";
    public static final String SCHEMA = "schema";
    public static final String BASE_PATH = "basePath";
  }

  /**
   * Properties for KeyValueTables
   */
  public static class KeyValueTable {
    public static final String KEY_FIELD = "key.field";
    public static final String VALUE_FIELD = "value.field";
    public static final String DEFAULT_KEY_FIELD = "key";
    public static final String DEFAULT_VALUE_FIELD = "value";
  }

  /**
   * Properties for Cube
   */
  public static class Cube {
    public static final String NAME = "name";
    public static final String PROPERTY_RESOLUTIONS = co.cask.cdap.api.dataset.lib.cube.Cube.PROPERTY_RESOLUTIONS;
    public static final String MAPPING_CONFIG_PROPERTY = "mapping.config";
    public static final String CUSTOM_PROPERTIES = "custom.properties";
  }

  /**
   * Properties for Tables
   */
  public static class Table {
    public static final String PROPERTY_SCHEMA = co.cask.cdap.api.dataset.table.Table.PROPERTY_SCHEMA;
    public static final String PROPERTY_SCHEMA_ROW_FIELD =
      co.cask.cdap.api.dataset.table.Table.PROPERTY_SCHEMA_ROW_FIELD;
  }

  /**
   * Properties for ProjectionTransform
   */
  public static class ProjectionTransform {
    public static final String DROP = "drop";
    public static final String RENAME = "rename";
    public static final String CONVERT = "convert";
  }

  /**
   * Common properties for BatchWritable source and sinks
   */
  public static class BatchReadableWritable {
    public static final String NAME = "name";
    public static final String TYPE = "type";
  }

  private Properties() {
  }
}
