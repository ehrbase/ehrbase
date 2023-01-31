/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginEvent {
  public static PluginEvent of(String type, String identifier) {
    return new PluginEvent(type, identifier);
  }
  
  public static PluginEvent of(String type) {
    return new PluginEvent(type);
  }
  
  private final String type;
  private final String identifier;
  private Map<String,Object> payload = new HashMap<>();
  
  private PluginEvent(String type) {
    this(type, UUID.randomUUID().toString());
  }
  
  private PluginEvent(String type, String identifier) {
    this.type = type;
    this.identifier = identifier;
  }
  
  public void addPayload(String name, Object value) {
    payload.put(name, value);
  }
  
  public String getType() { return type; }
  public String getIdentifier() { return identifier; }
  public Map<String, Object> getPayload() { return payload; }
}