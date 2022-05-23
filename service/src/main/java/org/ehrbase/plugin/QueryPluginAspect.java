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
package org.ehrbase.plugin;

import static org.ehrbase.plugin.PluginHelper.PLUGIN_MANAGER_PREFIX;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ehrbase.plugin.dto.QueryWithParameters;
import org.ehrbase.plugin.extensionpoints.QueryExtensionPoint;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Stefan Spiska
 */
@Component
@Aspect
@ConditionalOnProperty(prefix = PLUGIN_MANAGER_PREFIX, name = "enable", havingValue = "true")
public class QueryPluginAspect extends AbstractPluginAspect<QueryExtensionPoint> {

    public QueryPluginAspect(ListableBeanFactory beanFactory) {
        super(beanFactory, QueryExtensionPoint.class);
    }

    /**
     * Handle Extension-points for Query execute
     *
     * @param pjp
     * @return
     * @see <a href="I_EHR_SERVICE in openEHR Platform Service
     * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
     */
    @Around("inServiceLayerPC() && " + "execution(* org.ehrbase.api.service.QueryService.query(..))")
    public Object aroundQueryExecute(ProceedingJoinPoint pjp) {
        return proceedWithPluginExtensionPoints(
                pjp,
                QueryExtensionPoint::aroundQueryExecution,
                args -> new QueryWithParameters((String) args[0], (Map<String, Object>) args[1]),
                (i, args) -> {
                    args[0] = i.getQuery();
                    args[1] = i.getParameters();
                    return args;
                });
    }
}
