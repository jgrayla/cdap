/*
 * Copyright 2012-2014 Continuuity, Inc.
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

package com.continuuity.reactor.shell.completer;

import com.continuuity.reactor.shell.util.AsyncSupplier;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import jline.console.completer.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static jline.internal.Preconditions.checkNotNull;

/**
 * Completer for a set of strings.
 */
public class StringsCompleter implements Completer {

  private static final Logger LOG = LoggerFactory.getLogger(StringsCompleter.class);

  private final Supplier<Collection<String>> strings;

  public StringsCompleter(Supplier<Collection<String>> strings) {
    checkNotNull(strings);
    this.strings = Suppliers.memoizeWithExpiration(AsyncSupplier.of(strings), 3, TimeUnit.SECONDS);
  }

  public StringsCompleter(Collection<String> strings) {
    checkNotNull(strings);
    this.strings = Suppliers.ofInstance(strings);
  }

  public TreeSet<String> getStrings() {
    return new TreeSet<String>(strings.get());
  }

  public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
    int result = doComplete(buffer, cursor, candidates);
//    LOG.debug("complete(" + buffer + ", " + cursor + ", [" + Joiner.on(" ").join(candidates) + "]) = " + result);
    return result;
  }

  public int doComplete(final String buffer, final int cursor, final List<CharSequence> candidates) {
    // buffer could be null
    checkNotNull(candidates);

    TreeSet<String> strings = getStrings();
    if (buffer == null) {
      candidates.addAll(strings);
    } else {
      for (String match : strings.tailSet(buffer)) {
        if (!match.startsWith(buffer)) {
          break;
        }

        candidates.add(match);
      }
    }

    if (candidates.size() == 1) {
      candidates.set(0, candidates.get(0) + " ");
    }

    return candidates.isEmpty() ? -1 : 0;
  }
}
