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
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Stefan Spiska
 */
public abstract class WebMvcEhrBasePlugin extends SpringPlugin implements PluginWithConfig {

  protected WebMvcEhrBasePlugin(PluginWrapper wrapper) {
    super(wrapper);
  }

  @Override
  protected ApplicationContext createApplicationContext() {
    return getDispatcherServlet().getWebApplicationContext();
  }

  public abstract DispatcherServlet getDispatcherServlet();

  public abstract String getContextPath();
}
