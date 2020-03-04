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
        import org.postgresql.util.PGobject;

        import java.sql.SQLException;
        import java.sql.Time;
        import java.sql.Timestamp;
        import java.time.OffsetDateTime;
        import java.util.*;

        import static org.ehrbase.jooq.pg.Tables.*;

        /***
         *@Created by Luis Marco-Ruiz on Jun 13, 2019
         */

        /**
         * This mock data provider coresponds to the versioning of folders with the status in the file testFolderVersionsDB.sql
         */
        public class FolderAccessHistoryMockDataProvider implements MockDataProvider{

            @Override
            public MockResult[] execute(MockExecuteContext ctx) throws SQLException {

                DSLContext create = DSL.using(SQLDialect.POSTGRES);
                MockResult[] mock = new MockResult[1];
                String sql2 = ctx.sql();

                if (sql2.toUpperCase().startsWith("DROP")) {
                    throw new SQLException("Statement not supported: " + sql2);
                }else if (sql2.toUpperCase().startsWith("CREATE")) {
                    throw new SQLException("Statement not supported: " + sql2);
                }else if(sql2.toUpperCase().contains("DELETE")){
                    throw new SQLException("Statement not supported: " + sql2);
                }else if(sql2.toLowerCase().contains("select \"alias_90044192\".\"folder_id\", \"alias_90044192\".\"item_object_ref_id\", \"alias_90044192\".\"item_in_contribution\"")) {

                    if (((UUID) ctx.bindings()[1]).equals(UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"))) {

                        if(((UUID) ctx.bindings()[0]).equals(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"))) {
                            MockResult[] mock2 = new MockResult[1];
                            Result<Record11<UUID, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>, String, String, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 = create.newResult(FOLDER_ITEMS.FOLDER_ID, FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"), FOLDER_ITEMS.SYS_TRANSACTION, FOLDER_ITEMS.SYS_PERIOD, OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID.as("obj_ref_id"), OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"), OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"), OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"));

                            mock2[0] = new MockResult(0, result2);
                            return mock2;
                        }else if(((UUID) ctx.bindings()[0]).equals(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"))) {
                            MockResult[] mock2 = new MockResult[1];
                            Result<Record11<UUID, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>, String, String, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 = create.newResult(FOLDER_ITEMS.FOLDER_ID, FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"), FOLDER_ITEMS.SYS_TRANSACTION, FOLDER_ITEMS.SYS_PERIOD, OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID.as("obj_ref_id"), OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"), OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"), OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"));

                            result2.add(create
                                .newRecord(FOLDER_ITEMS.FOLDER_ID, FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"), FOLDER_ITEMS.SYS_TRANSACTION, FOLDER_ITEMS.SYS_PERIOD, OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID.as("obj_ref_id"), OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"), OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"), OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"))
                                .values(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"), UUID.fromString("88282ddd-4c7d-444a-8159-458a03c9827f"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:32:47.013378+00"), OffsetDateTime.parse("2020-01-09T11:39:00.898947+00")), "namespace leave", "COMPOSITION", UUID.fromString("88282ddd-4c7d-444a-8159-458a03c9827f"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:32:47.013378+00"), OffsetDateTime.parse("2020-01-09T11:39:00.898947+00"))));

                            mock2[0] = new MockResult(1, result2);
                            return mock2;
                        }else{
                            MockResult[] mock2 = new MockResult[1];
                            Result<Record11<UUID, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>, String, String, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 = create.newResult(FOLDER_ITEMS.FOLDER_ID, FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"), FOLDER_ITEMS.SYS_TRANSACTION, FOLDER_ITEMS.SYS_PERIOD, OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID.as("obj_ref_id"), OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"), OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"), OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"));
                            mock2[0] = new MockResult(0, result2);
                            return mock2;
                        }
                    }else {

                        MockResult[] mock2 = new MockResult[1];
                        Result<Record11<UUID, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>, String, String, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 = create.newResult(FOLDER_ITEMS.FOLDER_ID, FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"), FOLDER_ITEMS.SYS_TRANSACTION, FOLDER_ITEMS.SYS_PERIOD, OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID.as("obj_ref_id"), OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"), OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"), OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"));

                        result2.add(create
                                .newRecord(FOLDER_ITEMS.FOLDER_ID, FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"), FOLDER_ITEMS.SYS_TRANSACTION, FOLDER_ITEMS.SYS_PERIOD, OBJECT_REF.ID_NAMESPACE, OBJECT_REF.TYPE, OBJECT_REF.ID.as("obj_ref_id"), OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"), OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"), OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"))
                                .values(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("44282ddd-4c7d-444a-8159-458a03c9827f"), UUID.fromString("af550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2020-01-15 22:22:22.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T15:43:18.213207+00"), null), "middle leave UPDATED", "COMPOSITION", UUID.fromString("44282ddd-4c7d-444a-8159-458a03c9827f"), UUID.fromString("af550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2020-01-15 22:22:22.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T15:43:11.287968+00"), null)));
                        mock2[0] = new MockResult(1, result2);
                        return mock2;
                    }
                }else if(sql2.toLowerCase().contains("select \"folder_union_fol_hist\".\"id\", \"folder_union_fol_hist\".\"in_contribution\", \"folder_union_fol_hist\".\"name\", \"folder_union_fol_hist\".\"archetype_node_id\"")) {

                    if (((UUID) ctx.bindings()[1]).equals(UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"))) {

                        MockResult[] mock2 = new MockResult[1];
                        Result<Record8<UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 = create.newResult(FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD);

                        result2.add(create
                                .newRecord(FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD)
                                .values(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), "folder_archetype_nameLeave1", "folder_archetypeLeave.v1", true, null, Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T15:43:11.287968+00"), null)));
                        mock2[0] = new MockResult(1, result2);
                        return mock2;
                    }else{

                        MockResult[] mock2 = new MockResult[1];
                        Result<Record8<UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 = create.newResult(FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD);

                        result2.add(create
                                .newRecord(FOLDER.ID, FOLDER.IN_CONTRIBUTION, FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS, FOLDER.SYS_TRANSACTION, FOLDER.SYS_PERIOD)
                                .values(UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"), UUID.fromString("af550555-ec91-4025-838d-09ddb4e473cb"), "folder_archetype_nameLeave1", "folder_archetypeLeave.v1", true, null, Timestamp.valueOf("2020-01-15 22:22:22.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:28:06.7612+00"), OffsetDateTime.parse("2020-01-09T11:39:00.898947+00"))));
                        mock2[0] = new MockResult(1, result2);
                        return mock2;
                    }
                }else if(sql2.toLowerCase().contains("with")) {

                    if (((Timestamp) ctx.bindings()[0]).before(Timestamp.valueOf("2020-01-15 22:22:22.688"))) {

                        //TABLE RETURNED FOR DELETE FOLDER TEST
                        if (sql2.toLowerCase().contains("with recursive \"subfolders\" as ((select \"alias_12246427\".\"parent_folder\", \"alias_12246427\".\"child_folder\"")) {
                            //  if(((UUID)ctx.bindings()[0]).compareTo(UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"))==0) {

                            DateTimeFormatter dateTimeFormatter
                                    = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                            DateTime expected
                                    = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                            TimeZone.getTimeZone("UTC");
                            MockResult[] mock2 = new MockResult[1];
                            Result<Record15<UUID, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>, UUID, Timestamp, UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 =
                                    create.newResult(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER_HIERARCHY.PARENT_FOLDER.as("parent_folder_id"), FOLDER_HIERARCHY.SYS_TRANSACTION.as("latest_sys_transaction"), FOLDER_HIERARCHY.PARENT_FOLDER.as("id"), FOLDER_HIERARCHY.IN_CONTRIBUTION.as("in_contribution_folder_info"), FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS.as("details"), FOLDER.SYS_TRANSACTION.as("sys_transaction_folder"), FOLDER.SYS_PERIOD.as("sys_period_folder"));

                            result2.add(create
                                    .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER_HIERARCHY.PARENT_FOLDER.as("parent_folder_id"), FOLDER_HIERARCHY.SYS_TRANSACTION.as("latest_sys_transaction"), FOLDER_HIERARCHY.PARENT_FOLDER.as("id"), FOLDER_HIERARCHY.IN_CONTRIBUTION.as("in_contribution_folder_info"), FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS.as("details"), FOLDER.SYS_TRANSACTION.as("sys_transaction_folder"), FOLDER.SYS_PERIOD.as("sys_period_folder"))
                                    .values(UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"), UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:28:32.089794+00"), null), UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"), Timestamp.valueOf("2019-12-05 19:00:00.688"), UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), "folder_archetype_root init", "folder_archetypeRoot.v1", true, null, Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:27:53.370395+00"), null)));
                            result2.add(create
                                    .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER_HIERARCHY.PARENT_FOLDER.as("parent_folder_id"), FOLDER_HIERARCHY.SYS_TRANSACTION.as("latest_sys_transaction"), FOLDER_HIERARCHY.PARENT_FOLDER.as("id"), FOLDER_HIERARCHY.IN_CONTRIBUTION.as("in_contribution_folder_info"), FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS.as("details"), FOLDER.SYS_TRANSACTION.as("sys_transaction_folder"), FOLDER.SYS_PERIOD.as("sys_period_folder"))
                                    .values(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:44:43.712587+00"), null), UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), Timestamp.valueOf("2019-12-05 19:00:00.688"), UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), "folder_archetype_middle1", "folder_archetypemiddle.v1", true, null, Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:27:53.370395+00"), null)));


                            mock2[0] = new MockResult(5, result2);
                            return mock2;
                            // }

                        }

                        throw new SQLException("SQL statement not expected. Consider enhancing or revising the FolderMockDataProvider in order to ensure the controlled behaviour of your code: " + sql2);

                    }else{

                    //TABLE RETURNED FOR DELETE FOLDER TEST
                    if (sql2.toLowerCase().contains("with recursive \"subfolders\" as ((select \"alias_12246427\".\"parent_folder\", \"alias_12246427\".\"child_folder\"")) {
                        DateTimeFormatter dateTimeFormatter
                                = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS");
                        DateTime expected
                                = DateTime.parse("2019-06-13 18:10:33.76", dateTimeFormatter);
                        TimeZone.getTimeZone("UTC");
                        MockResult[] mock2 = new MockResult[1];
                        Result<Record15<UUID, UUID, UUID, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>, UUID, Timestamp, UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>> result2 =
                                create.newResult(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER_HIERARCHY.PARENT_FOLDER.as("parent_folder_id"), FOLDER_HIERARCHY.SYS_TRANSACTION.as("latest_sys_transaction"), FOLDER_HIERARCHY.PARENT_FOLDER.as("id"), FOLDER_HIERARCHY.IN_CONTRIBUTION.as("in_contribution_folder_info"), FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS.as("details"), FOLDER.SYS_TRANSACTION.as("sys_transaction_folder"), FOLDER.SYS_PERIOD.as("sys_period_folder"));

                        result2.add(create
                                .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER_HIERARCHY.PARENT_FOLDER.as("parent_folder_id"), FOLDER_HIERARCHY.SYS_TRANSACTION.as("latest_sys_transaction"), FOLDER_HIERARCHY.PARENT_FOLDER.as("id"), FOLDER_HIERARCHY.IN_CONTRIBUTION.as("in_contribution_folder_info"), FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS.as("details"), FOLDER.SYS_TRANSACTION.as("sys_transaction_folder"), FOLDER.SYS_PERIOD.as("sys_period_folder"))
                                .values(UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"), UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:28:32.089794+00"), null), UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"), Timestamp.valueOf("2019-12-05 19:00:00.688"), UUID.fromString("7f069129-7312-447b-bd71-567305a9a871"), UUID.fromString("00550555-ec91-4025-838d-09ddb4e473cb"), "folder_archetype_root init", "folder_archetypeRoot.v1", true, null, Timestamp.valueOf("2019-12-05 19:00:00.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:27:53.370395+00"), null)));
                        result2.add(create
                                .newRecord(FOLDER_HIERARCHY.PARENT_FOLDER, FOLDER_HIERARCHY.CHILD_FOLDER, FOLDER_HIERARCHY.IN_CONTRIBUTION, FOLDER_HIERARCHY.SYS_TRANSACTION, FOLDER_HIERARCHY.SYS_PERIOD, FOLDER_HIERARCHY.PARENT_FOLDER.as("parent_folder_id"), FOLDER_HIERARCHY.SYS_TRANSACTION.as("latest_sys_transaction"), FOLDER_HIERARCHY.PARENT_FOLDER.as("id"), FOLDER_HIERARCHY.IN_CONTRIBUTION.as("in_contribution_folder_info"), FOLDER.NAME, FOLDER.ARCHETYPE_NODE_ID, FOLDER.ACTIVE, FOLDER.DETAILS.as("details"), FOLDER.SYS_TRANSACTION.as("sys_transaction_folder"), FOLDER.SYS_PERIOD.as("sys_period_folder"))
                                .values(UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("eda6951b-5506-4726-89dc-7032872997ce"), UUID.fromString("af550555-ec91-4025-838d-09ddb4e473cb"), Timestamp.valueOf("2020-01-15 22:22:22.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:44:43.712587+00"), null), UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), Timestamp.valueOf("2020-01-15 22:22:22.688"), UUID.fromString("129dc79c-e0bc-4946-bfa6-28ce609bbd2c"), UUID.fromString("af550555-ec91-4025-838d-09ddb4e473cb"), "folder_archetype_middle1", "folder_archetypemiddle.v1", true, null, Timestamp.valueOf("2020-01-15 22:22:22.688"), new AbstractMap.SimpleEntry<>(OffsetDateTime.parse("2020-01-09T11:27:53.370395+00"), null)));


                        mock2[0] = new MockResult(5, result2);
                        return mock2;

                    }
                    throw new SQLException("SQL statement not expected. Consider enhancing or revising the FolderMockDataProvider in order to ensure the controlled behaviour of your code: " + sql2);
                }
                }
                    throw new SQLException("Statement not currently supported. Consider enhancing or revising the FolderMockDataProvider: " + sql2);
                }
        }
