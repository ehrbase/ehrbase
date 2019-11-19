/*
 * Copyright (c) 2019 Vitasystems GmbH, Jake Smolka (Hannover Medical School), and Luis Marco-Ruiz (Hannover Medical School).
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

package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.datastructures.Item;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

/***
 *@Created by Luis Marco-Ruiz on Jun 13, 2019
 */
public class FolderMockDataProvider implements MockDataProvider{

    private static final String TEST_SYSTEM_ID = "d315e003-7822-4168-89d6-813d79fc5d94";
    private static final String TEST_PARTY_IDENTIFIED_ID = "f7e7c0f4-9308-4093-baa2-d4e4b3abf022";

    @Override
    public MockResult[] execute(MockExecuteContext ctx) throws SQLException {

        DSLContext create = DSL.using(SQLDialect.POSTGRES_9_5);
        MockResult[] mock = new MockResult[1];
        String sql2 = ctx.sql();

        if (sql2.toUpperCase().startsWith("DROP")) {
            throw new SQLException("Statement not supported: " + sql2);
        }else if (sql2.toUpperCase().startsWith("CREATE")) {
            throw new SQLException("Statement not supported: " + sql2);
        }else if(sql2.toUpperCase().contains("DELETE FROM \"EHR\".\"FOLDER_ITEMS\" WHERE (\"EHR\".\"FOLDER_ITEMS\".\"FOLDER_ID\" = ? AND \"EHR\".\"FOLDER_ITEMS\".\"IN_CONTRIBUTION\" = ?)")){
                mock[0]=new MockResult(1, create.newResult(FOLDER_ITEMS));
                return mock;
        }else if(sql2.toUpperCase().contains("INSERT INTO \"EHR\".\"FOLDER_ITEMS\" (\"FOLDER_ID\", \"OBJECT_REF_ID\", \"IN_CONTRIBUTION\", \"SYS_TRANSACTION\", \"SYS_PERIOD\")")){

                mock[0]=new MockResult(1, create.newResult(FOLDER_ITEMS));
                return mock;


        }else if(sql2.toUpperCase().contains("INSERT INTO \"EHR\".\"OBJECT_REF\" (\"ID_NAMESPACE\", \"TYPE\", \"ID\", \"IN_CONTRIBUTION\", \"SYS_TRANSACTION\", \"SYS_PERIOD")){
             mock[0]=new MockResult(1, create.newResult(OBJECT_REF));
                return mock;

        }else if(sql2.toLowerCase().contains("select \"ehr\".\"folder\".\"id\", \"ehr\".\"folder\".\"in_contribution\", \"ehr\".\"folder\".\"name\", \"ehr\".\"folder\".\"archetype_node_id\", \"ehr\".\"folder\".\"active\", \"ehr\".\"folder\".\"details\", \"ehr\".\"folder\".\"sys_transaction\", \"ehr\".\"folder\".\"sys_period\" from \"ehr\".\"folder\" where \"ehr\".\"folder\".\"id\" = ?")) {
            if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb"))==0) {

                DateTimeFormatter dateTimeFormatter
                        = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                DateTime expected
                        = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                MockResult[] mock2 = new MockResult[1];
                Result<Record8<UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, Object>> result2 = create.newResult(FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD);

                result2.add(create
                        .newRecord(
                                FOLDER.ID,
                                FOLDER.IN_CONTRIBUTION,
                                FOLDER.NAME,
                                FOLDER.ARCHETYPE_NODE_ID,
                                FOLDER.ACTIVE,
                                FOLDER.DETAILS,
                                FOLDER.SYS_TRANSACTION,
                                FOLDER.SYS_PERIOD
                        )
                        .values(
                                UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb"),
                                UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                "folder_archetype_name_4", "folder_archetype.v1",
                                true,
                                //"{\"details\": \"xxx4\"}",
                                new ItemStructure() {
                                    @Override
                                    public List getItems() {
                                        Item item = new Item() {
                                            @Override
                                            public DvText getName() {
                                                return new DvText("xxx4");
                                            }
                                        };
                                        List<Item> items =  new ArrayList<>();
                                        items.add(item);
                                        return items;
                                    }
                                },
                                new Timestamp(expected.getMillis()),
                                new Timestamp(expected.getMillis())
                        )
                );

                mock2[0] = new MockResult(1, result2);//no rows returned
                return mock2;
            }else if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"))==0){

                DateTimeFormatter dateTimeFormatter
                        = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                DateTime expected
                        = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                MockResult[] mock2 = new MockResult[1];
                Result<Record8<UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, Object>> result2 = create.newResult(FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD);

                result2.add(create
                        .newRecord(
                                FOLDER.ID,
                                FOLDER.IN_CONTRIBUTION,
                                FOLDER.NAME,
                                FOLDER.ARCHETYPE_NODE_ID,
                                FOLDER.ACTIVE,
                                FOLDER.DETAILS,
                                FOLDER.SYS_TRANSACTION,
                                FOLDER.SYS_PERIOD
                        )
                        .values(
                                UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"),
                                UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                "folder_archetype_name_5","folder_archetype.v1",
                                true,
                                //"{\"details\": \"xxx5\"}",
                                new ItemStructure() {
                                    @Override
                                    public List getItems() {
                                        Item item = new Item() {
                                            @Override
                                            public DvText getName() {
                                                return new DvText("xxx5");
                                            }
                                        };
                                        List<Item> items =  new ArrayList<>();
                                        items.add(item);
                                        return items;
                                    }
                                },
                                new Timestamp(expected.getMillis()),
                                /*new Timestamp(expected.getMillis())*/null
                        )
                );

                mock2[0] = new MockResult(1, result2);//no rows returned
                return mock2;
            }
            throw new SQLException("SQL statement not expected. Consider enhancing or revising the FolderMockDataProvider in order to ensure the controlled behaviour of your code: " + sql2);

        }else if (sql2.toUpperCase().startsWith("SELECT \"EHR\".\"CONTRIBUTION\".\"EHR_ID\" FROM \"EHR\".\"CONTRIBUTION\" WHERE \"EHR\".\"CONTRIBUTION\".\"ID\" =")) {
            Result<Record1< UUID>> result2 = create.newResult(EHR_.ID);
            result2.add(create
                    .newRecord(EHR_.ID)
                    .values(UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634")));
            mock[0] = new MockResult(1, result2);
            return mock;
        }else if(sql2.toLowerCase().contains("with")) {

            //TABLE RETURNED FOR DELETE FOLDER TEST
            if(sql2.toUpperCase().contains("WITH RECURSIVE \"SUBFOLDERS\" AS ((SELECT \"ALIAS_70689680\".\"PARENT_FOLDER\", \"ALIAS_70689680\".\"CHILD_FOLDER\", \"ALIAS_70689680\".\"IN_CONTRIBUTION\", \"ALIAS_70689680\".\"SYS_TRANSACTION\", \"ALIAS_70689680\".\"SYS_PERIOD\" FROM (SELECT \"EHR\".\"FOLDER_HIERARCHY\".\"PARENT_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"CHILD_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"IN_CONTRIBUTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_TRANSACTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_PERIOD\" FROM \"EHR\".\"FOLDER_HIERARCHY\" WHERE \"EHR\".\"FOLDER_HIERARCHY\".\"PARENT_FOLDER\" = ?) AS \"ALIAS_70689680\") UNION (SELECT \"ALIAS_133512525\".\"PARENT_FOLDER\", \"ALIAS_133512525\".\"CHILD_FOLDER\", \"ALIAS_133512525\".\"IN_CONTRIBUTION\", \"ALIAS_133512525\".\"SYS_TRANSACTION\", \"ALIAS_133512525\".\"SYS_PERIOD\" FROM (SELECT \"EHR\".\"FOLDER_HIERARCHY\".\"PARENT_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"CHILD_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"IN_CONTRIBUTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_TRANSACTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_PERIOD\" FROM \"EHR\".\"FOLDER_HIERARCHY\") AS \"ALIAS_133512525\" JOIN SUBFOLDERS ON \"ALIAS_133512525\".\"PARENT_FOLDER\" = SUBFOLDERS.\"CHILD_FOLDER\")) SELECT * FROM \"SUBFOLDERS\"")){
                if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) {

                    DateTimeFormatter dateTimeFormatter
                            = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                    DateTime expected
                            = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                    TimeZone.getTimeZone("UTC");
                    MockResult[] mock2 = new MockResult[1];
                    Result<Record4<UUID, UUID, UUID, Timestamp>> result2 = create.newResult(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION);

                    result2.add(create
                            .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION)
                            .values(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"), UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"),  UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis())));
                    result2.add(create
                            .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION)
                            .values(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"), UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"),  UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis())));
                    result2.add(create
                            .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION)
                            .values(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"), UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb"),  UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis())));
                    result2.add(create
                            .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION)
                            .values(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"), UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"),  UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis())));

                    mock2[0] = new MockResult(5, result2);
                    return mock2;
                }

            }else if(sql2.toUpperCase().contains("WITH RECURSIVE \"SUBFOLDERS\" AS ((SELECT \"ALIAS_70689680\".\"PARENT_FOLDER\", \"ALIAS_70689680\".\"CHILD_FOLDER\", \"ALIAS_70689680\".\"IN_CONTRIBUTION\", \"ALIAS_70689680\".\"SYS_TRANSACTION\", \"ALIAS_70689680\".\"SYS_PERIOD\", \"T_FOLDER1\".\"ID\", \"T_FOLDER1\".\"IN_CONTRIBUTION\", \"T_FOLDER1\".\"NAME\", \"T_FOLDER1\".\"ARCHETYPE_NODE_ID\", \"T_FOLDER1\".\"ACTIVE\", \"T_FOLDER1\".\"DETAILS\", \"T_FOLDER1\".\"SYS_TRANSACTION\", \"T_FOLDER1\".\"SYS_PERIOD\" FROM (SELECT \"EHR\".\"FOLDER_HIERARCHY\".\"PARENT_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"CHILD_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"IN_CONTRIBUTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_TRANSACTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_PERIOD\" FROM \"EHR\".\"FOLDER_HIERARCHY\" WHERE \"EHR\".\"FOLDER_HIERARCHY\".\"PARENT_FOLDER\" = ?) AS \"ALIAS_70689680\" LEFT OUTER JOIN (SELECT \"EHR\".\"FOLDER\".\"ID\", \"EHR\".\"FOLDER\".\"IN_CONTRIBUTION\", \"EHR\".\"FOLDER\".\"NAME\", \"EHR\".\"FOLDER\".\"ARCHETYPE_NODE_ID\", \"EHR\".\"FOLDER\".\"ACTIVE\", \"EHR\".\"FOLDER\".\"DETAILS\", \"EHR\".\"FOLDER\".\"SYS_TRANSACTION\", \"EHR\".\"FOLDER\".\"SYS_PERIOD\" FROM \"EHR\".\"FOLDER\") AS \"T_FOLDER1\" ON \"ALIAS_70689680\".\"PARENT_FOLDER\" = \"T_FOLDER1\".\"ID\") UNION (SELECT \"ALIAS_133512525\".\"PARENT_FOLDER\", \"ALIAS_133512525\".\"CHILD_FOLDER\", \"ALIAS_133512525\".\"IN_CONTRIBUTION\", \"ALIAS_133512525\".\"SYS_TRANSACTION\", \"ALIAS_133512525\".\"SYS_PERIOD\", \"T_FOLDER2\".\"ID\", \"T_FOLDER2\".\"IN_CONTRIBUTION\", \"T_FOLDER2\".\"NAME\", \"T_FOLDER2\".\"ARCHETYPE_NODE_ID\", \"T_FOLDER2\".\"ACTIVE\", \"T_FOLDER2\".\"DETAILS\", \"T_FOLDER2\".\"SYS_TRANSACTION\", \"T_FOLDER2\".\"SYS_PERIOD\" FROM (SELECT \"EHR\".\"FOLDER_HIERARCHY\".\"PARENT_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"CHILD_FOLDER\", \"EHR\".\"FOLDER_HIERARCHY\".\"IN_CONTRIBUTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_TRANSACTION\", \"EHR\".\"FOLDER_HIERARCHY\".\"SYS_PERIOD\" FROM \"EHR\".\"FOLDER_HIERARCHY\") AS \"ALIAS_133512525\" JOIN SUBFOLDERS ON \"ALIAS_133512525\".\"PARENT_FOLDER\" = SUBFOLDERS.\"CHILD_FOLDER\" LEFT OUTER JOIN (SELECT \"EHR\".\"FOLDER\".\"ID\", \"EHR\".\"FOLDER\".\"IN_CONTRIBUTION\", \"EHR\".\"FOLDER\".\"NAME\", \"EHR\".\"FOLDER\".\"ARCHETYPE_NODE_ID\", \"EHR\".\"FOLDER\".\"ACTIVE\", \"EHR\".\"FOLDER\".\"DETAILS\", \"EHR\".\"FOLDER\".\"SYS_TRANSACTION\", \"EHR\".\"FOLDER\".\"SYS_PERIOD\" FROM \"EHR\".\"FOLDER\") AS \"T_FOLDER2\" ON \"T_FOLDER2\".\"ID\" = SUBFOLDERS.\"CHILD_FOLDER\")) SELECT * FROM \"SUBFOLDERS\"")){
                /*TABLE FOR THE RETRIEVE FOLDER ACCESS FOR EXISTING FOLDER*/

                if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) {//TABLE FOR THE RETRIEVE FOLDER ACCESS FOR EXISTING FOLDER

                    DateTimeFormatter dateTimeFormatter
                            = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                    DateTime expected
                            = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                    TimeZone.getTimeZone("UTC");
                    MockResult[] mock2 = new MockResult[1];
                    Result<Record13<UUID, UUID, UUID, Timestamp, Object, UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, Object>> result2 = create.newResult(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD);

                    result2.add(create
                            .newRecord(
                                    FOLDER_HIERARCHY.PARENT_FOLDER,
                                    FOLDER_HIERARCHY.CHILD_FOLDER,
                                    FOLDER_HIERARCHY.IN_CONTRIBUTION,
                                    FOLDER_HIERARCHY.SYS_TRANSACTION,
                                    FOLDER_HIERARCHY.SYS_PERIOD,
                                    FOLDER.ID,
                                    FOLDER.IN_CONTRIBUTION,
                                    FOLDER.NAME,
                                    FOLDER.ARCHETYPE_NODE_ID,
                                    FOLDER.ACTIVE,
                                    FOLDER.DETAILS,
                                    FOLDER.SYS_TRANSACTION,
                                    FOLDER.SYS_PERIOD
                            )
                            .values(
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"),
                                    new Timestamp(expected.getMillis()),
                                    "xxx1",
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                    "folder_archetype_name_1",
                                    "folder_archetype.v1",
                                    true,
                                    //"{\"details\": \"xxx1\"}",
                                    new ItemStructure() {
                                        @Override
                                        public List getItems() {
                                            Item item = new Item() {
                                                @Override
                                                public DvText getName() {
                                                    return new DvText("xxx1");
                                                }
                                            };
                                            List<Item> items =  new ArrayList<>();
                                            items.add(item);
                                            return items;
                                        }
                                    },
                                    new Timestamp(expected.getMillis()),
                                    "[\"2019-08-09 09:56:52.464799+02\",)"
                            )
                    );
                    result2.add(create
                            .newRecord(
                                    FOLDER_HIERARCHY.PARENT_FOLDER,
                                    FOLDER_HIERARCHY.CHILD_FOLDER,
                                    FOLDER_HIERARCHY.IN_CONTRIBUTION,
                                    FOLDER_HIERARCHY.SYS_TRANSACTION,
                                    FOLDER_HIERARCHY.SYS_PERIOD,
                                    FOLDER.ID,
                                    FOLDER.IN_CONTRIBUTION,
                                    FOLDER.NAME,
                                    FOLDER.ARCHETYPE_NODE_ID,
                                    FOLDER.ACTIVE,
                                    FOLDER.DETAILS,
                                    FOLDER.SYS_TRANSACTION,
                                    FOLDER.SYS_PERIOD
                            )
                            .values(
                                    UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"),
                                    new Timestamp(expected.getMillis()),
                                    "xxx2",
                                    UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                    "folder_archetype_name_2",
                                    "folder_archetype.v1",
                                    true,
                                    // "{\"details\": \"xxx2\"}",
                                    new ItemStructure() {
                                        @Override
                                        public List getItems() {
                                            Item item = new Item() {
                                                @Override
                                                public DvText getName() {
                                                    return new DvText("xxx2");
                                                }
                                            };
                                            List<Item> items =  new ArrayList<>();
                                            items.add(item);
                                            return items;
                                        }
                                    },
                                    new Timestamp(expected.getMillis()),
                                    "[\"2019-08-09 09:56:52.464799+02\",)"
                            )
                    );
                    result2.add(create
                            .newRecord(
                                    FOLDER_HIERARCHY.PARENT_FOLDER,
                                    FOLDER_HIERARCHY.CHILD_FOLDER,
                                    FOLDER_HIERARCHY.IN_CONTRIBUTION,
                                    FOLDER_HIERARCHY.SYS_TRANSACTION,
                                    FOLDER_HIERARCHY.SYS_PERIOD,
                                    FOLDER.ID,
                                    FOLDER.IN_CONTRIBUTION,
                                    FOLDER.NAME,
                                    FOLDER.ARCHETYPE_NODE_ID,
                                    FOLDER.ACTIVE,
                                    FOLDER.DETAILS,
                                    FOLDER.SYS_TRANSACTION,
                                    FOLDER.SYS_PERIOD
                            )
                            .values(
                                    UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"),
                                    new Timestamp(expected.getMillis()),
                                    "xxx3",
                                    UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                    "folder_archetype_name_3",
                                    "folder_archetype.v1",
                                    true,
                                    // "{\"details\": \"xxx3\"}",
                                    new ItemStructure() {
                                        @Override
                                        public List getItems() {
                                            Item item = new Item() {
                                                @Override
                                                public DvText getName() {
                                                    return new DvText("xxx3");
                                                }
                                            };
                                            List<Item> items =  new ArrayList<>();
                                            items.add(item);
                                            return items;
                                        }
                                    },
                                    new Timestamp(expected.getMillis()),
                                    "[\"2019-08-09 09:56:52.464799+02\",)"
                            )
                    );
                    result2.add(create
                            .newRecord(
                                    FOLDER_HIERARCHY.PARENT_FOLDER,
                                    FOLDER_HIERARCHY.CHILD_FOLDER,
                                    FOLDER_HIERARCHY.IN_CONTRIBUTION,
                                    FOLDER_HIERARCHY.SYS_TRANSACTION,
                                    FOLDER_HIERARCHY.SYS_PERIOD,
                                    FOLDER.ID,
                                    FOLDER.IN_CONTRIBUTION,
                                    FOLDER.NAME,
                                    FOLDER.ARCHETYPE_NODE_ID,
                                    FOLDER.ACTIVE,
                                    FOLDER.DETAILS,
                                    FOLDER.SYS_TRANSACTION,
                                    FOLDER.SYS_PERIOD
                            )
                            .values(
                                    UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"),
                                    new Timestamp(expected.getMillis()),
                                    "xxx3",
                                    UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"),
                                    UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"),
                                    "folder_archetype_name_3",
                                    "folder_archetype.v1",
                                    true,
                                    // "{\"details\": \"xxx3\"}",
                                    new ItemStructure() {
                                        @Override
                                        public List getItems() {
                                            Item item = new Item() {
                                                @Override
                                                public DvText getName() {
                                                    return new DvText("xxx3");
                                                }
                                            };
                                            List<Item> items =  new ArrayList<>();
                                            items.add(item);
                                            return items;
                                        }
                                    },
                                    new Timestamp(expected.getMillis()),
                                    "[\"2019-08-09 09:56:52.464799+02\",)"
                            )
                    );

                    mock2[0] = new MockResult(1, result2);//no rows returned
                    return mock2;
                }
            }else if(sql2.toUpperCase().contains("WITH \"FOLDERITEMSSELECT\" AS (SELECT \"EHR\".\"FOLDER_ITEMS\".\"OBJECT_REF_ID\" AS \"OBJECT_REF_ID\", \"EHR\".\"FOLDER_ITEMS\".\"IN_CONTRIBUTION\" AS \"ITEM_IN_CONTRIBUTION\" FROM \"EHR\".\"FOLDER_ITEMS\" WHERE \"EHR\".\"FOLDER_ITEMS\".\"FOLDER_ID\" = ?) SELECT * FROM \"EHR\".\"OBJECT_REF\", \"FOLDERITEMSSELECT\" WHERE (\"OBJECT_REF_ID\" = \"EHR\".\"OBJECT_REF\".\"ID\" AND \"ITEM_IN_CONTRIBUTION\" = \"EHR\".\"OBJECT_REF\".\"IN_CONTRIBUTION\")")){

                DateTimeFormatter dateTimeFormatter
                        = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                DateTime expected
                        = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                TimeZone.getTimeZone("UTC");

                MockResult[] mock2 = new MockResult[1];
                Result<Record8<String, String, UUID, UUID, Timestamp, Object, UUID, UUID>> result2 = create.newResult(OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID, OBJECT_REF.IN_CONTRIBUTION, OBJECT_REF.SYS_TRANSACTION, OBJECT_REF.SYS_PERIOD, FOLDER_ITEMS.OBJECT_REF_ID, FOLDER_ITEMS.IN_CONTRIBUTION);


                if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) {
                    result2.add(create
                            .newRecord(OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID, OBJECT_REF.IN_CONTRIBUTION, OBJECT_REF.SYS_TRANSACTION, OBJECT_REF.SYS_PERIOD, FOLDER_ITEMS.OBJECT_REF_ID, FOLDER_ITEMS.IN_CONTRIBUTION)
                            .values("namespace", "FOLDER", UUID.fromString("48282ddd-4c7d-444a-8159-458a03c9827f"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis()),  "[\"2019-08-09 09:56:52.464799+02\",)", UUID.fromString("48282ddd-4c7d-444a-8159-458a03c9827f"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb")));
                    mock2[0] = new MockResult(1, result2);
                    return mock2;

                }else if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))==0) {

                    result2.add(create
                            .newRecord(OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID, OBJECT_REF.IN_CONTRIBUTION, OBJECT_REF.SYS_TRANSACTION, OBJECT_REF.SYS_PERIOD, FOLDER_ITEMS.OBJECT_REF_ID, FOLDER_ITEMS.IN_CONTRIBUTION)
                            .values("namespace2", "COMPOSITION", UUID.fromString("076f09ee-8da3-ae1b-0072-3ee18965fbb9"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis()),  "[\"2019-08-09 09:56:52.464799+02\",)", UUID.fromString("076f09ee-8da3-ae1b-0072-3ee18965fbb9"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb")));

                    result2.add(create
                            .newRecord(OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID, OBJECT_REF.IN_CONTRIBUTION, OBJECT_REF.SYS_TRANSACTION, OBJECT_REF.SYS_PERIOD, FOLDER_ITEMS.OBJECT_REF_ID, FOLDER_ITEMS.IN_CONTRIBUTION)
                            .values("namespace3", "EHR", UUID.fromString("5bf07118-e22e-e233-35c9-78820d76627c"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), new Timestamp(expected.getMillis()),  "[\"2019-08-09 09:56:52.464799+02\",)", UUID.fromString("5bf07118-e22e-e233-35c9-78820d76627c"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb")));
                    mock2[0] = new MockResult(2, result2);
                    return mock2;
                }
                mock2[0] = new MockResult(0, result2);//return empty, i.e. no rows
                return mock2;
            }

            throw new SQLException("SQL statement not expected. Consider enhancing or revising the FolderMockDataProvider in order to ensure the controlled behaviour of your code: " + sql2);

        }else if(sql2.toUpperCase().contains("SELECT 1 AS \"ONE\"")) {//query needed for mocking initialization of the system
            Field<Integer> c = DSL.count();
            Result<Record1< Integer>> result2 = create.newResult(c);
            result2.add(create
                    .newRecord(c)
                    .values(1));
            mock[0] = new MockResult(1, result2);
            return mock;
        }else if(sql2.toUpperCase().contains("SELECT \"EHR\".\"PARTY_IDENTIFIED\"")) {//query needed for mocking initialization of the system
            System.out.println("SQL2 is: "+sql2.toUpperCase());
            System.out.println("PARTY_IDENTIFIED hist selected");
            Result<Record6< UUID, String, String, String, String, String>> result2 = create.newResult(PARTY_IDENTIFIED.ID, PARTY_IDENTIFIED.NAME, PARTY_IDENTIFIED.PARTY_REF_VALUE, PARTY_IDENTIFIED.PARTY_REF_SCHEME, PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PARTY_IDENTIFIED.PARTY_REF_TYPE);
            result2.add(create
                    .newRecord(PARTY_IDENTIFIED.ID, PARTY_IDENTIFIED.NAME, PARTY_IDENTIFIED.PARTY_REF_VALUE, PARTY_IDENTIFIED.PARTY_REF_SCHEME, PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PARTY_IDENTIFIED.PARTY_REF_TYPE)
                    .values(UUID.fromString(TEST_PARTY_IDENTIFIED_ID), "Dr Tony Blegon", null, null, null, null));
            mock[0] = new MockResult(1, result2);
            return mock;
        }else if(sql2.toUpperCase().contains("SELECT \"EHR\".\"SYSTEM\".\"ID\" FROM \"EHR\".\"SYSTEM\" WHERE \"EHR\".\"SYSTEM\".\"SETTINGS\" =")) {//query needed for mocking initialization of the system
            Result<Record1< UUID>> result2 = create.newResult(SYSTEM.ID);
            result2.add(create
                    .newRecord(SYSTEM.ID)
                    .values(UUID.fromString(TEST_SYSTEM_ID)));
            mock[0] = new MockResult(1, result2);
            return mock;
        }else if(sql2.toUpperCase().contains("SELECT \"EHR\".\"CONCEPT\".\"ID\", \"EHR\".\"CONCEPT\".\"CONCEPTID\", \"EHR\".\"CONCEPT\".\"LANGUAGE\", \"EHR\".\"CONCEPT\".\"DESCRIPTION\"")) {//query needed for mocking initialization of the system
            Result<Record4<UUID, Integer, String, String>> result2 = create.newResult(CONCEPT.ID, CONCEPT.CONCEPTID, CONCEPT.LANGUAGE, CONCEPT.DESCRIPTION);
            result2.add(create
                    .newRecord(CONCEPT.ID, CONCEPT.CONCEPTID, CONCEPT.LANGUAGE, CONCEPT.DESCRIPTION)
                    .values(UUID.fromString("dcd3b8c5-36e3-4ac7-83aa-7e9068f443d8"), new Integer(249), "en", "creation"));
            mock[0] = new MockResult(1, result2);
            return mock;
        } else if (sql2.toUpperCase().startsWith("INSERT INTO \"EHR\".\"CONTRIBUTION")) {
            if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("f6a2af65-fe89-45a4-9456-07c5e17b1634"))==0) {
                Result<Record1< UUID>> result2 = create.newResult(CONTRIBUTION.ID);
                result2.add(create
                        .newRecord(CONTRIBUTION.ID)
                        .values(UUID.fromString("3e2fcd27-d846-4a0d-888a-47b2da9e5cbb")));
                mock[0] = new MockResult(1, result2);
                return mock;

            }

            throw new SQLException("Statement not currently supported. Consider enhancing or revising the FolderMockDataProvider: " + sql2);

        }else if (sql2.toUpperCase().startsWith("INSERT INTO \"EHR\".\"FOLDER\" (\"ID\", \"IN_CONTRIBUTION\",")) {

            if(sql2.toUpperCase().startsWith(("INSERT INTO \"EHR\".\"FOLDER\" (\"ID\", \"IN_CONTRIBUTION\", \"NAME\", \"ARCHETYPE_NODE_ID\", \"ACTIVE\", \"DETAILS\", \"SYS_TRANSACTION\") VALUES (?, ?, ?, ?, ?, '{\n" +
                    "  \"_TYPE\" : \"\",\n" +
                    "  \"ITEMS\" : [ {\n" +
                    "    \"NAME\" : {\n" +
                    "      \"_TYPE\" : \"DV_TEXT\",\n" +
                    "      \"VALUE\" : \"FOL2\"\n" +
                    "    }\n" +
                    "  } ]\n" +
                    "}'::JSONB, CAST(? AS TIMESTAMP)) RETURNING \"EHR\".\"FOLDER\".\"ID\""))){

                    if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"))==0) {
                        Result<Record1< UUID>> result2 = create.newResult(FOLDER.ID);
                        result2.add(create
                                .newRecord(FOLDER.ID)
                                .values(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634")));
                        mock[0] = new MockResult(1, result2);
                        return mock;
                    }
            }else if(sql2.toUpperCase().startsWith(("INSERT INTO \"EHR\".\"FOLDER\" (\"ID\", \"IN_CONTRIBUTION\", \"NAME\", \"ARCHETYPE_NODE_ID\", \"ACTIVE\", \"DETAILS\", \"SYS_TRANSACTION\") VALUES (?, ?, ?, ?, ?, '{\n" +
                    "  \"_TYPE\" : \"\",\n" +
                    "  \"ITEMS\" : [ {\n" +
                    "    \"NAME\" : {\n" +
                    "      \"_TYPE\" : \"DV_TEXT\",\n" +
                    "      \"VALUE\" : \"FOL1\"\n" +
                    "    }\n" +
                    "  } ]\n" +
                    "}'::JSONB, CAST(? AS TIMESTAMP)) RETURNING \"EHR\".\"FOLDER\".\"ID\""))){

                    if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("f8a2af65-fe89-45a4-9456-07c5e17b1634"))==0) {
                        Result<Record1< UUID>> result2 = create.newResult(FOLDER.ID);
                        result2.add(create
                                .newRecord(FOLDER.ID)
                                .values(UUID.fromString("f8a2af65-fe89-45a4-9456-07c5e17b1634")));
                        mock[0] = new MockResult(1, result2);
                        return mock;
                    }
            }else if(sql2.toUpperCase().startsWith(("INSERT INTO \"EHR\".\"FOLDER\" (\"ID\", \"IN_CONTRIBUTION\", \"NAME\", \"ARCHETYPE_NODE_ID\", \"ACTIVE\", \"DETAILS\", \"SYS_TRANSACTION\") VALUES (?, ?, ?, ?, ?, '{\n" +
                    "  \"_TYPE\" : \"\",\n" +
                    "  \"ITEMS\" : [ { } ]\n" +
                    "}'::JSONB, CAST(? AS TIMESTAMP)) RETURNING \"EHR\".\"FOLDER\".\"ID\""))){

                if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634"))==0) {
                    Result<Record1< UUID>> result2 = create.newResult(FOLDER.ID);
                    result2.add(create
                            .newRecord(FOLDER.ID)
                            .values(UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634")));
                    mock[0] = new MockResult(1, result2);
                    return mock;
                }
            }
            throw new SQLException("SQL statement not expected. Consider enhancing or revising the FolderMockDataProvider in order to ensure the controlled behaviour of your code: " + sql2);
        }else if (sql2.toUpperCase().startsWith("INSERT INTO \"EHR\".\"FOLDER_HIERARCHY\" (\"PARENT_FOLDER\", \"CHILD_FOLDER\", \"IN_CONTRIBUTION\", \"SYS_TRANSACTION\") VALUES")) {

            String uidParentFolder = ((UUID)ctx.bindings()[0]).toString();
            String uidChildFolder = ((UUID)ctx.bindings()[1]).toString();

            if((uidParentFolder.equals("f0a2af65-fe89-45a4-9456-07c5e17b1634")) && (uidChildFolder.equals("f4a2af65-fe89-45a4-9456-07c5e17b1634"))) {
                Result<Record2< UUID, UUID>> result2 = create.newResult(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER);
                result2.add(create
                        .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER)
                        .values(UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634"), UUID.fromString("f4a2af65-fe89-45a4-9456-07c5e17b1634")));
                mock[0] = new MockResult(1, result2);
                return mock;

            }else if((uidParentFolder.equals("f8a2af65-fe89-45a4-9456-07c5e17b1634")) && (uidChildFolder.equals("f0a2af65-fe89-45a4-9456-07c5e17b1634"))) {
                Result<Record2< UUID, UUID>> result2 = create.newResult(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER);
                result2.add(create
                        .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER)
                        .values(UUID.fromString("f8a2af65-fe89-45a4-9456-07c5e17b1634"), UUID.fromString("f0a2af65-fe89-45a4-9456-07c5e17b1634")));
                mock[0] = new MockResult(1, result2);
                return mock;

            }
        }else if (sql2.toUpperCase().startsWith("UPDATE \"EHR\".\"CONTRIBUTION\" SET \"CONTRIBUTION_TYPE\"")) {
            Field<Integer> c = DSL.count();
            Result<Record1< Integer>> result2 = create.newResult(c);
            result2.add(create
                    .newRecord(c)
                    .values(1));
            mock[0] = new MockResult(1, result2);
            return mock;

        }else if (sql2.toUpperCase().startsWith("UPDATE \"EHR\".\"CONTRIBUTION\" SET \"EHR_ID\" = ?, \"CONTRIBUTION_TYPE\" = ?::\"EHR\".\"CONTRIBUTION_DATA_TYPE\", \"STATE\" =")) {
            Field<Integer> c = DSL.count();
            Result<Record1< Integer>> result2 = create.newResult(c);
            result2.add(create
                    .newRecord(c)
                    .values(1));
            mock[0] = new MockResult(1, result2);
            return mock;

        }else if (sql2.toUpperCase().startsWith("UPDATE \"EHR\".\"FOLDER\" SET \"ID\" = ?, \"IN_CONTRIBUTION\" = ?, \"NAME\" = ?, \"ARCHETYPE_NODE_ID\" = ?, \"ACTIVE\" = ?, \"DETAILS\" = ")) {

            if((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((String)ctx.bindings()[2]).compareTo("modifiedName")==0) && (((String)ctx.bindings()[3]).compareTo("modifiedArchetypeNodeId")==0) && (((Boolean)ctx.bindings()[4]).compareTo(false)==0)) {
                mock[0]=new MockResult(1, create.newResult(FOLDER));
                return mock;

            }else if((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((String)ctx.bindings()[2]).compareTo("new name of folder 2")==0) && (((String)ctx.bindings()[3]).compareTo("folder_archetype.v1")==0) && (((Boolean)ctx.bindings()[4]).compareTo(true)==0)){

                mock[0]=new MockResult(1, create.newResult(FOLDER));
                return mock;
            }else if((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("33550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((String)ctx.bindings()[2]).compareTo("folder_archetype_name_3")==0) && (((String)ctx.bindings()[3]).compareTo("new archetype node id")==0) && (((Boolean)ctx.bindings()[4]).compareTo(true)==0)){

                mock[0]=new MockResult(1, create.newResult(FOLDER));
                return mock;
            } else if((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"))==0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((String)ctx.bindings()[2]).compareTo("folder_archetype_name_5")==0) && (((String)ctx.bindings()[3]).compareTo("folder_archetype.v1")==0) && (((Boolean)ctx.bindings()[4]).compareTo(false)==0)){

            mock[0]=new MockResult(1, create.newResult(FOLDER));
            return mock;
        }else if((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("77750555-ec91-4025-838d-09ddb4e999cb"))==0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((String)ctx.bindings()[2]).compareTo("folder_archetype_name_4")==0) && (((String)ctx.bindings()[3]).compareTo("folder_archetype.v1")==0) && (((Boolean)ctx.bindings()[4]).compareTo(true)==0)){

                mock[0]=new MockResult(1, create.newResult(FOLDER));
                return mock;
            }else if((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("00550555-ec91-4025-838d-09ddb4e999cb"))==0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString("3e2fcd27-d846-4a0d-888a-47b2da9e5cbb"))==0)){

                mock[0]=new MockResult(1, create.newResult(FOLDER));
                return mock;
            }
        }else  if (sql2.toUpperCase().startsWith("DELETE FROM \"EHR\".\"FOLDER\" WHERE (\"EHR\".\"FOLDER\".\"ID\" IN (?, ?, ?, ?) OR \"EHR\".\"FOLDER\".\"ID\" = ?)")) {

            if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("99550555-ec91-4025-838d-09ddb4e999cb"))==0) {

                mock[0]=new MockResult(5, create.newResult(FOLDER));
                return mock;
            }
        }else if (sql2.toLowerCase().startsWith("insert into \"ehr\".\"audit_details\" (\"system_id\", \"committer\", \"time_committed\", \"time_committed_tzid\", \"change_type\") values")) {
            if ((((UUID)ctx.bindings()[0]).compareTo(UUID.fromString(TEST_SYSTEM_ID)) == 0) && (((UUID)ctx.bindings()[1]).compareTo(UUID.fromString(TEST_PARTY_IDENTIFIED_ID)) == 0)) {
                Result<Record1<UUID>> result2 = create.newResult(AUDIT_DETAILS.ID);
                result2.add(create
                        .newRecord(AUDIT_DETAILS.ID)
                        .values(UUID.fromString("3e2fcd27-d846-4a0d-888a-47b2da9e5cbb")));
                mock[0] = new MockResult(1, result2);
                return mock;
            }

            throw new SQLException("SQL statement not expected. Consider enhancing or revising the FolderMockDataProvider in order to ensure the controlled behaviour of your code: " + sql2);

        }
        return mock;
    }
}
