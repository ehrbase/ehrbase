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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.plugin.extensionpoints.CompositionExtensionPointInterface;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * @author Stefan Spiska
 */
public abstract class AbstartPluginAspect<T> {

  private final Comparator<Map.Entry<String, T>> EXTENSION_POINTS_COMPARATOR =
      // respect @Order
      ((Comparator<Map.Entry<String, T>>)
              (e1, e2) ->
                  AnnotationAwareOrderComparator.INSTANCE.compare(e1.getValue(), e2.getValue()))
          .reversed()
          // ensure constant ordering
          .thenComparing(Map.Entry::getKey);

  protected final ListableBeanFactory beanFactory;

  private final Class<T> clazz;

  protected AbstartPluginAspect(ListableBeanFactory beanFactory, Class<T> clazz) {
    this.beanFactory = beanFactory;
    this.clazz = clazz;
  }

  /**
   * Proceed with Error handling.
   *
   * @param pjp
   * @param args
   * @return
   */
  protected Object proceed(ProceedingJoinPoint pjp, Object[] args) {
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
  protected List<T> getExtensionPointInterfaceList() {

    return beanFactory.getBeansOfType(clazz).entrySet().stream()
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
  protected Chain<T> buildChain(Collection<T> extensionPointInterfaceList, T identity) {
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
  protected <X, R> R handleChain(
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

  protected static class Chain<T> {

    T current;
    Chain<T> next;
  }
}
