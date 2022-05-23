/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.config.plugin;

import static org.ehrbase.plugin.PluginHelper.PLUGIN_MANAGER_PREFIX;

import java.util.HashMap;
import java.util.Map;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.plugin.WebMvcEhrBasePlugin;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Stefan Spiska
 */
@Configuration
@EnableConfigurationProperties(PluginManagerProperties.class)
@ConditionalOnProperty(prefix = PLUGIN_MANAGER_PREFIX, name = "enable", havingValue = "true")
public class PluginConfig {

    @Bean
    public EhrBasePluginManager pluginManager(Environment environment) {

        return new EhrBasePluginManager(getPluginManagerProperties(environment));
    }
    // since this is used in a BeanFactoryPostProcessor the PluginManagerProperties must be bound
    // manually.
    private PluginManagerProperties getPluginManagerProperties(Environment environment) {
        return Binder.get(environment)
                .bind(PLUGIN_MANAGER_PREFIX, PluginManagerProperties.class)
                .get();
    }

    /** Register the {@link DispatcherServlet} for all {@link WebMvcEhrBasePlugin} */
    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor(
            EhrBasePluginManager pluginManager, Environment environment) {

        PluginManagerProperties pluginManagerProperties = getPluginManagerProperties(environment);

        Map<String, String> registeredUrl = new HashMap<>();

        return beanFactory -> {
            pluginManager.loadPlugins();

            pluginManager.getPlugins().stream()
                    .map(PluginWrapper::getPlugin)
                    .filter(p -> WebMvcEhrBasePlugin.class.isAssignableFrom(p.getClass()))
                    .map(WebMvcEhrBasePlugin.class::cast)
                    .forEach(p -> register(beanFactory, pluginManagerProperties, registeredUrl, p));
        };
    }

    /**
     * Register the {@link DispatcherServlet} for a {@link WebMvcEhrBasePlugin}
     *
     * @param beanFactory
     * @param pluginManagerProperties
     * @param registeredUrl
     * @param p
     */
    private void register(
            ConfigurableListableBeanFactory beanFactory,
            PluginManagerProperties pluginManagerProperties,
            Map<String, String> registeredUrl,
            WebMvcEhrBasePlugin p) {

        String pluginId = p.getWrapper().getPluginId();

        final String uri = UriComponentsBuilder.newInstance()
                .path(pluginManagerProperties.getPluginContextPath())
                .path(p.getContextPath())
                .path("/*")
                .build()
                .getPath();

        // check for duplicate plugin uri
        registeredUrl.entrySet().stream()
                .filter(e -> e.getValue().equals(uri))
                .findAny()
                .ifPresent(e -> {
                    throw new InternalServerException(String.format(
                            "uri %s for plugin %s already registered by plugin %s", uri, pluginId, e.getKey()));
                });

        registeredUrl.put(pluginId, uri);

        ServletRegistrationBean<DispatcherServlet> bean = new ServletRegistrationBean<>(p.getDispatcherServlet(), uri);

        bean.setLoadOnStartup(1);
        bean.setOrder(1);
        bean.setName(pluginId);
        beanFactory.initializeBean(bean, pluginId);
        beanFactory.autowireBean(bean);
        beanFactory.registerSingleton(pluginId, bean);
    }

    /**
     * Create a Listener for the {@link ServletWebServerInitializedEvent } to initialise the {@link
     * org.pf4j.ExtensionPoint} after all {@link DispatcherServlet} have been initialised.
     *
     * @param pluginManager
     * @return
     */
    @Bean
    ApplicationListener<ServletWebServerInitializedEvent> servletWebServerInitializedEventApplicationListener(
            EhrBasePluginManager pluginManager) {

        return event -> pluginManager.initPlugins();
    }
}
