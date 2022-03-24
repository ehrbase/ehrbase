/*
 * Copyright (c) 2022. vitasystems GmbH and Hannover Medical School.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ehrbase.plugin;

import java.util.Collections;
import java.util.List;
import org.pf4j.PluginWrapper;

/**
 * @author Stefan Spiska
 */
public interface PluginWithConfig {

  /**
   * List of config file wich will be loaded from the <code>plugin-manager.plugin-config-dir</code>
   * /{@link PluginWrapper#getPluginId()} dir add addet to the plugin environment.
   *
   * <p>json, yml and properties extensions are supported
   *
   * @return
   */
  default List<String> getConfigFileNames() {
    return Collections.emptyList();
  }
}
