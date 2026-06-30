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
package org.ehrbase.configuration.config.security;

import java.util.List;
import org.ehrbase.configuration.config.security.SecurityProperties.AuthTypes;

/**
 * <p>Rules are bound from configuration (see {@link SecurityProperties#getAdditionalAuthorizations()}) and applied by
 * the matching {@code SecurityConfig} implementation.
 *
 * @param authType the authentication type this rule applies to; a rule is only applied on the matching auth chain
 * @param pathPattern the ant-style request path to secure
 */
public record EndpointAuthorization(AuthTypes authType, String pathPattern, List<String> roles) {}
