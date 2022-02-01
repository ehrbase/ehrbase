/*
 * Copyright 2021 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;
import org.openehr.schemas.v1.COBJECT;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

/**
 * Utility class that implements basic operations for OPT template.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
public class TemplateUtils {

  public static final List<String> UNSUPPORTED_RM_TYPES = List.of("ITEM_TABLE");

  private TemplateUtils() {
  }

  /**
   * Retrieves the template ID from the given OPT template.
   *
   * @param template the template
   * @return template ID
   */
  public static String getTemplateId(OPERATIONALTEMPLATE template) {
    if (template == null) {
      throw new IllegalArgumentException("Template must not be null");
    }
    return Optional.ofNullable(template.getTemplateId())
        .map(OBJECTID::getValue)
        .orElseThrow(() -> new IllegalArgumentException(
            "Template ID must not be null for the given template"));
  }

  /**
   * Retrieves the template unique ID from the given OPT template.
   *
   * @param template the template
   * @return template unique ID
   */
  public static UUID getUid(OPERATIONALTEMPLATE template) {
    if (template == null) {
      throw new IllegalArgumentException("Template must not be null");
    }
    return Optional.ofNullable(template.getUid())
        .map(OBJECTID::getValue)
        .map(UUID::fromString)
        .orElseThrow(() -> new IllegalArgumentException(
            "Unique ID must not be null for the given template"));
  }

  /**
   * @param template
   * @return
   */
  public static boolean isNotSupported(OPERATIONALTEMPLATE template) {
    if (template == null) {
      return true;
    }
    return hasNotSupportedRmType(template.getDefinition());
  }

  private static boolean hasNotSupportedRmType(COBJECT cobject) {
    if (UNSUPPORTED_RM_TYPES.contains(cobject.getRmTypeName())) {
      return true;
    }

    if (cobject instanceof CCOMPLEXOBJECT) {
      var ccomplexobject = (CCOMPLEXOBJECT) cobject;
      return Arrays.stream(ccomplexobject.getAttributesArray())
          .flatMap(attribute -> Arrays.stream(attribute.getChildrenArray()))
          .anyMatch(TemplateUtils::hasNotSupportedRmType);
    }

    return false;
  }
}
