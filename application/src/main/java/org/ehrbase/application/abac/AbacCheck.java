/*
 * Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.application.abac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
/*
  This class has only some extracted methods to handle ABAC server connection and requests.
  It is mainly a separate class so it can be overwritten by a MockBean in the context of tests.
 */
public class AbacCheck {

  AbacConfig abacConfig;

  public AbacCheck(AbacConfig abacConfig) {
    this.abacConfig = abacConfig;
  }

  /**
   * Helper to build and send the actual HTTP request to the ABAC server.
   *
   * @param url     URL for ABAC server request
   * @param bodyMap Map of attributes for the request
   * @return HTTP response
   * @throws IOException          On error during attribute or HTTP handling
   * @throws InterruptedException On error during HTTP handling
   */
  public boolean execute(String url, Map<String, String> bodyMap)
      throws IOException, InterruptedException {
    return evaluateResponse(send(url, bodyMap));
  }

  private HttpResponse<?> send(String url, Map<String, String> bodyMap)
      throws IOException, InterruptedException {
    // convert bodyMap to JSON
    ObjectMapper objectMapper = new ObjectMapper();
    String requestBody = objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(bodyMap);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(requestBody))
        .build();

    return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
  }

  private boolean evaluateResponse(HttpResponse<?> response) {
    return response.statusCode() == 200;
  }
}
