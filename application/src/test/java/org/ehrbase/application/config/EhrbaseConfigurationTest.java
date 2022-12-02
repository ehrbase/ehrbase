package org.ehrbase.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = {EhrbaseConfiguration.class})
public class EhrbaseConfigurationTest {

  @Autowired
  private EhrbaseConfiguration ehrbaseConfiguration;

  // 'port' is defined in 'application-local.yml'
  // 'local' profile is activated in 'application.yml'
  @Test
  void testConfiguration() {
    int port = ehrbaseConfiguration.getPort();
    assertEquals(8080, port);
  }

}
