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

package co.cask.cdap.internal.app.deploy.pipeline.adapter;

import co.cask.cdap.api.dataset.module.DatasetModule;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.internal.app.deploy.pipeline.DatasetModulesDeployer;
import co.cask.cdap.pipeline.AbstractStage;
import co.cask.cdap.proto.Id;
import co.cask.cdap.templates.AdapterDefinition;
import com.google.common.reflect.TypeToken;
import org.apache.twill.filesystem.Location;

/**
 * This {@link co.cask.cdap.pipeline.Stage} is responsible for automatic
 * deploy of the {@link DatasetModule}s specified by an adapter.
 */
public class DeployAdapterDatasetModulesStage extends AbstractStage<AdapterDefinition> {
  private final DatasetModulesDeployer datasetModulesDeployer;
  private final Location templateJarLocation;

  public DeployAdapterDatasetModulesStage(CConfiguration configuration,
                                          Id.Namespace namespace,
                                          Location templateJarLocation,
                                          DatasetFramework datasetFramework,
                                          DatasetFramework inMemoryDatasetFramework) {
    super(TypeToken.of(AdapterDefinition.class));
    this.datasetModulesDeployer = new DatasetModulesDeployer(datasetFramework, inMemoryDatasetFramework,
                                                             namespace, configuration);
    this.templateJarLocation = templateJarLocation;
  }

  /**
   * Deploys dataset modules present in the given adapter spec.
   *
   * @param input An instance of {@link AdapterDefinition}
   */
  @Override
  public void process(AdapterDefinition input) throws Exception {

    datasetModulesDeployer.deployModules(input.getDatasetModules(), templateJarLocation);

    // Emit the input to next stage.
    emit(input);
  }
}
