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

package org.ehrbase.plugin;

import static org.ehrbase.plugin.PluginHelper.PLUGIN_MANAGER_PREFIX;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ehrbase.plugin.dto.CompositionIdWithVersionAndEhrId;
import org.ehrbase.plugin.dto.CompositionVersionIdWithEhrId;
import org.ehrbase.plugin.dto.CompositionWithEhrId;
import org.ehrbase.plugin.dto.CompositionWithEhrIdAndPreviousVersion;
import org.ehrbase.plugin.extensionpoints.CompositionExtensionPointInterface;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Stefan Spiska
 */
@Component
@Aspect
@ConditionalOnProperty(prefix = PLUGIN_MANAGER_PREFIX, name = "enable", havingValue = "true")
public class CompositionPluginAspect
    extends AbstartPluginAspect<CompositionExtensionPointInterface> {

  public CompositionPluginAspect(ListableBeanFactory beanFactory) {
    super(beanFactory, CompositionExtensionPointInterface.class);
  }

  /**
   * Handle Extension-points for Composition create
   *
   * @param pjp
   * @return
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  @Around("execution(* org.ehrbase.api.service.CompositionService.create(..))")
  public Object aroundCreateComposition(ProceedingJoinPoint pjp) {

    Chain<CompositionExtensionPointInterface> chain =
        buildChain(
            getCompositionExtensionPointInterfaceList(),
            new CompositionExtensionPointInterface() {});
    Object[] args = pjp.getArgs();
    CompositionWithEhrId input = new CompositionWithEhrId((Composition) args[1], (UUID) args[0]);

    return Optional.of(
        handleChain(
            chain,
            l -> (l::aroundCreation),
            input,
            i -> {
              args[1] = i.getComposition();
              args[0] = i.getEhrId();

              return ((Optional<UUID>) proceed(pjp, args)).orElseThrow();
            }));
  }

  /**
   * Handle Extension-points for Composition update
   *
   * @param pjp
   * @return
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  @Around("execution(* org.ehrbase.api.service.CompositionService.update(..))")
  public Object aroundUpdateComposition(ProceedingJoinPoint pjp) {

    Chain<CompositionExtensionPointInterface> chain =
        buildChain(
            getCompositionExtensionPointInterfaceList(),
            new CompositionExtensionPointInterface() {});
    Object[] args = pjp.getArgs();
    CompositionWithEhrIdAndPreviousVersion input =
        new CompositionWithEhrIdAndPreviousVersion(
            (Composition) args[2], (ObjectVersionId) args[1], (UUID) args[0]);

    return Optional.of(
        handleChain(
            chain,
            l -> (l::aroundUpdate),
            input,
            i -> {
              args[2] = i.getComposition();
              args[1] = i.getPreviousVersion();
              args[0] = i.getEhrId();

              return ((Optional<UUID>) proceed(pjp, args)).orElseThrow();
            }));
  }

  /**
   * Handle Extension-points for Composition delete
   *
   * @param pjp
   * @return
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  @Around("execution(* org.ehrbase.api.service.CompositionService.delete(..))")
  public void aroundDeleteComposition(ProceedingJoinPoint pjp) {

    Chain<CompositionExtensionPointInterface> chain =
        buildChain(
            getCompositionExtensionPointInterfaceList(),
            new CompositionExtensionPointInterface() {});
    Object[] args = pjp.getArgs();
    CompositionVersionIdWithEhrId input =
        new CompositionVersionIdWithEhrId((ObjectVersionId) args[1], (UUID) args[0]);

    handleChain(
        chain,
        l -> (l::aroundDelete),
        input,
        i -> {
          args[1] = i.getVersionId();
          args[0] = i.getEhrId();

          proceed(pjp, args);
          return (Void) null;
        });
  }

  /**
   * Handle Extension-points for Composition retrieve
   *
   * @param pjp
   * @return
   * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
   */
  @Around("execution(* org.ehrbase.api.service.CompositionService.retrieve(..))")
  public Object aroundRetrieveComposition(ProceedingJoinPoint pjp) {

    Chain<CompositionExtensionPointInterface> chain =
        buildChain(
            getCompositionExtensionPointInterfaceList(),
            new CompositionExtensionPointInterface() {});
    Object[] args = pjp.getArgs();
    CompositionIdWithVersionAndEhrId input =
        new CompositionIdWithVersionAndEhrId((UUID) args[0], (UUID) args[1], (Integer) args[2]);

    return handleChain(
        chain,
        l -> (l::aroundRetrieve),
        input,
        i -> {
          args[2] = i.getVersion();
          args[1] = i.getCompositionId();
          args[0] = i.getEhrId();

          return (Optional<Composition>) proceed(pjp, args);
        });
  }

}
