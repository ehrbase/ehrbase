/*
 * Copyright (c) 2019 Vitasystems GmbH,
 * Jake Smolka (Hannover Medical School),
 * Luis Marco-Ruiz (Hannover Medical School),
 * Stefan Spiska (Vitasystems GmbH).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.service;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import java.util.ArrayList;
import java.util.List;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.BaseService;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.ServiceDataAccess;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class BaseServiceImp implements BaseService {

  public static final String DEMOGRAPHIC = "DEMOGRAPHIC";
  public static final String PARTY = "PARTY";

  private final ServerConfig serverConfig;
  private final KnowledgeCacheService knowledgeCacheService;
  private final DSLContext context;

  @Autowired
  private IAuthenticationFacade authenticationFacade;

  public BaseServiceImp(KnowledgeCacheService knowledgeCacheService, DSLContext context,
      ServerConfig serverConfig) {
    this.knowledgeCacheService = knowledgeCacheService;
    this.context = context;
    this.serverConfig = serverConfig;
  }

  protected I_DomainAccess getDataAccess() {
    return new ServiceDataAccess(context, knowledgeCacheService, knowledgeCacheService,
        this.serverConfig);
  }

  /**
   * Get default system UUID.<br>
   * Internally makes use of configured local system's node name.
   * @return Default system UUID.
   */
  public UUID getSystemUuid() {
    return I_SystemAccess.createOrRetrieveLocalSystem(getDataAccess());
  }

  /**
   * Get default user UUID, derived from authenticated user via Spring Security.<br>
   * Internally checks and retrieves the matching user UUID, if it already exists with given info.
   * @return UUID of default user, derived from authenticated user.
   */
  protected UUID getUserUuid() {
    var name = authenticationFacade.getAuthentication().getName();
    List<DvIdentifier> identifiers = new ArrayList<>();
    var identifier = new DvIdentifier();
    identifier.setId(name);
    identifier.setIssuer("EHRbase");
    identifier.setAssigner("EHRbase");
    identifier.setType("EHRbase Security Authentication User");
    identifiers.add(identifier);
    // Following getOrCreate will check for matching party with given UUID, but as it is random, it also checks
    // for matching name + identifiers. So it will find already created parties for existing users.
    return new PersistedPartyProxy(getDataAccess())
        .getOrCreate("EHRbase Internal " + name, UUID.randomUUID().toString(), DEMOGRAPHIC, "User",
            PARTY, identifiers);
  }

  public ServerConfig getServerConfig() {
    return this.serverConfig;
  }

}
