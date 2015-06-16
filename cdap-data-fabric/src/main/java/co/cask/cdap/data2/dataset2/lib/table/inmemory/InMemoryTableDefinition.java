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

package co.cask.cdap.data2.dataset2.lib.table.inmemory;

import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.dataset.DatasetContext;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.DatasetSpecification;
import co.cask.cdap.api.dataset.lib.AbstractDatasetDefinition;
import co.cask.cdap.api.dataset.table.ConflictDetection;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.common.conf.CConfiguration;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class InMemoryTableDefinition
  extends AbstractDatasetDefinition<Table, InMemoryTableAdmin> {

  @Inject
  private CConfiguration cConf;

  public InMemoryTableDefinition(String name) {
    super(name);
  }

  @Override
  public DatasetSpecification configure(String name, DatasetProperties properties) {
    return DatasetSpecification.builder(name, getName())
      .properties(properties.getProperties())
      .build();
  }

  @Override
  public Table getDataset(DatasetContext datasetContext, DatasetSpecification spec,
                          Map<String, String> arguments, ClassLoader classLoader) {
    // TODO: refactor common table properties into a common class
    ConflictDetection conflictDetection =
      ConflictDetection.valueOf(spec.getProperty(Table.PROPERTY_CONFLICT_LEVEL, ConflictDetection.ROW.name()));
    String schemaRowField = spec.getProperty(Table.PROPERTY_SCHEMA_ROW_FIELD);
    String schemaStr = spec.getProperty(Table.PROPERTY_SCHEMA);
    Schema schema;
    try {
      schema = schemaStr == null ? null : Schema.parseJson(schemaStr);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid schema", e);
    }
    return new InMemoryTable(datasetContext, spec.getName(), conflictDetection, cConf, schema, schemaRowField);
  }

  @Override
  public InMemoryTableAdmin getAdmin(DatasetContext datasetContext, DatasetSpecification spec,
                                     ClassLoader classLoader) throws IOException {
    // todo: or pass the full spec?
    return new InMemoryTableAdmin(datasetContext, spec.getName(), cConf);
  }
}
