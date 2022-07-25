/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.plugin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KeyValueEntryRepository {
    public List<KeyValueEntry> findByPluginId(String uid);

    public Optional<KeyValueEntry> findByPluginIdAndKey(String id, String key);

    public Optional<KeyValueEntry> findBy(UUID uid);

    public KeyValueEntry save(KeyValueEntry kve);

    public boolean deleteBy(UUID uid);
}
