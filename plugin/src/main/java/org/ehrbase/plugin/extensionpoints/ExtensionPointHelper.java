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
package org.ehrbase.plugin.extensionpoints;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Extension Point for Ehr handling.
 *
 * @author Stefan Spiska
 * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
 * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
 */
public interface ExtensionPointHelper {
    /**
     * Applies "before" to the input, uses the result to proceed with the chain and returns the result of the chain.
     * <p>
     * Providing null for "before" is allowed and will cause the step to be skipped.
     *
     * @param input  input to use
     * @param chain  call chain function
     * @param before function applied to input before proceeding with the chain, Optional/nullable
     * @param <IN>   input parameter type
     * @param <OUT>  result type
     * @return result of chain(before(input))
     */
    static <IN, OUT> OUT before(IN input, Function<IN, OUT> chain, UnaryOperator<IN> before) {
        return beforeAndAfter(input, chain, before, null);
    }

    /**
     * Proceeds with the chain and returns the result of the chain after applying "after" to it.
     * <p>
     * Providing null for "after" is allowed and will cause the step to be skipped.
     *
     * @param input input to use
     * @param chain call chain function
     * @param after function applied to the result of proceeding with the chain, Optional/nullable
     * @param <IN>  input parameter type
     * @param <OUT> result type
     * @return result of after(chain(input))
     */
    static <IN, OUT> OUT after(IN input, Function<IN, OUT> chain, UnaryOperator<OUT> after) {
        return beforeAndAfter(input, chain, null, after);
    }

    /**
     * Applies "before" to the input, uses the result to proceed with the chain and returns the result of the chain after
     * applying "after" to it.
     * <p>
     * Providing null for "before" or "after" is allowed and will cause the step to be skipped.
     *
     * @param input  input to use
     * @param chain  call chain function
     * @param before function applied to input before proceeding with the chain, Optional/nullable
     * @param after  function applied to the result of proceeding with the chain, Optional/nullable
     * @param <IN>   input parameter type
     * @param <OUT>  result type
     * @return result of after(chain(before(input)))
     */
    static <IN, OUT> OUT beforeAndAfter(
            IN input, Function<IN, OUT> chain, UnaryOperator<IN> before, UnaryOperator<OUT> after) {
        OUT result = chain.apply(before == null ? input : before.apply(input));
        return after == null ? result : after.apply(result);
    }
}
