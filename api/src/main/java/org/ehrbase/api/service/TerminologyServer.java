/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.service;

import java.util.EnumSet;
import java.util.List;

/***
 *@Created by Luis Marco-Ruiz on Feb 12, 2020
 *
 * @param <T> concept type
 * @param <ID> generic id type to specify the data type used for the identifier of the concept in the terminology server of choice.
 * @param <ID> generic type for parameters that are custom to each operation implementation.
 */
public interface TerminologyServer<T, ID, U> {
    /**
     * Expands the value set identified by the provided ID.
     * @param valueSetId
     * @return Returns the list of concepts of type T that conform the expansion of the value set.
     */
    List<T> expand(ID valueSetId);

    /**
     * Expands the value set identified by the provided ID.
     * @param valueSetId
     * @return Returns the list of concepts of type T that conform the expansion of the value set.
     */
    List<T> expandWithParameters(
            ID valueSetId,
            @SuppressWarnings("unchecked")
                    U... operationParams); // warning is ignored because the specific implementation will type the
    // method avoiding possible heap pollution

    /**
     * Searches all the attributes associated with the concept that corresponds to the provided ID.
     * @param conceptId
     * @return A complex Object of type T that contains all the attributes directly associated to the concept identified by the provided ID.
     */
    T lookUp(ID conceptId);
    /**
     * Evaluates if the concept provided T belongs to the value set identified by the provided ID.
     * @param concept to evaluate.
     * @param valueSetId
     * @return true if the concept belongs to the specified value set.
     */
    Boolean validate(T concept, ID valueSetId);

    /**
     * Evaluates if the concept provided as one operationParams  belongs to the value set provided as another operationParam.
     * @param dynamic list of parameters to perform the operation against an external terminology server.
     * @return true if the concept belongs to the specified value set.
     */
    Boolean validate(U... operationParams);
    /**
     * Evaluates if the concept B subsumes concept A.
     * @param concept that is subsumed by the concept in the second param.
     * @param concept that subsumes the concept in the first param.
     * @return {@link org.ehrbase.aql.compiler.tsclient.TerminologyServer.SubsumptionResult} indicating the result of the subsumption evaluation.
     */
    SubsumptionResult subsumes(T conceptA, T conceptB);
    /**
     *
     * Possible subsumption evaluation results.
     *
     */
    public enum SubsumptionResult {
        EQUIVALENT,
        SUBSUMES,
        SUBSUMEDBY,
        NOTSUBSUMED;
    }

    public enum TerminologyAdapter {
        FHIR("hl7.org/fhir/R4"),
        OCEAN("OTS.OCEANHEALTHSYSTEMS.COM"),
        BETTER("bts.better.care"),
        DTS4("dts4.apelon.com"),
        INDIZEN("cts2.indizen.com");

        private String adapterId;

        public String getAdapterId() {
            return adapterId;
        }

        private static EnumSet<TerminologyAdapter> supportedAdapters = EnumSet.of(FHIR);

        private TerminologyAdapter(String adapterId) {
            this.adapterId = adapterId;
        }

        public static boolean isAdapterSupported(String adapterToCheck) {
            for (TerminologyAdapter ta : supportedAdapters) {
                if (ta.name().equals(adapterToCheck)) {
                    return true;
                }
            }
            return false;
        }
    }
}
