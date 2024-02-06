/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.ehrbase.openehr.sdk.terminology.openehr.CodeSetAccess;
import org.ehrbase.openehr.sdk.terminology.openehr.OpenEHRCodeSetIdentifiers;
import org.ehrbase.openehr.sdk.terminology.openehr.TerminologyAccess;
import org.ehrbase.openehr.sdk.terminology.openehr.TerminologyService;
import org.ehrbase.openehr.sdk.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.openehr.sdk.terminology.openehr.implementation.LocalizedTerminologies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TerminologyServiceImp implements TerminologyService {

    private static TerminologyServiceImp instance;
    private LocalizedTerminologies localizedTerminologies;

    @Autowired
    public TerminologyServiceImp() throws Exception {
        localizedTerminologies = new LocalizedTerminologies();
    }

    public static TerminologyServiceImp getInstance() {
        return instance;
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    @Override
    public TerminologyAccess terminology(String name) {
        return localizedTerminologies.getDefault().terminology(name);
    }

    @Override
    public TerminologyAccess terminology(String name, String language) {
        return localizedTerminologies.locale(language).terminology(name);
    }

    @Override
    public CodeSetAccess codeSet(String name) {
        return localizedTerminologies.getDefault().codeSet(name);
    }

    @Override
    public CodeSetAccess codeSet(String name, String language) {
        return localizedTerminologies.locale(language).codeSet(name);
    }

    @Override
    public CodeSetAccess codeSetForId(String name) {
        return localizedTerminologies.getDefault().codeSetForId(OpenEHRCodeSetIdentifiers.valueOf(name));
    }

    @Override
    public CodeSetAccess codeSetForId(String name, String language) {
        return localizedTerminologies.locale(language).codeSetForId(OpenEHRCodeSetIdentifiers.valueOf(name));
    }

    @Override
    public Boolean hasTerminology(String name) {
        return localizedTerminologies.getDefault().hasTerminology(name);
    }

    @Override
    public Boolean hasTerminology(String name, String language) {
        return localizedTerminologies.locale(language).hasTerminology(name);
    }

    @Override
    public Boolean hasCodeSet(String name) {
        return localizedTerminologies.getDefault().hasCodeSet(name);
    }

    @Override
    public Boolean hasCodeSet(String name, String language) {
        return localizedTerminologies.locale(language).hasCodeSet(name);
    }

    @Override
    public String[] terminologyIdentifiers() {
        return localizedTerminologies.getDefault().terminologyIdentifiers().toArray(new String[] {});
    }

    @Override
    public String[] terminologyIdentifiers(String language) {
        return localizedTerminologies.locale(language).terminologyIdentifiers().toArray(new String[] {});
    }

    @Override
    public Map<String, String> openehrCodeSets() {
        return localizedTerminologies.getDefault().openehrCodeSets();
    }

    @Override
    public Map<String, String> openehrCodeSets(String language) {
        return localizedTerminologies.locale(language).openehrCodeSets();
    }

    @Override
    public String[] codeSetIdentifiers() {
        return localizedTerminologies.getDefault().codeSetIdentifiers().toArray(new String[] {});
    }

    @Override
    public String[] codeSetIdentifiers(String language) {
        return localizedTerminologies.locale(language).codeSetIdentifiers().toArray(new String[] {});
    }

    @Override
    public String getLabelForCode(String code, String language) {
        return localizedTerminologies.locale(language).terminology("openehr").rubricForCode(code, language);
    }

    @Override
    public AttributeCodesetMapping codesetMapping() {
        return localizedTerminologies.codesetMapping();
    }

    @Override
    public LocalizedTerminologies localizedTerminologies() {
        return localizedTerminologies;
    }
}
