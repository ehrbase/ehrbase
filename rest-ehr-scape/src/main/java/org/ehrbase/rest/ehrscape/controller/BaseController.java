/*
 * Copyright 2019-2022 vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.rest.ehrscape.controller;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * This base controller implements the basic functionality for all specific controllers. This
 * includes error handling and utils.
 *
 * @author Stefan Spiska
 * @author Jake Smolka
 */
public abstract class BaseController {

  public Map<String, Map<String, String>> add2MetaMap(
      Map<String, Map<String, String>> metaMap, String key, String value) {
    Map<String, String> contentMap;

    if (metaMap == null) {
      metaMap = new HashMap<>();
      contentMap = new HashMap<>();
      metaMap.put("meta", contentMap);
    } else {
      contentMap = metaMap.get("meta");
    }

    contentMap.put(key, value);
    return metaMap;
  }

  protected String getBaseEnvLinkURL() {

    return ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
  }
}
