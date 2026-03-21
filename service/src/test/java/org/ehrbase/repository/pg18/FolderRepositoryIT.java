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
package org.ehrbase.repository.pg18;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.ehrbase.test.EhrbasePostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for ehr_system.ehr_folder and ehr_system.ehr_folder_item tables.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class FolderRepositoryIT {

    private static EhrbasePostgreSQLContainer pg;

    @BeforeAll
    static void startContainer() {
        pg = EhrbasePostgreSQLContainer.sharedInstance();
    }

    private Connection connect() throws Exception {
        Connection conn = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        conn.createStatement().execute("SET ehrbase.current_tenant = '1'");
        return conn;
    }

    private String createEhr(Connection conn) throws Exception {
        ResultSet rs = conn.createStatement()
                .executeQuery(
                        "INSERT INTO ehr_system.ehr (subject_id, subject_namespace, sys_tenant) "
                                + "VALUES ('folder-patient-" + UUID.randomUUID() + "', 'ehr.folder.org', 1) "
                                + "RETURNING id");
        rs.next();
        return rs.getString("id");
    }

    @Test
    void createFolderHierarchy() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            String prefix = "h" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String rootPath = prefix;
            String clinicalPath = prefix + ".clinical";
            String labPath = prefix + ".clinical.lab";

            // Insert root folder
            PreparedStatement psRoot = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::ext.ltree, 'Root', 'creation', 'committer', 1) RETURNING id");
            psRoot.setString(1, ehrId);
            psRoot.setString(2, rootPath);
            ResultSet rsRoot = psRoot.executeQuery();
            assertThat(rsRoot.next()).isTrue();

            // Insert clinical folder
            PreparedStatement psClinical = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::ext.ltree, 'Clinical', 'creation', 'committer', 1) RETURNING id");
            psClinical.setString(1, ehrId);
            psClinical.setString(2, clinicalPath);
            ResultSet rsClinical = psClinical.executeQuery();
            assertThat(rsClinical.next()).isTrue();

            // Insert lab folder
            PreparedStatement psLab = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::ext.ltree, 'Lab', 'creation', 'committer', 1) RETURNING id");
            psLab.setString(1, ehrId);
            psLab.setString(2, labPath);
            ResultSet rsLab = psLab.executeQuery();
            assertThat(rsLab.next()).isTrue();

            // Verify all 3 folders exist
            PreparedStatement query = conn.prepareStatement(
                    "SELECT name, path::text FROM ehr_system.ehr_folder "
                            + "WHERE ehr_id = ?::uuid ORDER BY path");
            query.setString(1, ehrId);
            ResultSet rs = query.executeQuery();

            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            assertThat(names).containsExactly("Root", "Clinical", "Lab");
        }
    }

    @Test
    void queryDescendants() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            String prefix = "d" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            // Create hierarchy: prefix, prefix.clinical, prefix.clinical.lab, prefix.clinical.radiology
            String[][] folders = {
                {prefix, "Root"},
                {prefix + ".clinical", "Clinical"},
                {prefix + ".clinical.lab", "Lab"},
                {prefix + ".clinical.radiology", "Radiology"}
            };
            for (String[] folder : folders) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                                + "VALUES (?::uuid, ?::ext.ltree, ?, 'creation', 'committer', 1)");
                ps.setString(1, ehrId);
                ps.setString(2, folder[0]);
                ps.setString(3, folder[1]);
                ps.execute();
            }

            // Query descendants of prefix.clinical (should return clinical, lab, radiology)
            PreparedStatement query = conn.prepareStatement(
                    "SELECT name FROM ehr_system.ehr_folder "
                            + "WHERE ehr_id = ?::uuid AND path <@ ?::ext.ltree ORDER BY path");
            query.setString(1, ehrId);
            query.setString(2, prefix + ".clinical");
            ResultSet rs = query.executeQuery();

            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            assertThat(names).containsExactly("Clinical", "Lab", "Radiology");
        }
    }

    @Test
    void queryAncestors() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            String prefix = "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            // Create hierarchy
            String[][] folders = {
                {prefix, "Root"},
                {prefix + ".clinical", "Clinical"},
                {prefix + ".clinical.lab", "Lab"}
            };
            for (String[] folder : folders) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                                + "VALUES (?::uuid, ?::ext.ltree, ?, 'creation', 'committer', 1)");
                ps.setString(1, ehrId);
                ps.setString(2, folder[0]);
                ps.setString(3, folder[1]);
                ps.execute();
            }

            // Query ancestors of prefix.clinical.lab (should return root, clinical, lab)
            PreparedStatement query = conn.prepareStatement(
                    "SELECT name FROM ehr_system.ehr_folder "
                            + "WHERE ehr_id = ?::uuid AND path @> ?::ext.ltree ORDER BY path");
            query.setString(1, ehrId);
            query.setString(2, prefix + ".clinical.lab");
            ResultSet rs = query.executeQuery();

            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            // @> returns nodes that are ancestors of (or equal to) the given path
            assertThat(names).containsExactly("Root", "Clinical", "Lab");
        }
    }

    @Test
    void addCompositionToFolder() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            String prefix = "fi" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            // Create folder
            PreparedStatement psFolder = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::ext.ltree, 'Documents', 'creation', 'committer', 1) RETURNING id");
            psFolder.setString(1, ehrId);
            psFolder.setString(2, prefix);
            ResultSet folderRs = psFolder.executeQuery();
            folderRs.next();
            String folderId = folderRs.getString("id");

            // Create template + composition
            String templateUnique = "folder.template." + UUID.randomUUID();
            PreparedStatement tps = conn.prepareStatement(
                    "INSERT INTO ehr_system.template (template_id, content, sys_tenant) "
                            + "VALUES (?, '<template/>', 1) RETURNING id");
            tps.setString(1, templateUnique);
            ResultSet tplRs = tps.executeQuery();
            tplRs.next();
            String templateId = tplRs.getString("id");

            PreparedStatement compPs = conn.prepareStatement(
                    "INSERT INTO ehr_system.composition (ehr_id, template_id, archetype_id, template_name, "
                            + "composer_name, sys_version, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 'openEHR-EHR-COMPOSITION.encounter.v1', "
                            + "'Encounter', 'Dr. Smith', 1, 'creation', 'committer', 1) RETURNING id");
            compPs.setString(1, ehrId);
            compPs.setString(2, templateId);
            ResultSet compRs = compPs.executeQuery();
            compRs.next();
            String compositionId = compRs.getString("id");

            // Link composition to folder
            PreparedStatement itemPs = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder_item (folder_id, composition_id, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, 1)");
            itemPs.setString(1, folderId);
            itemPs.setString(2, compositionId);
            itemPs.execute();

            // Verify the link
            PreparedStatement verifyPs = conn.prepareStatement(
                    "SELECT composition_id FROM ehr_system.ehr_folder_item WHERE folder_id = ?::uuid");
            verifyPs.setString(1, folderId);
            ResultSet verifyRs = verifyPs.executeQuery();

            assertThat(verifyRs.next()).isTrue();
            assertThat(verifyRs.getString("composition_id")).isEqualTo(compositionId);
        }
    }

    @Test
    void folderVersioning() throws Exception {
        try (Connection conn = connect()) {
            String ehrId = createEhr(conn);
            String prefix = "fv" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            // Create folder v1
            PreparedStatement psV1 = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (ehr_id, path, name, change_type, committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::ext.ltree, 'Documents v1', 'creation', 'committer-a', 1) "
                            + "RETURNING id, valid_period");
            psV1.setString(1, ehrId);
            psV1.setString(2, prefix);
            ResultSet rsV1 = psV1.executeQuery();
            rsV1.next();
            String folderId = rsV1.getString("id");

            // Archive v1 to history with closed valid_period
            conn.createStatement()
                    .execute("INSERT INTO ehr_system.ehr_folder_history "
                            + "SELECT id, ehr_id, parent_id, path, name, archetype_node_id, "
                            + "tstzrange(lower(valid_period), now()), sys_version, contribution_id, "
                            + "change_type, committed_at, committer_name, committer_id, sys_tenant "
                            + "FROM ehr_system.ehr_folder WHERE id = '" + folderId + "'");

            // Delete v1 from current
            conn.createStatement()
                    .execute("DELETE FROM ehr_system.ehr_folder WHERE id = '" + folderId + "'");

            // Insert v2
            PreparedStatement psV2 = conn.prepareStatement(
                    "INSERT INTO ehr_system.ehr_folder (id, ehr_id, path, name, sys_version, change_type, "
                            + "committer_name, sys_tenant) "
                            + "VALUES (?::uuid, ?::uuid, ?::ext.ltree, 'Documents v2', 2, 'modification', "
                            + "'committer-b', 1) RETURNING sys_version, name");
            psV2.setString(1, folderId);
            psV2.setString(2, ehrId);
            psV2.setString(3, prefix);
            ResultSet rsV2 = psV2.executeQuery();

            assertThat(rsV2.next()).isTrue();
            assertThat(rsV2.getInt("sys_version")).isEqualTo(2);
            assertThat(rsV2.getString("name")).isEqualTo("Documents v2");

            // Verify v1 in history
            ResultSet histRs = conn.createStatement()
                    .executeQuery("SELECT sys_version, name, upper_inf(valid_period) AS open "
                            + "FROM ehr_system.ehr_folder_history WHERE id = '" + folderId + "'");
            assertThat(histRs.next()).isTrue();
            assertThat(histRs.getInt("sys_version")).isEqualTo(1);
            assertThat(histRs.getString("name")).isEqualTo("Documents v1");
            assertThat(histRs.getBoolean("open")).isFalse();
        }
    }
}
