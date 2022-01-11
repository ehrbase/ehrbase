package org.ehrbase.application.abac;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayTestConfiguration {

  @Bean
  public FlywayMigrationStrategy clean() {
    return flyway -> {
      flyway.clean();
      flyway.migrate();
    };
  }
}
