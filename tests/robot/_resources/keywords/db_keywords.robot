# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Library           DatabaseLibrary
Library           OperatingSystem
Library           Collections



*** Variables ***
${DBHost}         localhost
${DBName}         ehrbase
${DBPass}         postgres
${DBPort}         5432
${DBUser}         postgres



*** Keywords ***
Connect With DB
    [Documentation]     Establishes a database connection over psycopg2 PostgreSQL adapter.

                        Connect To Database    psycopg2    ${DBName}    ${DBUser}    ${DBPass}
                        ...                                ${DBHost}    ${DBPort}


Clean DB
    [Documentation]         Deletes rows in tables which were used by test.
    Connect With DB

    # Set Auto Commit
    # Execute SQL String    DROP DATABASE ehrbase;
    # Execute SQL Script    ${EXECDIR}${/}dbclean.sql


    # Delete All Templates
    # Delete All EHR Records

    # Delete All Rows From Table    ehr.attestation
    # Delete All Rows From Table    ehr.attested_view
    # Delete All Rows From Table    ehr.contribution
    # Delete All Rows From Table    ehr.audit_details
    Delete All Rows From Table    ehr.composition
    # Delete All Rows From Table    ehr.folder
    # Delete All Rows From Table    ehr.folder_hierarchy
    # Delete All Rows From Table    ehr.object_ref
    # Delete All Rows From Table    ehr.folder_items
    # Delete All Rows From Table    ehr.composition_history
    # Delete All Rows From Table    ehr.contribution_history
    # Delete All Rows From Table    ehr.event_context
    # Delete All Rows From Table    ehr.entry
    # Delete All Rows From Table    ehr.compo_xref
    # Delete All Rows From Table    ehr.participation
    # Delete All Rows From Table    ehr.concept
    Delete All Rows From Table    ehr.ehr
    Delete All Rows From Table    ehr.status
    # Delete All Rows From Table    ehr.party_identified
    # Delete All Rows From Table    ehr.identifier
    # Delete All Rows From Table    ehr.audit_details_history
    # Delete All Rows From Table    ehr.containment
    # Delete All Rows From Table    ehr.entry_history
    # Delete All Rows From Table    ehr.event_context_history
    # Delete All Rows From Table    ehr.folder_hierarchy_history
    # Delete All Rows From Table    ehr.folder_history
    # Delete All Rows From Table    ehr.folder_items_history
    # Delete All Rows From Table    ehr.object_ref_history
    # Delete All Rows From Table    ehr.participation_history
    Delete All Rows From Table    ehr.status_history
    Delete All Rows From Table    ehr.template_store

    # # Delete All Rows From Table    ehr.access
    # # Delete All Rows From Table    ehr.heading
    # # Delete All Rows From Table    ehr.language
    # # Delete All Rows From Table    ehr.schema_version
    # # Delete All Rows From Table    ehr.session_log
    # # Delete All Rows From Table    ehr.stored_query
    # # Delete All Rows From Table    ehr.terminology_provider
    # # Delete All Rows From Table    ehr.territory

    [Teardown]          Disconnect From Database


Delete All Templates
    [Documentation]     Deletes all templates from ehr.template_store table.
    ...                 Is meant to be used with EHRbase server started with disabled Cache
    ...                 e.g. `java -jar ehrbase-server.jar --cache.enabled=false`

                        Connect With DB
                        Delete All Rows From Table    ehr.template_store

    [Teardown]          Disconnect From Database


Delete All EHR Records
    [Documentation]     Deletes all EHR records from ehr.ehr table.
    ...                 Is meant to be used with EHRbase server started with disabled Cache
    ...                 e.g. `java -jar ehrbase-server.jar --cache.enabled=false`

                        Connect With DB
                        Delete All Rows From Table    ehr.ehr

    [Teardown]          Disconnect From Database


Get Information About DB Table
    [Documentation]     Provides infos about columns and their types of given table.
    ...                 table name exaples:
    ...                 ehr.ehr
    ...                 ehr.composition
    ...                 ehr.contribution
    ...                 ehr.folder
    ...                 ehr.template_store

    [Arguments]         ${table_name}

    @{output}=          Description    SELECT * FROM ${table_name}
                        Log Many    @${output}


Count Rows In DB Table
    [Documentation]     Provides infos about amount of rows in given table.
    [Arguments]         ${table_name}

    ${rowCount}=        Row Count    SELECT * FROM ${table_name}
    [Return]            ${rowCount}

# check https://github.com/franz-see/Robotframework-Database-Library/blob/master/test/PostgreSQL_DB_Tests.robot
# for more examples
