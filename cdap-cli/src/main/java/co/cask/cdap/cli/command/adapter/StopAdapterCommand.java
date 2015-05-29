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

package co.cask.cdap.cli.command.adapter;

import co.cask.cdap.cli.ArgumentName;
import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.cli.ElementType;
import co.cask.cdap.cli.english.Article;
import co.cask.cdap.cli.english.Fragment;
import co.cask.cdap.cli.util.AbstractAuthCommand;
import co.cask.cdap.client.AdapterClient;
import co.cask.cdap.proto.Id;
import co.cask.common.cli.Arguments;
import com.google.inject.Inject;

import java.io.PrintStream;

/**
 * Stops an adapter.
 */
public class StopAdapterCommand extends AbstractAuthCommand {

  private final AdapterClient adapterClient;

  @Inject
  public StopAdapterCommand(AdapterClient adapterClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.adapterClient = adapterClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream output) throws Exception {
    Id.Adapter adapter = Id.Adapter.from(cliConfig.getCurrentNamespace(),
                                         arguments.get(ArgumentName.ADAPTER.toString()));
    adapterClient.stop(adapter);
    output.printf("Successfully stopped %s\n", adapter);
  }

  @Override
  public String getPattern() {
    return String.format("stop adapter <%s>", ArgumentName.ADAPTER);
  }

  @Override
  public String getDescription() {
    return String.format("Stops %s.", Fragment.of(Article.A, ElementType.ADAPTER.getTitleName()));
  }
}
