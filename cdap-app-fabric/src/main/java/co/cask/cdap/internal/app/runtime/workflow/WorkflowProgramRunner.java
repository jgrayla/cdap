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
package co.cask.cdap.internal.app.runtime.workflow;

import co.cask.cdap.api.metrics.MetricsCollectionService;
import co.cask.cdap.api.workflow.WorkflowActionSpecification;
import co.cask.cdap.api.workflow.Workflow;
import co.cask.cdap.api.workflow.WorkflowSpecification;
import co.cask.cdap.app.ApplicationSpecification;
import co.cask.cdap.app.program.Program;
import co.cask.cdap.app.runtime.Arguments;
import co.cask.cdap.app.runtime.ProgramController;
import co.cask.cdap.app.runtime.ProgramOptions;
import co.cask.cdap.app.runtime.ProgramRunner;
import co.cask.cdap.app.stream.StreamWriterFactory;
import co.cask.cdap.common.app.RunIds;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.internal.app.runtime.ProgramOptionConstants;
import co.cask.cdap.internal.app.runtime.ProgramRunnerFactory;
import co.cask.cdap.internal.app.runtime.adapter.PluginInstantiator;
import co.cask.cdap.internal.workflow.DefaultWorkflowActionSpecification;
import co.cask.cdap.proto.ProgramType;
import co.cask.cdap.templates.AdapterDefinition;
import co.cask.tephra.TransactionSystemClient;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.api.RunId;
import org.apache.twill.api.ServiceAnnouncer;
import org.apache.twill.discovery.DiscoveryServiceClient;

import java.net.InetAddress;
import javax.annotation.Nullable;

/**
 * A {@link ProgramRunner} that runs a {@link Workflow}
 */
public class WorkflowProgramRunner implements ProgramRunner {
  private static final Gson GSON = new Gson();

  private final ProgramRunnerFactory programRunnerFactory;
  private final ServiceAnnouncer serviceAnnouncer;
  private final InetAddress hostname;

  private final CConfiguration cConf;
  private final MetricsCollectionService metricsCollectionService;
  private final DatasetFramework datasetFramework;
  private final DiscoveryServiceClient discoveryServiceClient;
  private final TransactionSystemClient txClient;
  private final StreamWriterFactory streamWriterFactory;


  @Inject
  public WorkflowProgramRunner(ProgramRunnerFactory programRunnerFactory,
                               ServiceAnnouncer serviceAnnouncer,
                               @Named(Constants.AppFabric.SERVER_ADDRESS) InetAddress hostname,
                               CConfiguration cConf, MetricsCollectionService metricsCollectionService,
                               DatasetFramework datasetFramework, DiscoveryServiceClient discoveryServiceClient,
                               TransactionSystemClient txClient, StreamWriterFactory streamWriterFactory) {
    this.programRunnerFactory = programRunnerFactory;
    this.serviceAnnouncer = serviceAnnouncer;
    this.hostname = hostname;
    this.cConf = cConf;
    this.metricsCollectionService = metricsCollectionService;
    this.datasetFramework = datasetFramework;
    this.discoveryServiceClient = discoveryServiceClient;
    this.txClient = txClient;
    this.streamWriterFactory = streamWriterFactory;
  }

  @Override
  public ProgramController run(Program program, ProgramOptions options) {
    // Extract and verify options
    ApplicationSpecification appSpec = program.getApplicationSpecification();
    Preconditions.checkNotNull(appSpec, "Missing application specification.");

    ProgramType processorType = program.getType();
    Preconditions.checkNotNull(processorType, "Missing processor type.");
    Preconditions.checkArgument(processorType == ProgramType.WORKFLOW, "Only WORKFLOW process type is supported.");

    WorkflowSpecification workflowSpec = appSpec.getWorkflows().get(program.getName());
    Preconditions.checkNotNull(workflowSpec, "Missing WorkflowSpecification for %s", program.getName());

    // Controller needs to be created before starting the driver so that the state change of the driver
    // service can be fully captured by the controller.
    RunId runId = RunIds.fromString(options.getArguments().getOption(ProgramOptionConstants.RUN_ID));

    AdapterDefinition adapterSpec = getAdapterSpecification(options.getArguments());

    WorkflowActionSpecification newActionSpec = new DefaultWorkflowActionSpecification(workflowSpec.getClassName(),
                                                                                       workflowSpec.getName(),
                                                                                       workflowSpec.getDescription(),
                                                                                       workflowSpec.getProperties());

    BasicWorkflowActionContext context =
      new BasicWorkflowActionContext(newActionSpec, program, runId, 1, options.getUserArguments(), cConf,
                                     metricsCollectionService, datasetFramework, txClient, discoveryServiceClient,
                                     streamWriterFactory, adapterSpec,
                                     createPluginInstantiator(adapterSpec, program.getClassLoader()));

    WorkflowDriver driver = new WorkflowDriver(program, options, hostname, workflowSpec, programRunnerFactory, context);

    ProgramController controller = new WorkflowProgramController(program, driver, serviceAnnouncer, runId);
    driver.start();

    return controller;
  }

  @Nullable
  private AdapterDefinition getAdapterSpecification(Arguments arguments) {
    // TODO: Refactor ProgramRunner class hierarchy to have common logic moved to a common parent.
    if (!arguments.hasOption(ProgramOptionConstants.ADAPTER_SPEC)) {
      return null;
    }
    return GSON.fromJson(arguments.getOption(ProgramOptionConstants.ADAPTER_SPEC), AdapterDefinition.class);
  }

  @Nullable
  private PluginInstantiator createPluginInstantiator(@Nullable AdapterDefinition adapterSpec,
                                                      ClassLoader programClassLoader) {
    // TODO: Refactor ProgramRunner class hierarchy to have common logic moved to a common parent.
    if (adapterSpec == null) {
      return null;
    }
    return new PluginInstantiator(cConf, adapterSpec.getTemplate(), programClassLoader);
  }
}
