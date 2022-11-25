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
package org.ehrbase.aql.sql.queryimpl.attribute;

import org.jooq.Table;

/**
 * this class maintains the state of joins to build depending on the selected attributes
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452", "java:S1075"})
public class JoinSetup {
    // boolean indicating the resulting joins to generate
    private boolean joinComposition = false;
    private boolean joinEventContext = false;
    private boolean joinSubject = false;
    private boolean joinEhr = false;
    private boolean joinEhrStatus = false;
    private boolean joinComposer = false;
    private boolean joinContextFacility = false;
    private boolean joinSystem = false;
    private boolean containsEhrStatus = false;

    private boolean useEntry = false;

    protected Table
            partyJoinRef; // this table reference is used by forTableField() it indicates on which table the join is to
    // be done

    public boolean isJoinComposition() {
        return joinComposition;
    }

    public void setJoinComposition(boolean joinComposition) {
        this.joinComposition = joinComposition;
    }

    public boolean isJoinEventContext() {
        return joinEventContext;
    }

    public void setJoinEventContext(boolean joinEventContext) {
        this.joinEventContext = joinEventContext;
    }

    public boolean isJoinSubject() {
        return joinSubject;
    }

    public void setJoinSubject(boolean joinSubject) {
        this.joinSubject = joinSubject;
    }

    public boolean isJoinEhr() {
        return joinEhr;
    }

    public void setJoinEhr(boolean joinEhr) {
        this.joinEhr = joinEhr;
    }

    public boolean isJoinEhrStatus() {
        return joinEhrStatus;
    }

    public void setJoinEhrStatus(boolean joinEhrStatus) {
        this.joinEhrStatus = joinEhrStatus;
    }

    public boolean isJoinComposer() {
        return joinComposer;
    }

    public void setJoinComposer(boolean joinComposer) {
        this.joinComposer = joinComposer;
    }

    public boolean isJoinContextFacility() {
        return joinContextFacility;
    }

    public void setJoinContextFacility(boolean joinContextFacility) {
        this.joinContextFacility = joinContextFacility;
    }

    public boolean isContainsEhrStatus() {
        return containsEhrStatus;
    }

    public void setContainsEhrStatus(boolean containsEhrStatus) {
        this.containsEhrStatus = containsEhrStatus;
    }

    public Table getPartyJoinRef() {
        return partyJoinRef;
    }

    public void setPartyJoinRef(Table partyJoinRef) {
        this.partyJoinRef = partyJoinRef;
    }

    public void setJoinSystem(boolean b) {
        this.joinSystem = b;
    }

    public boolean isJoinSystem() {
        return joinSystem;
    }

    public void setUseEntry(boolean useEntry) {
        this.useEntry = useEntry;
    }

    public boolean isUseEntry() {
        return useEntry;
    }

    public JoinSetup merge(JoinSetup anotherJoinSetup) {
        joinComposition = joinComposition || anotherJoinSetup.isJoinComposition();
        joinComposer = joinComposer || anotherJoinSetup.isJoinComposer();
        joinEventContext = joinEventContext || anotherJoinSetup.isJoinEventContext();
        joinSubject = joinSubject || anotherJoinSetup.isJoinSubject();
        joinEhr = joinEhr || anotherJoinSetup.isJoinEhr();
        joinEhrStatus = joinEhrStatus || anotherJoinSetup.isJoinEhrStatus();
        joinComposer = joinComposer || anotherJoinSetup.joinComposer;
        joinContextFacility = joinContextFacility || anotherJoinSetup.isJoinContextFacility();
        joinSystem = joinSystem || anotherJoinSetup.isJoinSystem();
        containsEhrStatus = containsEhrStatus || anotherJoinSetup.isContainsEhrStatus();
        useEntry = useEntry || anotherJoinSetup.isUseEntry();

        return this;
    }
}
