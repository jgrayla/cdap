/*
 * Copyright © 2014-2015 Cask Data, Inc.
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

package co.cask.cdap.data2.util.hbase;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.data2.increment.hbase94.IncrementHandler;
import co.cask.cdap.data2.transaction.coprocessor.hbase94.DefaultTransactionProcessor;
import co.cask.cdap.data2.transaction.queue.coprocessor.hbase94.DequeueScanObserver;
import co.cask.cdap.data2.transaction.queue.coprocessor.hbase94.HBaseQueueRegionObserver;
import co.cask.cdap.data2.util.TableId;
import co.cask.cdap.proto.Id;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HServerLoad;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.io.hfile.Compression;
import org.apache.hadoop.hbase.regionserver.StoreFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HBase94TableUtil extends HBaseTableUtil {

  private final HTable94NameConverter nameConverter = new HTable94NameConverter();

  @Override
  public HTable createHTable(Configuration conf, TableId tableId) throws IOException {
    Preconditions.checkArgument(tableId != null, "Table id should not be null");
    return new HTable(conf, nameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public HTableDescriptor createHTableDescriptor(TableId tableId) {
    Preconditions.checkArgument(tableId != null, "Table id should not be null");
    return new HTableDescriptor(nameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public HTableDescriptor getHTableDescriptor(HBaseAdmin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    return admin.getTableDescriptor(Bytes.toBytes(nameConverter.toTableName(tablePrefix, tableId)));
  }

  @Override
  public boolean hasNamespace(HBaseAdmin admin, Id.Namespace namespace) throws IOException {
    return true;
  }

  @Override
  public void createNamespaceIfNotExists(HBaseAdmin admin, Id.Namespace namespace) throws IOException {
    // No-op.
  }

  @Override
  public void deleteNamespaceIfExists(HBaseAdmin admin, Id.Namespace namespace) throws IOException {
    // No-op
  }

  @Override
  public void disableTable(HBaseAdmin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    admin.disableTable(nameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public void enableTable(HBaseAdmin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    admin.enableTable(nameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public boolean tableExists(HBaseAdmin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    return admin.tableExists(nameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public void deleteTable(HBaseAdmin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    admin.deleteTable(nameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public void modifyTable(HBaseAdmin admin, HTableDescriptor tableDescriptor) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableDescriptor != null, "Table descriptor should not be null.");
    admin.modifyTable(tableDescriptor.getName(), tableDescriptor);
  }

  @Override
  public List<HRegionInfo> getTableRegions(HBaseAdmin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    return admin.getTableRegions(Bytes.toBytes(nameConverter.toTableName(tablePrefix, tableId)));
  }

  @Override
  public List<TableId> listTablesInNamespace(HBaseAdmin admin, Id.Namespace namespaceId) throws IOException {
    List<TableId> tableIds = Lists.newArrayList();
    for (TableId tableId : listTables(admin)) {
      if (namespaceId.equals(tableId.getNamespace())) {
        tableIds.add(tableId);
      }
    }
    return tableIds;
  }

  @Override
  public List<TableId> listTables(HBaseAdmin admin) throws IOException {
    List<TableId> tableIds = Lists.newArrayList();
    HTableDescriptor[] hTableDescriptors = admin.listTables();
    for (HTableDescriptor hTableDescriptor : hTableDescriptors) {
      if (isCDAPTable(hTableDescriptor)) {
        tableIds.add(nameConverter.from(hTableDescriptor));
      }
    }
    return tableIds;
  }

  @Override
  public void setCompression(HColumnDescriptor columnDescriptor, CompressionType type) {
    switch (type) {
      case LZO:
        columnDescriptor.setCompressionType(Compression.Algorithm.LZO);
        break;
      case SNAPPY:
        columnDescriptor.setCompressionType(Compression.Algorithm.SNAPPY);
        break;
      case GZIP:
        columnDescriptor.setCompressionType(Compression.Algorithm.GZ);
        break;
      case NONE:
        columnDescriptor.setCompressionType(Compression.Algorithm.NONE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported compression type: " + type);
    }
  }

  @Override
  public void setBloomFilter(HColumnDescriptor columnDescriptor, BloomType type) {
    switch (type) {
      case ROW:
        columnDescriptor.setBloomFilterType(StoreFile.BloomType.ROW);
        break;
      case ROWCOL:
        columnDescriptor.setBloomFilterType(StoreFile.BloomType.ROWCOL);
        break;
      case NONE:
        columnDescriptor.setBloomFilterType(StoreFile.BloomType.NONE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported bloom filter type: " + type);
    }
  }

  @Override
  public CompressionType getCompression(HColumnDescriptor columnDescriptor) {
    Compression.Algorithm type = columnDescriptor.getCompressionType();
    switch (type) {
      case LZO:
        return CompressionType.LZO;
      case SNAPPY:
        return CompressionType.SNAPPY;
      case GZ:
        return CompressionType.GZIP;
      case NONE:
        return CompressionType.NONE;
      default:
        throw new IllegalArgumentException("Unsupported compression type: " + type);
    }
  }

  @Override
  public BloomType getBloomFilter(HColumnDescriptor columnDescriptor) {
    StoreFile.BloomType type = columnDescriptor.getBloomFilterType();
    switch (type) {
      case ROW:
        return BloomType.ROW;
      case ROWCOL:
        return BloomType.ROWCOL;
      case NONE:
        return BloomType.NONE;
      default:
        throw new IllegalArgumentException("Unsupported bloom filter type: " + type);
    }
  }

  @Override
  public Class<? extends Coprocessor> getTransactionDataJanitorClassForVersion() {
    return DefaultTransactionProcessor.class;
  }

  @Override
  public Class<? extends Coprocessor> getQueueRegionObserverClassForVersion() {
    return HBaseQueueRegionObserver.class;
  }

  @Override
  public Class<? extends Coprocessor> getDequeueScanObserverClassForVersion() {
    return DequeueScanObserver.class;
  }

  @Override
  public Class<? extends Coprocessor> getIncrementHandlerClassForVersion() {
    return IncrementHandler.class;
  }

  @Override
  public Map<TableId, HBaseTableUtil.TableStats> getTableStats(HBaseAdmin admin) throws IOException {
    // The idea is to walk thru live region servers, collect table region stats and aggregate them towards table total
    // metrics.
    Map<TableId, TableStats> datasetStat = Maps.newHashMap();
    ClusterStatus clusterStatus = admin.getClusterStatus();

    for (ServerName serverName : clusterStatus.getServers()) {
      Map<byte[], HServerLoad.RegionLoad> regionsLoad = clusterStatus.getLoad(serverName).getRegionsLoad();

      for (HServerLoad.RegionLoad regionLoad : regionsLoad.values()) {
        byte[] tableNameInBytes = HRegionInfo.getTableName(regionLoad.getName());
        String tableName = Bytes.toString(tableNameInBytes);
        if (!admin.tableExists(tableName) || !isCDAPTable(admin.getTableDescriptor(tableNameInBytes))) {
          continue;
        }
        HTableNameConverter hTableNameConverter = new HTable94NameConverter();
        TableId tableId = hTableNameConverter.from(new HTableDescriptor(tableName));
        TableStats stat = datasetStat.get(tableId);
        if (stat == null) {
          stat = new TableStats(regionLoad.getStorefileSizeMB(), regionLoad.getMemStoreSizeMB());
          datasetStat.put(tableId, stat);
        } else {
          stat.incStoreFileSizeMB(regionLoad.getStorefileSizeMB());
          stat.incMemStoreSizeMB(regionLoad.getMemStoreSizeMB());
        }
      }
    }
    return datasetStat;
  }
}
