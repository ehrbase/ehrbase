/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.application.config;

import java.sql.SQLException;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Stefan Spiska
 */
@Component
public class SetTableRightsMigration extends BaseCallback {

    @Value("${spring.datasource.username}")
    private String ehrbaseUser;

    @Override
    public void handle(Event event, Context context) {
        if (event == Event.AFTER_MIGRATE) {

            java.sql.Statement st;
            try {
                st = context.getConnection().createStatement();
                st.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA ehr to " + ehrbaseUser);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
