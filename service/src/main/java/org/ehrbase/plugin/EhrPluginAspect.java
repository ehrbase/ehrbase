/*
 * Copyright (c) 2022. vitasystems GmbH and Hannover Medical School.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ehrbase.plugin;

import static org.ehrbase.plugin.PluginHelper.PLUGIN_MANAGER_PREFIX;

import com.nedap.archie.rm.ehr.EhrStatus;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ehrbase.plugin.dto.EhrStatusWithEhrId;
import org.ehrbase.plugin.extensionpoints.EhrExtensionPointInterface;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Stefan Spiska
 */
@Component
@Aspect
@ConditionalOnProperty(prefix = PLUGIN_MANAGER_PREFIX, name = "enable", havingValue = "true")
public class EhrPluginAspect extends AbstartPluginAspect<EhrExtensionPointInterface> {

  public EhrPluginAspect(ListableBeanFactory beanFactory) {
    super(beanFactory, EhrExtensionPointInterface.class);
  }

  /**
   * Handle Extension-points for Ehr create
   *
   * @param pjp
   * @return
   * @see <a href="I_EHR_SERVICE in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  @Around("execution(* org.ehrbase.api.service.EhrService.create(..))")
  public Object aroundCreateEhr(ProceedingJoinPoint pjp) {

    Chain<EhrExtensionPointInterface> chain = getChain();

    Object[] args = pjp.getArgs();
    EhrStatusWithEhrId input = new EhrStatusWithEhrId((EhrStatus) args[1], (UUID) args[0]);

    return handleChain(
        chain,
        l -> (l::aroundCreation),
        input,
        i -> {
          args[1] = i.getEhrStatus();
          args[0] = i.getEhrId();

          return (UUID) proceed(pjp, args);
        });
  }

  /**
   * Handle Extension-points for Ehr create
   *
   * @param pjp
   * @return
   * @see <a href="I_EHR_SERVICE in openEHR Platform Service
   *     Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface</a>
   */
  @Around("execution(* org.ehrbase.api.service.EhrService.updateStatus(..))")
  public Object aroundUpdateEhrStatus(ProceedingJoinPoint pjp) {

    Chain<EhrExtensionPointInterface> chain = getChain();

    Object[] args = pjp.getArgs();
    EhrStatusWithEhrId input = new EhrStatusWithEhrId((EhrStatus) args[1], (UUID) args[0]);

    return handleChain(
        chain,
        l -> (l::aroundUpdate),
        input,
        i -> {
          args[1] = i.getEhrStatus();
          args[0] = i.getEhrId();

          return (UUID) proceed(pjp, args);
        });
  }

  private Chain<EhrExtensionPointInterface> getChain() {
    return buildChain(getExtensionPointInterfaceList(), new EhrExtensionPointInterface() {});
  }
}
