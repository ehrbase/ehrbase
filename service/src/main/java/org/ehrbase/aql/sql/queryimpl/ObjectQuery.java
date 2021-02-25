/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.DSLContext;

/**
 * Created by christian on 5/6/2016.
 */
public abstract class ObjectQuery {

    protected I_DomainAccess domainAccess;
    protected PathResolver pathResolver;
    protected boolean jsonDataBlock = false;
    protected String itemType = null;

    public String getItemCategory() {
        return itemCategory;
    }

    protected String itemCategory = null;

    protected static int serial = 0; //used to alias fields for now.

    protected ObjectQuery(I_DomainAccess domainAccess, PathResolver pathResolver) {
        this.domainAccess = domainAccess;

        this.pathResolver = pathResolver;
    }

    public static void reset() {
        serial = 0;
    }

    public static void inc() {
        serial++;
    }

    public static int getSerial() {
        return serial;
    }

    public String getItemType() {
        return itemType;
    }

    public boolean isJsonDataBlock() {
        return jsonDataBlock;
    }

    public String variableTemplatePath(String templateId, String identifier){
        return pathResolver.pathOf(templateId, identifier);
    }

    public DSLContext getContext(){
        return domainAccess.getContext();
    }
}
