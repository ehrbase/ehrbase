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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.plugin.dto.CompositionIdWithVersionAndEhrId;
import org.ehrbase.plugin.dto.CompositionVersionIdWithEhrId;
import org.ehrbase.plugin.dto.CompositionWithEhrId;
import org.ehrbase.plugin.dto.CompositionWithEhrIdAndPreviousVersion;
import org.ehrbase.plugin.extensionpoints.CompositionExtensionPointInterface;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

/**
 * @author Stefan Spiska
 */
@Component
@Aspect
@ConditionalOnProperty(prefix = PLUGIN_MANAGER_PREFIX, name = "enable", havingValue = "true")
public class PluginAspect {

  public static final Comparator<Map.Entry<String, CompositionExtensionPointInterface>>
      EXTENSION_POINTS_COMPARATOR =
          // respect @Order
          ((Comparator<Map.Entry<String, CompositionExtensionPointInterface>>)
                  (e1, e2) ->
                      AnnotationAwareOrderComparator.INSTANCE.compare(e1.getValue(), e2.getValue()))
              .reversed()
              // ensure constant ordering
              .thenComparing(Map.Entry::getKey);

  private static class Chain<T> {

    T current;
    Chain<T> next;
  }

  private final ListableBeanFactory beanFactory;

  public PluginAspect(ListableBeanFactory beanFactory) {

    this.beanFactory = beanFactory;
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
  /**
   * Proceed with Error handling.
   *
   * @param pjp
   * @param args
   * @return
   */
  private Object proceed(ProceedingJoinPoint pjp, Object[] args) {
    try {
      return pjp.proceed(args);
    } catch (RuntimeException e) {
      // Simple rethrow to handle in Controller layer
      throw e;
    } catch (Exception e) {
      // should never happen
      throw new InternalServerException("Expedition in Plugin Aspect ", e);
    } catch (Throwable e) {
      // should never happen
      throw new InternalServerException(e.getMessage());
    }
  }

  /**
   * @return Order List of {@link CompositionExtensionPointInterface} in Context.
   */
  private List<CompositionExtensionPointInterface> getCompositionExtensionPointInterfaceList() {

    return beanFactory.getBeansOfType(CompositionExtensionPointInterface.class).entrySet().stream()
        .sorted(EXTENSION_POINTS_COMPARATOR)
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  /**
   * Convert List of Extension-points to chain.
   *
   * @param extensionPointInterfaceList
   * @param identity Extension-point which represents Identity.
   * @param <T> Class of the Extension-point
   * @return
   */
  private <T> Chain<T> buildChain(Collection<T> extensionPointInterfaceList, T identity) {
    Chain<T> chain = new Chain<>();
    // Add fist dummy Extension-point so the code path is the same weather there are
    // Extension-points or not.
    chain.current = identity;
    Chain<T> first = chain;

    for (T point : extensionPointInterfaceList) {
      Chain<T> next = new Chain<>();
      next.current = point;
      chain.next = next;
      chain = next;
    }
    return first;
  }

  /**
   * Execute chain of responsibility
   *
   * @param chain Fist chain
   * @param around Get the around Listener from Extension-point
   * @param input Initial Input
   * @param compositionFunction The intercepted Funktion
   * @param <X> Input of the intercepted Funktion
   * @param <R> Output of the intercepted Funktion
   * @param <T> Class of the Extension-point
   * @return output after all Extension-points have been handelt
   */
  private <X, R, T> R handleChain(
      Chain<T> chain,
      Function<T, BiFunction<X, Function<X, R>, R>> around,
      X input,
      Function<X, R> compositionFunction) {

    if (chain.next != null) {
      return handleChain(
          chain.next,
          around,
          input,
          i -> around.apply(chain.current).apply(i, compositionFunction));
    } else {
      return around.apply(chain.current).apply(input, compositionFunction);
    }
  }
}
