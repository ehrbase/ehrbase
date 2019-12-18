/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemStructure;

public interface I_ItemStructureVisitor {

    void validate(Composition composition) throws IllegalArgumentException, InternalError;

    void validate(ItemStructure itemStructure) throws IllegalArgumentException, InternalError;

    void validate(Locatable locatable) throws IllegalArgumentException, InternalError;
}
