/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.validation.Validator;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.configuration.CacheConfiguration.VALIDATOR_CACHE;

@Service
public class ValidationServiceImp implements ValidationService {

    private Cache<UUID, Validator> validatorCache;
    private final I_KnowledgeCache knowledgeCache;

    @Autowired
    public ValidationServiceImp(CacheManager cacheManager, I_KnowledgeCache knowledgeCache) {
        this.validatorCache = cacheManager.getCache(VALIDATOR_CACHE, UUID.class, Validator.class);
        this.knowledgeCache = knowledgeCache;
    }


    @Override
    public void check(UUID templateUUID, Composition composition) throws Exception {
        //check if a validator is already in the cache
        Validator validator = validatorCache.get(templateUUID);

        if (validator == null){
            //create a new one for template
            Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledgeCache.retrieveOperationalTemplate(templateUUID);
            if (!operationaltemplate.isPresent()){
                throw new IllegalArgumentException("Not found template uuid:" + templateUUID);
            }
            validator = new Validator(operationaltemplate.get());
            //add to cache
            validatorCache.put(templateUUID, validator);
        }

        //perform the validation
        validator.check(composition);
    }


    @Override
    public void check(String templateID, Composition composition)  throws Exception {
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledgeCache.retrieveOperationalTemplate(templateID);
        if (!operationaltemplate.isPresent())
            throw new IllegalArgumentException("Not found template id:" + templateID);

        check(UUID.fromString(operationaltemplate.get().getUid().getValue()), composition);
    }

    @Override
    public void invalidate(){
        validatorCache.removeAll();
    }

}
