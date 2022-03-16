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
import org.apache.commons.lang3.ArrayUtils;
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
  public Object aroundCreateComposition(ProceedingJoinPoint pjp) throws Throwable {

    Collection<CompositionExtensionPointInterface> compositionExtensionPointInterfaceList =
        getCompositionExtensionPointInterfaceList();

    Function<CompositionWithEhrId, UUID> last =
        i -> {
          Object[] clone = ArrayUtils.clone(pjp.getArgs());
          clone[1] = i.getComposition();
          clone[0] = i.getEhrId();
          try {
            return ((Optional<UUID>) pjp.proceed(clone)).get();
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new InternalServerException("Expedition in Plugin Aspect ", e);
          } catch (Throwable e) {
            // should never happen
            throw new RuntimeException(e);
          }
        };

    Chain<CompositionExtensionPointInterface> chain =
        buildChain(
            compositionExtensionPointInterfaceList, new CompositionExtensionPointInterface() {});

    Composition compositionArg = (Composition) pjp.getArgs()[1];
    UUID ehrIdArg = (UUID) pjp.getArgs()[0];

    return Optional.of(handle(chain, new CompositionWithEhrId(compositionArg, ehrIdArg), last));
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

  private Collection<CompositionExtensionPointInterface>
      getCompositionExtensionPointInterfaceList() {
    return beanFactory.getBeansOfType(CompositionExtensionPointInterface.class).values();
  }

  private UUID handle(
      Chain<CompositionExtensionPointInterface> chain,
      CompositionWithEhrId input,
      Function<CompositionWithEhrId, UUID> compositionFunction) {

    if (chain.next != null) {
      return handle(chain.next, input, i -> chain.current.aroundCreation(i, compositionFunction));
    } else {
      return chain.current.aroundCreation(input, compositionFunction);
    }
  }
}
