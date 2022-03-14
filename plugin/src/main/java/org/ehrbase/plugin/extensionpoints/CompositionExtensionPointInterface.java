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
import org.ehrbase.plugin.dto.CompositionWithEhrId;
import org.pf4j.ExtensionPoint;

import java.util.function.Function;

/**
 * @author Stefan Spiska
 */
public interface CompositionExtensionPointInterface extends ExtensionPoint {

    default Composition aroundCreation(CompositionWithEhrId input, Function<CompositionWithEhrId,Composition> chain ){
return chain.apply(input);
    }
}