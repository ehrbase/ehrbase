/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AQL features that can be optionally enabled.
 *
 * <ul>
 *     <li><code>pg-llj-workaround</code> Enables fix for an old postgresql bug where filters in lateral left joins inside a left join are not respected, default: <code>true</code></li>
 *     <li><code>experimental.aql-on-folder.enabled</code> if enabled allow to query <code>EHR</code> <code>FOLDER</code> using AQL, default: <code>false</code></li>
 * </ul>
 */
@ConfigurationProperties(prefix = "ehrbase.aql")
public record AqlConfigurationProperties(boolean pgLljWorkaround, Experimental experimental) {

    public AqlConfigurationProperties(boolean pgLljWorkaround, Experimental experimental) {
        this.pgLljWorkaround = pgLljWorkaround;
        this.experimental = Optional.ofNullable(experimental)
                .orElseGet(() -> new Experimental(new Experimental.AqlOnFolder(false)));
    }

    public record Experimental(AqlOnFolder aqlOnFolder) {

        public record AqlOnFolder(boolean enabled) {}
    }
}
