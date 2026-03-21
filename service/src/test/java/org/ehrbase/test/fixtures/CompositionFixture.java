/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.test.fixtures;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.support.identification.TerminologyId;

/**
 * Factory methods for creating RM Composition objects for tests.
 */
public final class CompositionFixture {

    private CompositionFixture() {}

    /**
     * Creates a minimal valid Composition with required fields populated.
     */
    public static Composition minimal(String templateId) {
        var composition = new Composition();
        composition.setName(new DvText("Test Composition"));
        composition.setArchetypeNodeId("openEHR-EHR-COMPOSITION.encounter.v1");

        var language = new CodePhrase(new TerminologyId("ISO_639-1"), "en");
        composition.setLanguage(language);

        var territory = new CodePhrase(new TerminologyId("ISO_3166-1"), "US");
        composition.setTerritory(territory);

        var category = new DvCodedText("event", new CodePhrase(new TerminologyId("openehr"), "433"));
        composition.setCategory(category);

        var composer = new PartyIdentified(null, "Test Composer", null);
        composition.setComposer(composer);

        var archetyped = new Archetyped();
        archetyped.setArchetypeId(new ArchetypeID("openEHR-EHR-COMPOSITION.encounter.v1"));
        archetyped.setRmVersion("1.0.4");
        var tid = new TemplateId();
        tid.setValue(templateId);
        archetyped.setTemplateId(tid);
        composition.setArchetypeDetails(archetyped);

        return composition;
    }

    /**
     * Creates a composition with null archetype_details (for validation error testing).
     */
    public static Composition withoutArchetypeDetails() {
        var composition = minimal("test-template");
        composition.setArchetypeDetails(null);
        return composition;
    }

    /**
     * Creates a composition with null template_id (for validation error testing).
     */
    public static Composition withoutTemplateId() {
        var composition = minimal("test-template");
        composition.getArchetypeDetails().setTemplateId(null);
        return composition;
    }

    /**
     * Creates a composition with null template_id value (for validation error testing).
     */
    public static Composition withEmptyTemplateIdValue() {
        var composition = minimal("test-template");
        composition.getArchetypeDetails().getTemplateId().setValue(null);
        return composition;
    }
}
