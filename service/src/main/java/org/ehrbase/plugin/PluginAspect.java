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

import com.nedap.archie.rm.composition.Composition;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.plugin.dto.CompositionWithEhrId;
import org.ehrbase.plugin.extensionpoints.CompositionExtensionPointInterface;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Stefan Spiska
 */
@Component
@Aspect
public class PluginAspect {

  private static class Chain<T> {

    T current;
    Chain<T> next;
  }

  private final ListableBeanFactory beanFactory;

  public PluginAspect(ListableBeanFactory beanFactory) {

    this.beanFactory = beanFactory;
  }

  @Around("execution(* org.ehrbase.api.service.CompositionService.create(..))")
  public Object aroundCreateComposition(ProceedingJoinPoint pjp) {

    Chain<CompositionExtensionPointInterface> chain =
        buildChain(
            getCompositionExtensionPointInterfaceList(),
            new CompositionExtensionPointInterface() {});
    CompositionWithEhrId input =
        new CompositionWithEhrId((Composition) pjp.getArgs()[1], (UUID) pjp.getArgs()[0]);

    return Optional.of(
        handleChain(
            chain,
            l -> (l::aroundCreation),
            input,
            i -> {
              pjp.getArgs()[1] = i.getComposition();
              pjp.getArgs()[0] = i.getEhrId();

              return ((Optional<UUID>) proceed(pjp)).orElseThrow();
            }));
  }

  private Object proceed(ProceedingJoinPoint pjp) {
    try {
      return pjp.proceed(pjp.getArgs());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerException("Expedition in Plugin Aspect ", e);
    } catch (Throwable e) {
      // should never happen
      throw new RuntimeException(e);
    }
  }

  private Collection<CompositionExtensionPointInterface>
      getCompositionExtensionPointInterfaceList() {
    return beanFactory.getBeansOfType(CompositionExtensionPointInterface.class).values();
  }

  private <T> Chain<T> buildChain(Collection<T> extensionPointInterfaceList, T identity) {
    Chain<T> chain = new Chain<>();
    chain.current = identity;

    for (T point : extensionPointInterfaceList) {
      Chain<T> next = new Chain<>();
      next.current = point;
      chain.next = next;
      chain = next;
    }
    return chain;
  }

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
