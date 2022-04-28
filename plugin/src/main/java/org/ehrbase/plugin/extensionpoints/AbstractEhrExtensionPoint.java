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

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.plugin.dto.EhrStatusVersionRequestParameters;
import org.ehrbase.plugin.dto.EhrStatusWithEhrId;

/**
 * Provides After and Before Interceptors for {@link EhrExtensionPoint}
 *
 * @author Stefan Spiska
 */
public abstract class AbstractEhrExtensionPoint implements EhrExtensionPoint {

  /**
   * Called before ehr create
   *
   * @param input ehr with ehrStatus {@link com.nedap.archie.rm.ehr.EhrStatus} to be created and
   *              optional ehrId {@link UUID}
   * @return input to be given to ehr create
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  public EhrStatusWithEhrId beforeCreation(EhrStatusWithEhrId input) {
    return input;
  }

  /**
   * Called after Ehr create
   *
   * @param output {@link UUID} of the created ehr
   * @return {@link UUID} of the created ehr
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  public UUID afterCreation(UUID output) {
    return output;
  }

  @Override
  public UUID aroundCreation(EhrStatusWithEhrId input, Function<EhrStatusWithEhrId, UUID> chain) {
    return afterCreation(chain.apply(beforeCreation(input)));
  }

  /**
   * Called before ehrStatus update
   *
   * @param input ehr with ehrStatus {@link com.nedap.archie.rm.ehr.EhrStatus} to be created and
   *     optional ehrId {@link UUID}
   * @return input to be given to ehrStatus update
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  public EhrStatusWithEhrId beforeUpdate(EhrStatusWithEhrId input) {
    return input;
  }

  /**
   * Called after ehrStatus update
   *
   * @param output {@link UUID} of the updated ehrStatus
   * @return {@link UUID} of the updated ehrStatus
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  public UUID afterUpdate(UUID output) {
    return output;
  }

  @Override
  public UUID aroundUpdate(EhrStatusWithEhrId input, Function<EhrStatusWithEhrId, UUID> chain) {
    return afterUpdate(chain.apply(beforeUpdate(input)));
  }

  /**
   * Called before ehrStatus retrieval
   *
   * @param input ehr with ehrStatus {@link com.nedap.archie.rm.ehr.EhrStatus} to be created and
   *              optional ehrId {@link UUID}
   * @return input to be given to ehrStatus update
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  public EhrStatusVersionRequestParameters beforeRetrieveAtVersion(EhrStatusVersionRequestParameters input) {
    return input;
  }

  /**
   * Called after ehrStatus retrieval
   *
   * @param output {@link EhrStatus} of the ehr
   * @return {@link EhrStatus} of the ehr
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  public Optional<OriginalVersion<EhrStatus>> afterRetrieveAtVersion(Optional<OriginalVersion<EhrStatus>> output) {
    return output;
  }

  @Override
  public Optional<OriginalVersion<EhrStatus>> aroundRetrieveAtVersion(EhrStatusVersionRequestParameters input,
                                                                      Function<EhrStatusVersionRequestParameters,
                                                                          Optional<OriginalVersion<EhrStatus>>> chain) {
    return afterRetrieveAtVersion(chain.apply(beforeRetrieveAtVersion(input)));
  }
}
