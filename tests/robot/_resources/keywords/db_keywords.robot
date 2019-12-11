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

Delete All Templates
    [Documentation]     Deletes all templates from ehr.template_store table.
    ...                 Is meant to be used with EHRbase server started with disabled Cache
    ...                 e.g. `java -jar ehrbase-server.jar --cache.enabled=false`

                        Connect With DB
                        Delete All Rows From Table    ehr.template_store

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

    @{output} =         Description    SELECT * FROM ${table_name}
                        Log Many    @${output}


Count Rows In DB Table
    [Documentation]     Provides infos about amout of rows in given table.
    [Arguments]         ${table_name}

    ${rowCount}	=       Row Count    SELECT * FROM ${table_name}
                        Log    ${rowCount}

# check https://github.com/franz-see/Robotframework-Database-Library/blob/master/test/PostgreSQL_DB_Tests.robot
# for more examples
