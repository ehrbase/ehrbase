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
import org.pf4j.spring.ExtensionsInjector;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * @author Stefan Spiska
 */
@Configuration
public class PluginConfig  {



    @Bean
    public EhrbaseSpringPluginManager pluginManager() {
        return new EhrbaseSpringPluginManager();
    }

@Bean
public BeanFactoryPostProcessor beanFactoryPostProcessor(EhrbaseSpringPluginManager pluginManager){

        return beanFactory -> {
            pluginManager.loadPlugins();

            pluginManager.getPlugins().stream()
                    .map(PluginWrapper::getPlugin)
                    .map(EhrBasePlugin.class::cast)
                    .forEach(
                            p -> {
                                String beanName = p.getWrapper().getPluginId();
                                System.out.println(beanName);

                                ServletRegistrationBean bean =  new ServletRegistrationBean(p.getDispatcherServlet(),"/plugin/" + p.getWrapper().getDescriptor().getPluginId() + "/*");

                                bean.setLoadOnStartup(1);
                                bean.setOrder(1);
                                bean.setName(beanName);
                                beanFactory.initializeBean(bean, beanName);
                                beanFactory.autowireBean(bean);
                                beanFactory.registerSingleton(beanName,bean);
                            });

        };
}

@Bean
    ApplicationListener<ServletWebServerInitializedEvent> servletWebServerInitializedEventApplicationListener(EhrbaseSpringPluginManager pluginManager){

        return event -> pluginManager.init2();
}



    public static class EhrbaseSpringPluginManager extends SpringPluginManager {



        public EhrbaseSpringPluginManager() {
            super(Path.of("c:", "plugin"));
        }

        private boolean init = false;

       @Override

        public void init() {

        }

        public void init2() {

      if (!init) {

        startPlugins();

        AbstractAutowireCapableBeanFactory beanFactory =
            (AbstractAutowireCapableBeanFactory)
                getApplicationContext().getAutowireCapableBeanFactory();
        ExtensionsInjector extensionsInjector = new ExtensionsInjector(this, beanFactory);
        extensionsInjector.injectExtensions();
        init = true;
             }

        }
    }


}
