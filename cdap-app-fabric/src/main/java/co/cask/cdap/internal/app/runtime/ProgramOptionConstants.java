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

package co.cask.cdap.internal.app.runtime;

/**
 * Defines constants used across different modules.
 */
public final class ProgramOptionConstants {

  public static final String RUN_ID = "runId";

  public static final String TWILL_RUN_ID = "twillRunId";

  public static final String INSTANCE_ID = "instanceId";

  public static final String INSTANCES = "instances";

  public static final String HOST = "host";

  public static final String LOGICAL_START_TIME = "logicalStartTime";

  public static final String RETRY_COUNT = "retryCount";

  public static final String WORKFLOW_BATCH = "workflowBatch";

  public static final String WORKFLOW_RUN_ID = "workflowRunId";

  public static final String WORKFLOW_NODE_ID = "workflowNodeId";

  public static final String WORKFLOW_NAME = "workflowName";

  public static final String SCHEDULE_NAME = "scheduleName";

  public static final String RUN_DATA_SIZE = "runDataSize";

  public static final String LAST_SCHEDULED_RUN_DATA_SIZE = "lastScheduledRunDataSize";

  public static final String LAST_SCHEDULED_RUN_LOGICAL_START_TIME = "lastScheduledRunLogicalStartTime";

  public static final String RUN_BASE_COUNT_SIZE = "runBaseCountSize";

  public static final String RUN_BASE_COUNT_TIME = "runBaseCountTime";

  public static final String ADAPTER_NAME = "adapterName";

  public static final String ADAPTER_SPEC = "adapterSpec";

  public static final String RESOURCES = "resources";
}
