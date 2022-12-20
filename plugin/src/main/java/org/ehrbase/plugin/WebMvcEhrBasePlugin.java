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
package org.ehrbase.plugin;

import org.ehrbase.plugin.security.PluginSecurityConfiguration;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Stefan Spiska
 */
public abstract class WebMvcEhrBasePlugin extends EhrBasePlugin {
    private final Logger log = LoggerFactory.getLogger(WebMvcEhrBasePlugin.class);

    protected WebMvcEhrBasePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    private DispatcherServlet dispatcherServlet;

    @Override
    protected final ApplicationContext createApplicationContext() {
        return getDispatcherServlet().getWebApplicationContext();
    }
    
    public final DispatcherServlet getDispatcherServlet() {
        if (dispatcherServlet == null) {
            dispatcherServlet = buildDispatcherServlet();

            WebApplicationContext applicationContext = dispatcherServlet.getWebApplicationContext();
            initPluginSecurity(applicationContext);

            EhrBasePluginManagerInterface pluginManager = (EhrBasePluginManagerInterface) getWrapper().getPluginManager();

            if (applicationContext instanceof ConfigurableApplicationContext ctx)
                loadProperties(ctx, pluginManager);
        }

        return dispatcherServlet;
    }
    
    private static final String WARN_PLUGIN_SEC = "Can not Configure Plugin Security, check that setting Classloader and Registering of Components is Possible";
    
    private void initPluginSecurity(WebApplicationContext ctx) {
      EhrBasePluginManagerInterface pluginManager = (EhrBasePluginManagerInterface) getWrapper().getPluginManager();
      
      if((ctx instanceof AbstractApplicationContext a1) && (ctx instanceof AnnotationConfigRegistry a2)) {
        a1.setClassLoader(wrapper.getPluginClassLoader());
        a2.register(PluginSecurityConfiguration.class);
        a1.setParent(pluginManager.getApplicationContext());
      } else
        log.warn(WARN_PLUGIN_SEC);
    }

    private static final String WARN_PLUGIN_SEC =
            "Can not Configure Plugin Security, check that setting Classloader and Registering of Components is Possible";

    private void initPluginSecurity(WebApplicationContext ctx) {
        EhrBasePluginManagerInterface pluginManager =
                (EhrBasePluginManagerInterface) getWrapper().getPluginManager();

        if ((ctx instanceof AbstractApplicationContext a1) && (ctx instanceof AnnotationConfigRegistry a2)) {
            a1.setClassLoader(wrapper.getPluginClassLoader());
            a2.register(PluginSecurityConfiguration.class);
            a1.setParent(pluginManager.getApplicationContext());
        } else log.warn(WARN_PLUGIN_SEC);
    }

    /**
     * Build the {@link DispatcherServlet} of the plugin. Will only be called once by EHRbase. The
     * contained {@link ApplicationContext} will be refreshed by EHRbase.
     *
     * @return
     */
    protected abstract DispatcherServlet buildDispatcherServlet();

    /**
     * Context path of the deployed {@link DispatcherServlet}. Relativ to <code>
     * server.servlet.context-path</code>/<code>plugin-manager.plugin-context-path</code>
     *
     * @return
     * @see WebMvcEhrBasePlugin#buildDispatcherServlet()
     */
    public abstract String getContextPath();
}
