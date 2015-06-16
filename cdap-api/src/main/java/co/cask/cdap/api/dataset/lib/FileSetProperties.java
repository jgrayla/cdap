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

package co.cask.cdap.api.dataset.lib;

import co.cask.cdap.api.dataset.DatasetProperties;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

/**
 * Helper to build properties for files datasets.
 */
public class FileSetProperties {

  /**
   * The base path of the dataset.
   */
  public static final String BASE_PATH = "base.path";

  /**
   * The name of the input format class.
   */
  public static final String INPUT_FORMAT = "input.format";

  /**
   * The name of the output format class.
   */
  public static final String OUTPUT_FORMAT = "output.format";

  /**
   * Prefix for additional properties for the input format. They are added to the
   * Hadoop configuration, with the prefix stripped from the name.
   */
  public static final String INPUT_PROPERTIES_PREFIX = "input.properties.";

  /**
   * Prefix for additional properties for the output format. They are added to the
   * Hadoop configuration, with the prefix stripped from the name.
   */
  public static final String OUTPUT_PROPERTIES_PREFIX = "output.properties.";

  /**
   * Whether this dataset should be enabled for explore.
   */
  public static final String PROPERTY_ENABLE_EXPLORE_ON_CREATE = "explore.enabled";

  /**
   * The format to use for the explore table. Currently, only text is supported.
   */
  public static final String PROPERTY_EXPLORE_FORMAT = "explore.format";

  /**
   * The schema to use for the explore table. This should have the form: column type, ...
   */
  public static final String PROPERTY_EXPLORE_SCHEMA = "explore.schema";

  /**
   * The serde to use for the Hive table.
   */
  public static final String PROPERTY_EXPLORE_SERDE = "explore.serde";

  /**
   * The input format to use for the Hive table.
   */
  public static final String PROPERTY_EXPLORE_INPUT_FORMAT = "explore.input.format";

  /**
   * The output format to use for the Hive table.
   */
  public static final String PROPERTY_EXPLORE_OUTPUT_FORMAT = "explore.output.format";

  /**
   * Prefix used to store additional table properties for Hive.
   */
  public static final String PROPERTY_EXPLORE_TABLE_PROPERTY_PREFIX = "explore.table.property.";

  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return The base path configured in the properties.
   */
  public static String getBasePath(Map<String, String> properties) {
    return properties.get(BASE_PATH);
  }

  /**
   * @return The input format configured in the properties.
   */
  public static String getInputFormat(Map<String, String> properties) {
    return properties.get(INPUT_FORMAT);
  }

  /**
   * @return The output format configured in the properties.
   */
  public static String getOutputFormat(Map<String, String> properties) {
    return properties.get(OUTPUT_FORMAT);
  }

  /**
   * @return The input format properties configured in the properties.
   */
  public static Map<String, String> getInputProperties(Map<String, String> properties) {
    return propertiesWithPrefix(properties, INPUT_PROPERTIES_PREFIX);
  }

  /**
   * @return The output format properties configured in the properties.
   */
  public static Map<String, String> getOutputProperties(Map<String, String> properties) {
    return propertiesWithPrefix(properties, OUTPUT_PROPERTIES_PREFIX);
  }

  /**
   * @return whether explore is enabled by the properties.
   */
  public static boolean isExploreEnabled(Map<String, String> properties) {
    // Boolean.valueOf returns false if the value is null
    return Boolean.valueOf(properties.get(PROPERTY_ENABLE_EXPLORE_ON_CREATE));
  }

  /**
   * @return the format of the explore table.
   */
  public static String getExploreFormat(Map<String, String> properties) {
    return properties.get(PROPERTY_EXPLORE_FORMAT);
  }

  /**
   * @return the schema of the explore table.
   */
  public static String getExploreSchema(Map<String, String> properties) {
    return properties.get(PROPERTY_EXPLORE_SCHEMA);
  }

  /**
   * @return the properties for the explore format
   */
  public static Map<String, String> getExploreFormatProperties(Map<String, String> properties) {
    String format = getExploreFormat(properties);
    if (format == null) {
      return Collections.emptyMap();
    }
    return propertiesWithPrefix(properties, String.format("%s.%s.", PROPERTY_EXPLORE_FORMAT, format));
  }

  /**
   * @return the class name of the serde configured in the properties.
   */
  public static String getSerDe(Map<String, String> properties) {
    return properties.get(PROPERTY_EXPLORE_SERDE);
  }

  /**
   * @return the class name of the input format to be used in Hive.
   * Note that this can be different than the input format used
   * for the file set itself.
   */
  public static String getExploreInputFormat(Map<String, String> properties) {
    return properties.get(PROPERTY_EXPLORE_INPUT_FORMAT);
  }

  /**
   * @return the class name of the output format to be used in Hive.
   * Note that this can be different than the output format used
   * for the file set itself.
   */
  public static String getExploreOutputFormat(Map<String, String> properties) {
    return properties.get(PROPERTY_EXPLORE_OUTPUT_FORMAT);
  }

  /**
   * @return the Hive table properties configured in the properties.
   */
  public static Map<String, String> getTableProperties(Map<String, String> properties) {
    return propertiesWithPrefix(properties, PROPERTY_EXPLORE_TABLE_PROPERTY_PREFIX);
  }

  /**
   * @return a map of all properties whose key begins with the given prefix, without that prefix.
   */
  public static Map<String, String> propertiesWithPrefix(Map<String, String> properties, String prefix) {
    Map<String, String> result = Maps.newHashMap();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        result.put(entry.getKey().substring(prefix.length()), entry.getValue());
      }
    }
    return result;
  }

  /**
   * A Builder to construct properties for FileSet datasets.
   */
  public static class Builder extends DatasetProperties.Builder {

    private String format = null;

    /**
     * Package visible default constructor, to allow sub-classing by other datasets in this package.
     */
    Builder() { }

    /**
     * Sets the base path for the file dataset.
     */
    public Builder setBasePath(String path) {
      add(BASE_PATH, path);
      return this;
    }

    /**
     * Sets the output format of the file dataset.
     */
    public Builder setOutputFormat(Class<?> outputFormatClass) {
      setOutputFormat(outputFormatClass.getName());
      return this;
    }

    /**
     * Sets the output format of the file dataset.
     */
    public Builder setOutputFormat(String className) {
      add(OUTPUT_FORMAT, className);
      return this;
    }

    /**
     * Sets the input format of the file dataset.
     */
    public Builder setInputFormat(Class<?> inputFormatClass) {
      setInputFormat(inputFormatClass.getName());
      return this;
    }

    /**
     * Sets the input format of the file dataset.
     */
    public Builder setInputFormat(String className) {
      add(INPUT_FORMAT, className);
      return this;
    }

    /**
     * Sets a property for the input format of the file dataset.
     */
    public Builder setInputProperty(String name, String value) {
      add(INPUT_PROPERTIES_PREFIX + name, value);
      return this;
    }

    /**
     * Sets a property for the output format of the file dataset.
     */
    public Builder setOutputProperty(String name, String value) {
      add(OUTPUT_PROPERTIES_PREFIX + name, value);
      return this;
    }

    /**
     * Enable explore for this dataset.
     */
    public Builder setEnableExploreOnCreate(boolean enabled) {
      add(PROPERTY_ENABLE_EXPLORE_ON_CREATE, Boolean.toString(enabled));
      return this;
    }

    /**
     * Set the format for the Hive table.
     * @param format currently, only "text" and "csv" are supported.
     */
    public Builder setExploreFormat(String format) {
      add(PROPERTY_EXPLORE_FORMAT, format);
      this.format = format;
      return this;
    }

    /**
     * Set the schema for the Hive table.
     * @param schema a Hive schema string of the form: field type, ...
     */
    public Builder setExploreSchema(String schema) {
      add(PROPERTY_EXPLORE_SCHEMA, schema);
      return this;
    }

    /**
     * Set a property for the table format.
     * This may only be a called after setting the format using {@link #setExploreFormat}.
     */
    public Builder setExploreFormatProperty(String name, String value) {
      if (format == null) {
        throw new IllegalStateException("explore format has not been set");
      }
      add(String.format("%s.%s.%s", PROPERTY_EXPLORE_FORMAT, format, name), value);
      return this;
    }

    /**
     * Set the class name of the SerDe used to create the Hive table.
     */
    public Builder setSerDe(String className) {
      add(PROPERTY_EXPLORE_SERDE, className);
      return this;
    }

    /**
     * Set the class name of the SerDe used to create the Hive table.
     */
    public Builder setSerDe(Class<?> serde) {
      return setSerDe(serde.getName());
    }

    /**
     * Set the input format used to create the Hive table.
     * Note that this can be different than the input format used
     * for the file set itself.
     */
    public Builder setExploreInputFormat(String className) {
      add(PROPERTY_EXPLORE_INPUT_FORMAT, className);
      return this;
    }

    /**
     * Set the input format used to create the Hive table.
     * Note that this can be different than the input format used
     * for the file set itself.
     */
    public Builder setExploreInputFormat(Class<?> inputFormat) {
      return setExploreInputFormat(inputFormat.getName());
    }

    /**
     * Set the output format used to create the Hive table.
     * Note that this can be different than the output format used
     * for the file set itself.
     */
    public Builder setExploreOutputFormat(String className) {
      add(PROPERTY_EXPLORE_OUTPUT_FORMAT, className);
      return this;
    }

    /**
     * Set the output format used to create the Hive table.
     * Note that this can be different than the output format used
     * for the file set itself.
     */
    public Builder setExploreOutputFormat(Class<?> outputFormat) {
      return setExploreOutputFormat(outputFormat.getName());
    }

    /**
     * Set a table property to be added to the Hive table. Multiple properties can be set.
     */
    public Builder setTableProperty(String name, String value) {
      add(PROPERTY_EXPLORE_TABLE_PROPERTY_PREFIX + name, value);
      return this;
    }

    /**
     * Create a DatasetProperties from this builder.
     */
    public DatasetProperties build() {
      return super.build();
    }
  }
}
