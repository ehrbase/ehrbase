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

import org.ehrbase.plugin.EhrBasePlugin;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.UriComponentsBuilder;

import static org.ehrbase.application.config.plugin.PluginManagerProperties.PLUGIN_MANAGER_PREFIX;

/**
 * @author Stefan Spiska
 */
@Configuration
@EnableConfigurationProperties(PluginManagerProperties.class)
@ConditionalOnProperty(prefix = PLUGIN_MANAGER_PREFIX, name = "enable", havingValue = "true")
public class PluginConfig {

  @Bean
  public EhrBasePluginManager pluginManager(Environment environment) {
    // since this is used in a BeanFactoryPostProcessor the PluginManagerProperties must be bind
    // manually.
    BindResult<PluginManagerProperties> result =
        Binder.get(environment).bind(PLUGIN_MANAGER_PREFIX, PluginManagerProperties.class);
    return new EhrBasePluginManager(result.get());
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor(EhrBasePluginManager pluginManager) {

    return beanFactory -> {
      pluginManager.loadPlugins();

      pluginManager.getPlugins().stream()
          .map(PluginWrapper::getPlugin)
          .filter(p -> EhrBasePlugin.class.isAssignableFrom(p.getClass()))
          .map(EhrBasePlugin.class::cast)
          .forEach(p -> register(beanFactory, p));
    };
  }

  private void register(ConfigurableListableBeanFactory beanFactory, EhrBasePlugin p) {
    String pluginId = p.getWrapper().getPluginId();

    final String uri =
        UriComponentsBuilder.newInstance()
            .path("/plugin")
            .path(p.getContextPath())
            .path("/*")
            .build()
            .getPath();

    ServletRegistrationBean<DispatcherServlet> bean =
        new ServletRegistrationBean<>(p.getDispatcherServlet(), uri);

    bean.setLoadOnStartup(1);
    bean.setOrder(1);
    bean.setName(pluginId);
    beanFactory.initializeBean(bean, pluginId);
    beanFactory.autowireBean(bean);
    beanFactory.registerSingleton(pluginId, bean);
  }

  @Bean
  ApplicationListener<ServletWebServerInitializedEvent>
      servletWebServerInitializedEventApplicationListener(EhrBasePluginManager pluginManager) {

    return event -> pluginManager.initPlugins();
  }

}
