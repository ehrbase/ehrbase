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

package org.ehrbase.plugin.extensionpoints;

import java.util.function.Function;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

/**
 * Provides After and Before Interceptors for {@link TemplateExtensionPoint}
 *
 * @author Stefan Spiska
 */
public abstract class AbstractTemplateExtensionPoint implements TemplateExtensionPoint {

  /**
   * Called before template create
   *
   * @param input {@link OPERATIONALTEMPLATE} to be created
   * @return input to be given to template create
   */
  public OPERATIONALTEMPLATE beforeCreation(OPERATIONALTEMPLATE input) {
    return input;
  }

  /**
   * Called after template create
   *
   * @param output template-id
   * @return template-id
   */
  public String afterCreation(String output) {
    return output;
  }

  @Override
  public String aroundCreation(
      OPERATIONALTEMPLATE input, Function<OPERATIONALTEMPLATE, String> chain) {
    return afterCreation(chain.apply(beforeCreation(input)));
  }
}
