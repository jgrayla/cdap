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
package co.cask.cdap.internal.app.runtime.distributed;

import co.cask.cdap.api.spark.SparkSpecification;
import co.cask.cdap.app.program.Program;
import co.cask.cdap.internal.app.runtime.spark.metrics.SparkMetricsSink;
import co.cask.cdap.proto.ProgramType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.twill.api.EventHandler;
import org.apache.twill.api.ResourceSpecification;
import org.apache.twill.api.TwillApplication;
import org.apache.twill.api.TwillSpecification;
import org.apache.twill.filesystem.Location;
import org.apache.twill.internal.ApplicationBundler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * {@link TwillApplication} to run {@link MapReduceTwillRunnable}
 */
public final class SparkTwillApplication extends AbstractProgramTwillApplication {

  static final File SPARK_JAR_FILE =
    new File("/var/spark-1.3.1/assembly/target/scala-2.10/spark-assembly-1.3.1-hadoop2.4.0.jar");

  private final SparkSpecification spec;
  private final Program program;
  private final EventHandler eventHandler;

  public SparkTwillApplication(Program program, SparkSpecification spec,
                               Map<String, File> localizeFiles, EventHandler eventHandler) {
    super(program, localizeFiles, eventHandler);
    this.spec = spec;
    this.program = program;
    this.eventHandler = eventHandler;
  }

  /**
   * Returns type of the program started by this {@link TwillApplication}.
   */
  @Override
  protected ProgramType getType() {
    return ProgramType.SPARK;
  }

  //  @Override
//  public TwillSpecification configure() {
//    // These resources are for the container that runs the spark driver that will launch the actual mapred job.
//    // It does not need much memory.  Memory for mappers and reduces are specified in the MapReduceSpecification,
//    // which is configurable by the author of the job.
//    ResourceSpecification resourceSpec = ResourceSpecification.Builder.with()
//      .setVirtualCores(1)
//      .setMemory(512, ResourceSpecification.SizeUnit.MEGA)
//      .setInstances(1)
//      .build();
//
//    Location programLocation = program.getJarLocation();
//
//    return TwillSpecification.Builder.with()
//      .setName(String.format("%s.%s.%s.%s",
//                             ProgramType.MAPREDUCE.name().toLowerCase(),
//                             program.getNamespaceId(), program.getApplicationId(), spec.getName()))
//      .withRunnable()
//      .add(spec.getName(),
//           new SparkTwillRunnable(spec.getName(), "hConf.xml", "cConf.xml"),
//           resourceSpec)
//      .withLocalFiles()
//      .add(programLocation.getName(), programLocation.toURI())
//      .add("hConf.xml", hConfig.toURI())
//      .add("cConf.xml", cConfig.toURI())
//      .add(SPARK_JAR_FILE.getName(), SPARK_JAR_FILE.toURI()).apply()
//      .anyOrder().withEventHandler(eventHandler).build();
//  }
  @Override
  protected void addRunnables(Map<String, RunnableResource> runnables) {
    // These resources are for the container that runs the mapred client that will launch the actual mapred job.
    // It does not need much memory.  Memory for mappers and reduces are specified in the MapReduceSpecification,
    // which is configurable by the author of the job.
    ResourceSpecification resourceSpec = ResourceSpecification.Builder.with()
      .setVirtualCores(1)
      .setMemory(512, ResourceSpecification.SizeUnit.MEGA)
      .setInstances(1)
      .build();

    runnables.put(spec.getName(), new RunnableResource(
      new SparkTwillRunnable(spec.getName(), "hConf.xml", "cConf.xml"),
      resourceSpec
    ));
  }
}
