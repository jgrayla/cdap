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

package co.cask.cdap.common.lang;

import co.cask.cdap.proto.ProgramType;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * ClassLoader that implements bundle jar feature, in which the application jar contains
 * its dependency jars inside.
 */
public class ProgramClassLoader extends DirectoryClassLoader {

  /**
   * Constructs an instance that load classes from the given directory, without program type. See
   * {@link ProgramResources#getVisibleResources(ProgramType)} for details on system classes that
   * are visible to the returned ClassLoader.
   */
  public static ProgramClassLoader create(File unpackedJarDir, ClassLoader parentClassLoader) throws IOException {
    return create(unpackedJarDir, parentClassLoader, null);
  }

  /**
   * Constructs an instance that load classes from the given directory for the given program type.
   * <p/>
   * The URLs for class loading are:
   * <p/>
   * <pre>
   * [dir]
   * [dir]/*.jar
   * [dir]/lib/*.jar
   * </pre>
   */
  public static ProgramClassLoader create(File unpackedJarDir, ClassLoader parentClassLoader,
                                          @Nullable ProgramType programType) throws IOException {
    Set<String> visibleResources = ProgramResources.getVisibleResources(programType);
    ImmutableSet.Builder<String> visiblePackages = ImmutableSet.builder();
    for (String resource : visibleResources) {
      if (resource.endsWith(".class")) {
        int idx = resource.lastIndexOf('/');
        // Ignore empty package
        if (idx > 0) {
          visiblePackages.add(resource.substring(0, idx));
        }
      }
    }

    ClassLoader filteredParent = new FilterClassLoader(Predicates.in(visibleResources),
                                                       Predicates.in(visiblePackages.build()),
                                                       parentClassLoader);
    return new ProgramClassLoader(unpackedJarDir, filteredParent);
  }

  private ProgramClassLoader(File dir, ClassLoader parent) {
    super(dir, parent, "lib");
  }
}
