/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.rest.openehr.specification;

import static org.ehrbase.rest.BaseController.OPENEHR_AUDIT_DETAILS;
import static org.ehrbase.rest.BaseController.OPENEHR_VERSION;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Documents the `openehr-version` and `openehr-audit-details` request headers of a direct commit.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(in = ParameterIn.HEADER, name = OPENEHR_VERSION, description = """
                Version attributes of the commit, as comma-separated `key="value"` pairs. \
                Key: `lifecycle_state.code_string`, e.g. `lifecycle_state.code_string="532"`. \
                The deprecated REST 1.0.3 header `openEHR-VERSION.lifecycle_state`, with the key `code_string`, \
                is also accepted.""", schema = @Schema(type = "string"))
@Parameter(in = ParameterIn.HEADER, name = OPENEHR_AUDIT_DETAILS, description = """
                Audit attributes of the commit, as comma-separated `key="value"` pairs; the header may be repeated. \
                Keys: `change_type.code_string`, `description.value`, `committer.name`, \
                `committer.external_ref.id`, `committer.external_ref.namespace`, `committer.external_ref.type`, \
                `system_id`, e.g. `change_type.code_string="251", committer.name="Dr. Jane Doe"`. \
                The deprecated REST 1.0.3 headers `openEHR-AUDIT_DETAILS.change_type`, \
                `openEHR-AUDIT_DETAILS.description` and `openEHR-AUDIT_DETAILS.committer` are also accepted; \
                their keys are relative to the attribute, e.g. `openEHR-AUDIT_DETAILS.committer: name="Dr. Jane Doe"`.""", schema = @Schema(type = "string"))
public @interface CommitAuditHeaderDocs {}
