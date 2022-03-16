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

package org.ehrbase.application.config.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * @author Stefan Spiska
 *     <p>{@link ConfigurationProperties} for {@link EhrBasePluginManager}.
 */
@ConfigurationProperties(prefix = PluginManagerProperties.PLUGIN_MANAGER_PREFIX)
public class PluginManagerProperties {
  public static final String PLUGIN_MANAGER_PREFIX = "plugin-manager";
  private Path pluginDir;
  private boolean enable;

  public void setPluginDir(Path pluginDir) {
    this.pluginDir = pluginDir;
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public Path getPluginDir() {
    return pluginDir;
  }

  public void setPluginDir(String pluginDir) {
    this.pluginDir = Path.of(pluginDir);
  }
}
