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
 * Integration tests for ehr_system.view_catalog table.
 * Connects directly to PG18 testcontainer — no Spring context needed.
 */
class ViewCatalogIT {

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

    @Test
    void registerView() throws Exception {
        String viewName =
                "v_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ehr_system.view_catalog (view_name, view_schema, view_type, template_id, "
                            + "source, description, sys_tenant) "
                            + "VALUES (?, 'ehr_views', 'template', 'test.template.v1', 'auto', "
                            + "'Auto-generated view for test template', 1) "
                            + "RETURNING id, view_name, view_type, source");
            ps.setString(1, viewName);
            ResultSet rs = ps.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(UUID.fromString(rs.getString("id")).version()).isEqualTo(7);
            assertThat(rs.getString("view_name")).isEqualTo(viewName);
            assertThat(rs.getString("view_type")).isEqualTo("template");
            assertThat(rs.getString("source")).isEqualTo("auto");
        }
    }

    @Test
    void listViewsByType() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        try (Connection conn = connect()) {
            // Insert template views
            for (int i = 1; i <= 2; i++) {
                PreparedStatement ps =
                        conn.prepareStatement("INSERT INTO ehr_system.view_catalog (view_name, view_schema, view_type, "
                                + "source, description, sys_tenant) "
                                + "VALUES (?, 'ehr_views', 'template', 'auto', 'Template view', 1)");
                ps.setString(1, "v_tpl_" + suffix + "_" + i);
                ps.execute();
            }

            // Insert compliance views
            for (int i = 1; i <= 3; i++) {
                PreparedStatement ps =
                        conn.prepareStatement("INSERT INTO ehr_system.view_catalog (view_name, view_schema, view_type, "
                                + "source, description, sys_tenant) "
                                + "VALUES (?, 'ehr_views', 'compliance', 'auto', 'Compliance view', 1)");
                ps.setString(1, "v_comp_" + suffix + "_" + i);
                ps.execute();
            }

            // Query template views with our suffix
            PreparedStatement queryTpl = conn.prepareStatement("SELECT view_name FROM ehr_system.view_catalog "
                    + "WHERE view_type = 'template' AND view_name LIKE ?");
            queryTpl.setString(1, "v_tpl_" + suffix + "%");
            ResultSet rsTpl = queryTpl.executeQuery();
            List<String> templateViews = new ArrayList<>();
            while (rsTpl.next()) {
                templateViews.add(rsTpl.getString("view_name"));
            }
            assertThat(templateViews).hasSize(2);

            // Query compliance views with our suffix
            PreparedStatement queryComp = conn.prepareStatement("SELECT view_name FROM ehr_system.view_catalog "
                    + "WHERE view_type = 'compliance' AND view_name LIKE ?");
            queryComp.setString(1, "v_comp_" + suffix + "%");
            ResultSet rsComp = queryComp.executeQuery();
            List<String> complianceViews = new ArrayList<>();
            while (rsComp.next()) {
                complianceViews.add(rsComp.getString("view_name"));
            }
            assertThat(complianceViews).hasSize(3);
        }
    }

    @Test
    void removeTemplateViews() throws Exception {
        String templateId = "removable.template." + UUID.randomUUID();
        try (Connection conn = connect()) {
            // Insert views for a specific template_id
            for (int i = 1; i <= 2; i++) {
                String viewName =
                        "v_rm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                PreparedStatement ps =
                        conn.prepareStatement("INSERT INTO ehr_system.view_catalog (view_name, view_schema, view_type, "
                                + "template_id, source, description, sys_tenant) "
                                + "VALUES (?, 'ehr_views', 'template', ?, 'auto', 'To be removed', 1)");
                ps.setString(1, viewName);
                ps.setString(2, templateId);
                ps.execute();
            }

            // Verify they exist
            PreparedStatement countBefore =
                    conn.prepareStatement("SELECT count(*) FROM ehr_system.view_catalog WHERE template_id = ?");
            countBefore.setString(1, templateId);
            ResultSet rsBefore = countBefore.executeQuery();
            rsBefore.next();
            assertThat(rsBefore.getInt(1)).isEqualTo(2);

            // Delete by template_id
            PreparedStatement del = conn.prepareStatement("DELETE FROM ehr_system.view_catalog WHERE template_id = ?");
            del.setString(1, templateId);
            int deleted = del.executeUpdate();
            assertThat(deleted).isEqualTo(2);

            // Verify removed
            PreparedStatement countAfter =
                    conn.prepareStatement("SELECT count(*) FROM ehr_system.view_catalog WHERE template_id = ?");
            countAfter.setString(1, templateId);
            ResultSet rsAfter = countAfter.executeQuery();
            rsAfter.next();
            assertThat(rsAfter.getInt(1)).isEqualTo(0);
        }
    }
}
