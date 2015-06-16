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

package co.cask.cdap.examples.profiles;

import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.data.stream.Stream;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.dataset.table.ConflictDetection;
import co.cask.cdap.api.dataset.table.Table;

/**
 * Demonstrates the use of column-level conflict detection by the example of managing user profiles,
 * where individual attributes such as name and email address can be updated without conflicting
 * with updates to other attributes such as the last active time of the user. This is achieved
 * by setting the conflict resolution level of the "profiles" table to COLUMN.
 */
public class UserProfiles extends AbstractApplication {

  @Override
  public void configure() {
    setName("UserProfiles");
    setDescription("Demonstrates the use of column-level conflict detection.");
    addStream(new Stream("events"));
    addFlow(new ActivityFlow());
    addService(new UserProfileService());
    createDataset("counters", KeyValueTable.class);

    // create the profiles table with a schema so that it can be explored via Hive
    Schema profileSchema = Schema.recordOf(
      "profile",
      // id, name, and email are never null and are set when a user profile is created
      Schema.Field.of("id", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("name", Schema.of(Schema.Type.STRING)),
      Schema.Field.of("email", Schema.of(Schema.Type.STRING)),
      // login and active are never set when a profile is created but are set later, so they are nullable.
      Schema.Field.of("login", Schema.nullableOf(Schema.of(Schema.Type.LONG))),
      Schema.Field.of("active", Schema.nullableOf(Schema.of(Schema.Type.LONG)))
    );
    createDataset("profiles", Table.class.getName(), DatasetProperties.builder()
      // create the profiles table with column-level conflict detection
      .add(Table.PROPERTY_CONFLICT_LEVEL, ConflictDetection.COLUMN.name())
      .add(Table.PROPERTY_SCHEMA, profileSchema.toString())
      // to indicate that the id field should come from the row key and not a row column
      .add(Table.PROPERTY_SCHEMA_ROW_FIELD, "id")
      .build());
  }
}
