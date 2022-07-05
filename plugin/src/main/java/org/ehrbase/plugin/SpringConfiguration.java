package org.ehrbase.plugin;

import org.ehrbase.plugin.repository.KeyValueEntry;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = {KeyValueEntry.class})
@EnableJpaRepositories()
public class SpringConfiguration { }
