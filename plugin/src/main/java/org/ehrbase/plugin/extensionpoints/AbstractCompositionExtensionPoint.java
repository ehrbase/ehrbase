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

import com.nedap.archie.rm.composition.Composition;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.plugin.dto.CompositionIdWithVersionAndEhrId;
import org.ehrbase.plugin.dto.CompositionVersionIdWithEhrId;
import org.ehrbase.plugin.dto.CompositionWithEhrId;
import org.ehrbase.plugin.dto.CompositionWithEhrIdAndPreviousVersion;

/**
 * Provides After and Before Interceptors for {@link CompositionExtensionPoint}
 *
 * @author Stefan Spiska
 */
public abstract class AbstractCompositionExtensionPoint implements CompositionExtensionPoint {

  /**
   * Called before Composition create
   *
   * @param input {@link com.nedap.archie.rm.composition.Composition} to be created in ehr with
   *              ehrId {@link UUID}
   * @return input to be given to Composition create
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public CompositionWithEhrId beforeCreation(CompositionWithEhrId input) {
    return input;
  }

  /**
   * Called after Composition create
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

  /**
   * Called before Composition update
   *
   * @param input {@link com.nedap.archie.rm.composition.Composition} to update previous version
   *     {@link com.nedap.archie.rm.support.identification.ObjectVersionId} in ehr with ehrId {@link
   *     UUID}
   * @return input to be given to Composition update
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public CompositionWithEhrIdAndPreviousVersion beforeUpdate(
      CompositionWithEhrIdAndPreviousVersion input) {
    return input;
  }

  /**
   * Called after Composition update
   *
   * @param output {@link UUID} of the updated Composition
   * @return {@link UUID} of the updated Composition
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public UUID afterUpdate(UUID output) {
    return output;
  }

  @Override
  public UUID aroundUpdate(
      CompositionWithEhrIdAndPreviousVersion input,
      Function<CompositionWithEhrIdAndPreviousVersion, UUID> chain) {
    return afterUpdate(chain.apply(beforeUpdate(input)));
  }

  /**
   * Called before Composition delete
   *
   * @param input {@link com.nedap.archie.rm.composition.Composition} to update previous version
   *     {@link com.nedap.archie.rm.support.identification.ObjectVersionId} in ehr with ehrId {@link
   *     UUID}
   * @return input to be given to Composition delete
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public CompositionVersionIdWithEhrId beforeDelete(CompositionVersionIdWithEhrId input) {
    return input;
  }

  /**
   * Called after Composition delete
   *
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public Void afterDelete(Void v) {
    return v;
  }

  @Override
  public Void aroundDelete(
      CompositionVersionIdWithEhrId input, Function<CompositionVersionIdWithEhrId, Void> chain) {
    return afterDelete(chain.apply(beforeDelete(input)));
  }

  /**
   * Called before Composition retrieve
   *
   * @param input composition id {@link UUID} with version in ehr with ehrId {@link UUID} to be
   *     retrieved
   * @return input to be given to Composition retrieve
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public CompositionIdWithVersionAndEhrId beforeRetrieve(CompositionIdWithVersionAndEhrId input) {
    return input;
  }

  /**
   * Called after Composition retrieve
   *
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  public Optional<Composition> afterRetrieve(Optional<Composition> output) {
    return output;
  }

  @Override
  public Optional<Composition> aroundRetrieve(
      CompositionIdWithVersionAndEhrId input,
      Function<CompositionIdWithVersionAndEhrId, Optional<Composition>> chain) {
    return afterRetrieve(chain.apply(beforeRetrieve(input)));
  }
}
