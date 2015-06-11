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

package co.cask.cdap.api.dataset.lib;

import co.cask.cdap.api.annotation.Beta;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Represents a dataset that is split into partitions that can be uniquely addressed
 * by time. Each partition is a path in a file set, with a timestamp attached as meta data.
 * The timestamp is mapped to a partition key of a {@link co.cask.cdap.api.dataset.lib.PartitionedFileSet}
 * with five integer partitioning fields: the year, month, day, hour and minute. Partitions can
 * be retrieved using time range or using a {@link co.cask.cdap.api.dataset.lib.PartitionFilter}.
 * <p>
 * The granularity of time is in minutes, that is, any seconds or milliseconds after the
 * full minute is ignored for the partition keys. That means, there can not be be two partitions
 * in the same minute. Also, when retrieving partitions via time or time range using
 * {@link #getPartitionByTime}, {@link #getPartitionsByTime}, or when writing a partition using
 * {@link #getPartitionOutput}, the seconds and milliseconds on the
 * time or time range are ignored.
 * <p>
 * This dataset can be made available for querying with SQL (explore). This is enabled through dataset
 * properties when the dataset is created. See {@link co.cask.cdap.api.dataset.lib.FileSetProperties}
 * for details. If it is enabled for explore, a Hive external table will be created when the dataset is
 * created. The Hive table is partitioned by year, month, day, hour and minute.
 */
@Beta
public interface TimePartitionedFileSet extends PartitionedFileSet {

  /**
   * Add a partition for a given time, stored at a given path (relative to the file set's base path).
   */
  void addPartition(long time, String path);

  // Why are some of these methods methodNameByTime, while others are just methodName?
  // (why are some suffixed by 'ByTime')
  /**
   * Add a partition for a given time, stored at a given path (relative to the file set's base path),
   * with given properties.
   */
  void addPartition(long time, String path, Map<String, String> properties);

  /**
   * Updates the properties for a particular partition.
   */
  void updateProperties(long time, Map<String, String> properties);

  /**
   * Remove a partition for a given time.
   */
  void dropPartition(long time);

  /**
   * Return the partition associated with the given time, rounded to the minute;
   * or null if no such partition exists.
   */
  @Nullable
  TimePartition getPartitionByTime(long time);

  /**
   * Return all partitions within the time range given by startTime (inclusive) and endTime (exclusive),
   * both rounded to the full minute.
   */
  Set<TimePartition> getPartitionsByTime(long startTime, long endTime);

  /**
   * Return a partition output for a specific time, rounded to the minute, in preparation for creating a new partition.
   * Obtain the location to write from the PartitionOutput, then call the {@link PartitionOutput#addPartition}
   * to add the partition to this dataset.
   */
  TimePartitionOutput getPartitionOutput(long time);

  /**
   * @return the relative path of the partition for a specific time, rounded to the minute.
   */
  @Deprecated
  @Nullable
  String getPartition(long time);

  /**
   * @return a mapping from the partition time to the relative path, of all partitions with a time
   *         that is between startTime (inclusive) and endTime (exclusive), both rounded to the full minute.
   */
  @Deprecated
  Map<Long, String> getPartitions(long startTime, long endTime);

  /**
   * @return the relative paths of all partitions with a time that is between startTime (inclusive)
   *         and endTime (exclusive), both rounded to the full minute.
   */
  @Deprecated
  Collection<String> getPartitionPaths(long startTime, long endTime);

  /**
   * @return the underlying (embedded) file set.
   * @deprecated use {@link #getEmbeddedFileSet} instead.
   */
  @Deprecated
  FileSet getUnderlyingFileSet();
}
