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
package org.ehrbase.service;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.repository.SystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * @author Stefan Spiska
 */
@Service
public class SystemService {

    private final SystemRepository systemRepository;
    private final ServerConfig serverConfig;

    private final Logger logger = LoggerFactory.getLogger(SystemService.class);

    private UUID systemId;

    public SystemService(SystemRepository systemRepository, ServerConfig serverConfig) {
        this.systemRepository = systemRepository;
        this.serverConfig = serverConfig;
    }

    @PostConstruct
    private void init() {

        Optional<UUID> uuid = systemRepository.findSystemId(serverConfig.getNodename());

        if (uuid.isEmpty()) {

            try {

                systemRepository.commit(
                        systemRepository.toRecord(serverConfig.getNodename(), "DEFAULT RUNNING SYSTEM"));
                // Might fail do to concurrent inserts
            } catch (DataIntegrityViolationException ex) {
                logger.info(ex.getMessage(), ex);
            }
            uuid = systemRepository.findSystemId(serverConfig.getNodename());
        }

        systemId = uuid.orElseThrow(() -> new InternalServerException("Can not fetch System Id"));
    }

    public UUID getSystemUuid() {

        return systemId;
    }
}
