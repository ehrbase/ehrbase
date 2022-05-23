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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.plugin.EhrBasePluginManagerInterface;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.ExtensionsInjector;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Stefan Spiska
 */
public class EhrBasePluginManager extends SpringPluginManager implements EhrBasePluginManagerInterface {

    private static final Map<String, PropertySourceLoader> PROPERTY_SOURCE_LOADER_MAP = Stream.of(
                    new YamlPropertySourceLoader(),
                    new PropertiesPropertySourceLoader(),
                    new JsonPropertySourceLoader())
            .flatMap(p -> Arrays.stream(p.getFileExtensions()).map(e -> Pair.of(e, p)))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

    private final PluginManagerProperties properties;

    public EhrBasePluginManager(PluginManagerProperties properties) {
        super(properties.getPluginDir());
        this.properties = properties;
    }

    private boolean init = false;

    @Override
    public void init() {
        // Plugins will be initialised in initPlugins
    }

    public void initPlugins() {

        if (!init) {

            startPlugins();

            AbstractAutowireCapableBeanFactory beanFactory =
                    (AbstractAutowireCapableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
            ExtensionsInjector extensionsInjector = new ExtensionsInjector(this, beanFactory);
            extensionsInjector.injectExtensions();
            init = true;
        }
    }

    /**
     * Create a property source from a file <code>fileName</code> in {@link
     * PluginManagerProperties#getPluginDir()}/{@link PluginWrapper#getPluginId()}
     *
     * @param fileName json, yml and properties extensions are supported
     * @param pluginWrapper
     * @return
     */
    protected PropertySource<?> getConfig(String fileName, PluginWrapper pluginWrapper) {

        Path totalPath = Path.of(properties.getPluginConfigDir().toString(), pluginWrapper.getPluginId(), fileName);

        return Optional.of(fileName)
                .map(FilenameUtils::getExtension)
                .map(PROPERTY_SOURCE_LOADER_MAP::get)
                .map(p -> {
                    try {

                        return p.load(fileName, new FileSystemResource(totalPath));
                    } catch (IOException e) {
                        throw new InternalServerException(e);
                    }
                })
                .stream()
                .flatMap(List::stream)
                .findAny()
                .orElseThrow(
                        () -> new InternalServerException(String.format("No Property Source found for %s", totalPath)));
    }

    public List<PropertySource<?>> loadConfig(PluginWrapper pluginWrapper) {
        Path totalPath = Path.of(properties.getPluginConfigDir().toString(), pluginWrapper.getPluginId());

        if (Files.exists(totalPath)) {
            try (Stream<Path> walk = Files.walk(totalPath)) {
                return walk.filter(p ->
                                PROPERTY_SOURCE_LOADER_MAP.keySet().contains(FilenameUtils.getExtension(p.toString())))
                        .map(p -> getConfig(p.getFileName().toString(), pluginWrapper))
                        .collect(Collectors.toList());

            } catch (IOException e) {
                throw new InternalServerException(e);
            }
        }

        return Collections.emptyList();
    }

    public static class JsonPropertySourceLoader extends YamlPropertySourceLoader {
        @Override
        public String[] getFileExtensions() {
            return new String[] {"json"};
        }
    }
}
