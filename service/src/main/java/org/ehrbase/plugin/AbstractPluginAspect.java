/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.function.TriFunction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Pointcut;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.plugin.extensionpoints.CompositionExtensionPoint;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * @author Stefan Spiska
 */
/*TODO: maybe we should add the Ordered interface, so derived aspects will be evaluated in a predictable order in
relation to other aspects we might have or add later for other features or even through a plugin?*/
public abstract class AbstractPluginAspect<EXTENSIONPOINT> {

    private final Comparator<Map.Entry<String, EXTENSIONPOINT>> EXTENSION_POINTS_COMPARATOR =
            // respect @Order
            ((Comparator<Map.Entry<String, EXTENSIONPOINT>>)
                            (e1, e2) -> AnnotationAwareOrderComparator.INSTANCE.compare(e1.getValue(), e2.getValue()))
                    .reversed()
                    // ensure constant ordering
                    .thenComparing(Map.Entry::getKey);

    protected final ListableBeanFactory beanFactory;

    private final Class<EXTENSIONPOINT> clazz;

    protected AbstractPluginAspect(ListableBeanFactory beanFactory, Class<EXTENSIONPOINT> clazz) {
        this.beanFactory = beanFactory;
        this.clazz = clazz;
    }

    @Pointcut("within(org.ehrbase.service..*)")
    public void inServiceLayerPC() {}

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
        } catch (RuntimeException | Error e) {
            // Simple rethrow to handle in Controller layer
            throw e;
        } catch (Throwable e) {
            // should never happen
            throw new InternalServerException(e.getMessage(), e);
        }
    }

    /**
     * @return Order List of {@link CompositionExtensionPoint} in Context.
     */
    protected List<EXTENSIONPOINT> getActiveExtensionPointsOrderedDesc() {

        return beanFactory.getBeansOfType(clazz).entrySet().stream()
                .sorted(EXTENSION_POINTS_COMPARATOR)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Proceeds with the invocation by calling the given method on all extension points in ascending order and then the
     * service method (analog to Spring AOP aspects behaviour).
     *
     * @param pjp
     * @param extensionPointMethod Method that is part of Type EXTENSIONPOINT to call
     * @param argsToInputObj       Function to convert from an Object array to the input type IN used by extensionPointMethod
     * @param setArgs              Function to set/modify the Object array used for {@link ProceedingJoinPoint}::proceed using
     *                             an object of type IN
     * @param <IN>                 POJO Type used by extensionPointMethod for argument aggregation
     * @param <OUT>                return type of extensionPointMethod and the service method invocation represented by pjp
     * @return result of passing the method call through all extension points to the service layer and processing the return
     * value back through all extension points
     */
    protected <IN, OUT> OUT proceedWithPluginExtensionPoints(
            ProceedingJoinPoint pjp,
            TriFunction<EXTENSIONPOINT, IN, Function<IN, OUT>, OUT> extensionPointMethod,
            Function<Object[], IN> argsToInputObj,
            BiFunction<IN, Object[], Object[]> setArgs) {
        return proceedWithPluginExtensionPoints(pjp, extensionPointMethod, argsToInputObj, setArgs, ret -> (OUT) ret);
    }

    /**
     * Proceeds with the invocation by calling the given method on all extension points in ascending order and then the
     * service method (analog to Spring AOP aspects behaviour).
     *
     * @param pjp
     * @param extensionPointMethod Method that is part of Type EXTENSIONPOINT to call
     * @param argsToInputObj       Function to convert from an Object array to the input type IN used by extensionPointMethod
     * @param setArgs              Function to set/modify the Object array used for {@link ProceedingJoinPoint}::proceed using
     *                             an object of type IN
     * @param afterProceed         function to apply to adapt the return value of the service call to the return type of the
     *                             extension point method (OUT)
     * @param <IN>                 POJO Type used by extensionPointMethod for argument aggregation
     * @param <OUT>                return type of extensionPointMethod and in most cases the service method invocation
     *                             represented by pjp
     * @return result of passing the method call through all extension points to the service layer and processing the return
     * value back through all extension points
     */
    protected <IN, OUT> OUT proceedWithPluginExtensionPoints(
            ProceedingJoinPoint pjp,
            TriFunction<EXTENSIONPOINT, IN, Function<IN, OUT>, OUT> extensionPointMethod,
            Function<Object[], IN> argsToInputObj,
            BiFunction<IN, Object[], Object[]> setArgs,
            Function<Object, OUT> afterProceed) {

        List<EXTENSIONPOINT> extensionPoints = getActiveExtensionPointsOrderedDesc();
        if (extensionPoints.isEmpty()) {
            return afterProceed.apply(proceed(pjp, pjp.getArgs()));
        }

        IN inputArgsObj = argsToInputObj.apply(pjp.getArgs());
        // last extension point (first in the list) will hand over to the service layer
        Function<IN, OUT> callChain = in -> afterProceed.apply(proceed(pjp, setArgs.apply(in, pjp.getArgs())));
        // set up extension points to hand over to the next one of lower priority
        for (int i = 0; i < extensionPoints.size() - 1; i++) {
            final EXTENSIONPOINT ep = extensionPoints.get(i);
            final Function<IN, OUT> lastCall = callChain;
            callChain = in -> extensionPointMethod.apply(ep, in, lastCall);
        }

        // actually execute the first extension point method
        return extensionPointMethod.apply(extensionPoints.get(extensionPoints.size() - 1), inputArgsObj, callChain);
    }
}
