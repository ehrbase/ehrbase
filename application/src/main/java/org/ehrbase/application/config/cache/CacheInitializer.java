/*
 * Copyright 2021 vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.application.config.cache;

import javax.annotation.PostConstruct;
import org.ehrbase.service.KnowledgeCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes caches during application startup.
 *
 * @author Renaud Subiger
 * @since 1.0.0
 */
public class CacheInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(CacheInitializer.class);

  private final KnowledgeCacheService knowledgeCacheService;

  public CacheInitializer(KnowledgeCacheService knowledgeCacheService) {
    this.knowledgeCacheService = knowledgeCacheService;
  }

  @PostConstruct
  public void initialize() {
    LOG.info("Initializing EHRbase caches...");
    knowledgeCacheService.initializeCaches();
  }
}
