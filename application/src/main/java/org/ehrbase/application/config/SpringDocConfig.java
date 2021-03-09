package org.ehrbase.application.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringDocConfig implements WebMvcConfigurer {

  @Bean
  public GroupedOpenApi openEhrApi() {
    return GroupedOpenApi.builder()
        .group("1. openEHR API")
        .pathsToMatch("/rest/openehr/**")
        .pathsToExclude("/rest/openehr/v1/admin/**", "/rest/openehr/v1/status")  // TODO: remove that when Admin API is moved to separate path
        .build();
  }

  @Bean
  public GroupedOpenApi ehrScapeApi() {
    return GroupedOpenApi.builder()
        .group("2. EhrScape API")
        .pathsToMatch("/rest/ecis/**")
        .build();
  }

  @Bean
  public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
        .group("4. EHRbase Admin API")
        .pathsToMatch("/rest/openehr/v1/admin/**") // TODO: move Admin API to separate path
        .build();
  }

  @Bean
  public GroupedOpenApi statusAndMetricsApi() {
    return GroupedOpenApi.builder()
        .group("5. EHRbase Status and Metrics API")
        .pathsToMatch("/status/**")
        .build();
  }

  @Bean
  public GroupedOpenApi statusApi() {
    return GroupedOpenApi.builder()
        .group("3. EHRbase Status Endpoint")
        .pathsToMatch("/rest/openehr/v1/status") // TODO: move status API to separate path
        .build();
  }

  @Bean
  public OpenAPI ehrBaseOpenAPI() {
    return new OpenAPI()
        .info(new Info().title("EHRbase API")
            .description("EHRbase implements the [official openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/) and "
                + "a subset of the [EhrScape API](https://www.ehrscape.com/). "
                + "Additionally, EHRbase provides a custom `status` heartbeat endpoint, "
                + "an [Admin API](https://ehrbase.readthedocs.io/en/latest/03_development/07_admin/index.html) (if activated) "
                + "and a [Status and Metrics API](https://ehrbase.readthedocs.io/en/latest/03_development/08_status_and_metrics/index.html?highlight=status) (if activated) "
                + "for monitoring and maintenance. "
                + "Please select the definition in the top right."
                + " "
                + "Note: The openEHR REST API and the EhrScape API are documented in their official documentation, not here. Please refer to their separate documentation.")
            .version("v1")
            .license(new License().name("Apache 2.0").url("https://github.com/ehrbase/ehrbase/blob/develop/LICENSE.md")))
        .externalDocs(new ExternalDocumentation()
            .description("EHRbase Documentation")
            .url("https://ehrbase.readthedocs.io/"));
  }
}
