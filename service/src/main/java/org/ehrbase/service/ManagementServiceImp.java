package org.ehrbase.service;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.service.ManagementService;
import org.jooq.DSLContext;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManagementServiceImp extends BaseServiceImp implements ManagementService {

    public ManagementServiceImp(
            KnowledgeCacheService knowledgeCacheService, DSLContext dslContext, ServerConfig serverConfig) {
        super(knowledgeCacheService, dslContext, serverConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLogLevel() {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        return rootLogger.getLevel().levelStr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String setLogLevel(String logLevel) {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(ch.qos.logback.classic.Level.valueOf(logLevel));
        return rootLogger.getLevel().levelStr;
    }
}
