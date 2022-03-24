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

import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Stefan Spiska
 */
public abstract class NonWebMvcEhrBasePlugin extends SpringPlugin implements PluginWithConfig {

  protected NonWebMvcEhrBasePlugin(PluginWrapper wrapper) {
    super(wrapper);
  }

  /**
   * Build the {@link ApplicationContext} of the plugin. Will only be called once by EHRbase. The
   * {@link ApplicationContext} will be refreshed by EHRbase.
   *
   * @return
   */
  protected abstract ApplicationContext buildApplicationContext();

  @Override
  protected final ApplicationContext createApplicationContext() {
    ApplicationContext applicationContext = buildApplicationContext();

    if (applicationContext instanceof ConfigurableApplicationContext
        && !((ConfigurableApplicationContext) applicationContext).isActive()) {

      EhrBasePluginManagerInterface pluginManager =
          (EhrBasePluginManagerInterface) getWrapper().getPluginManager();

      getConfigFileNames()
          .forEach(
              c ->
                  ((ConfigurableApplicationContext) applicationContext)
                      .getEnvironment()
                      .getPropertySources()
                      .addLast(pluginManager.getConfig(c, this.getWrapper())));

      ((ConfigurableApplicationContext) applicationContext).refresh();
    }
    return applicationContext;
  }
}
