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

import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.cli.ElementType;
import co.cask.cdap.cli.util.AbstractAuthCommand;
import co.cask.cdap.cli.util.RowMaker;
import co.cask.cdap.cli.util.table.Table;
import co.cask.cdap.client.AdapterClient;
import co.cask.cdap.proto.AdapterDetail;
import co.cask.common.cli.Arguments;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.io.PrintStream;
import java.util.List;

/**
 * Lists adapters.
 */
public class ListAdaptersCommand extends AbstractAuthCommand {

  private static final Gson GSON = new Gson();

  private final AdapterClient adapterClient;

  @Inject
  public ListAdaptersCommand(AdapterClient adapterClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.adapterClient = adapterClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream output) throws Exception {
    List<AdapterDetail> list = adapterClient.list(cliConfig.getCurrentNamespace());

    Table table = Table.builder()
      .setHeader("name", "description", "template", "config", "properties")
      .setRows(list, new RowMaker<AdapterDetail>() {
        @Override
        public List<?> makeRow(AdapterDetail object) {
          List<String> properties = Lists.newArrayList();
          if (object.getSchedule() != null) {
            properties.add("schedule=" + GSON.toJson(object.getSchedule()));
          }
          if (object.getInstances() != null) {
            properties.add("instances=" + object.getInstances());
          }
          return Lists.newArrayList(object.getName(), object.getDescription(), object.getTemplate(),
                                    object.getConfig().toString(),
                                    Joiner.on("\n").join(properties));
        }
      }).build();
    cliConfig.getTableRenderer().render(cliConfig, output, table);
  }

  @Override
  public String getPattern() {
    return "list adapters";
  }

  @Override
  public String getDescription() {
    return String.format("Lists all %s.", ElementType.ADAPTER.getTitleNamePlural());
  }
}
