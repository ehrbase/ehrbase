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
package org.ehrbase.repository.composition;

import java.util.List;
import java.util.Map;

/**
 * Extracted column data from an RM Composition, ready for INSERT into template tables.
 *
 * @param mainTableValues   column name → value for the main template table
 * @param childTableValues  child table name → list of row maps for repeating structures
 */
public record CompositionTableData(
        Map<String, Object> mainTableValues, Map<String, List<Map<String, Object>>> childTableValues) {}
