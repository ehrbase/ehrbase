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
package org.ehrbase.api.repsitory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KeyValuePairRepository {

    public List<KeyValuePair> findAllBy(String context);

    public Optional<KeyValuePair> findBy(String context, String key);

    public Optional<KeyValuePair> findBy(UUID uid);

    public KeyValuePair save(KeyValuePair kve);

    public boolean deleteBy(UUID uid);
}
