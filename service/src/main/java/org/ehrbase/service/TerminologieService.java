/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.service;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Service
@Transactional
public class TerminologieService extends BaseService {

    private static TerminologieService instance;

    @Autowired
    public TerminologieService(KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig) {
        super(knowledgeCacheService, context, serverConfig);
    }

    public static TerminologieService getInstance() {
        return instance;
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    public String getLabelForCode(String code) {
        return I_ConceptAccess.fetchConceptLiteral(getDataAccess(), Integer.parseInt(code), "en");
    }
}
