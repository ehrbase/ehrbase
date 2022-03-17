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

import org.ehrbase.plugin.dto.CompositionWithEhrId;

import java.util.UUID;
import java.util.function.Function;

/**
 * Provides After and Before Interceptors for {@link CompositionExtensionPointInterface}
 *
 * @author Stefan Spiska
 */
public abstract class AbstractCompositionExtensionPoint
    implements CompositionExtensionPointInterface {

  /**
   * Called before Composition create
   *
   * @param input {@link com.nedap.archie.rm.composition.Composition} to be created in ehr with
   *     ehrId {@link UUID}
   * @return input to be given to Composition create
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public CompositionWithEhrId beforeCreation(CompositionWithEhrId input) {
    return input;
  }

  /**
   * Intercept Composition create
   *
   * @param output {@link UUID} of the created Composition
   * @return {@link UUID} of the created Composition
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public UUID afterCreation(UUID output) {
    return output;
  }

  @Override
  public UUID aroundCreation(
      CompositionWithEhrId input, Function<CompositionWithEhrId, UUID> chain) {
    return afterCreation(chain.apply(beforeCreation(input)));
  }
}
