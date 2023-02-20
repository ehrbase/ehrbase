package org.ehrbase.api.service;

/**
 * Management service to get information about log level EHRbase
 */
public interface ManagementService extends BaseService {

    /**
     * Returns log level which is set. Possible values: DEBUG, INFO, ERROR, WARN, OFF
     *
     * @return Level
     */
    String getLogLevel();

    /**
     * Set log level. Possible values: DEBUG, INFO, ERROR, WARN, OFF.
     * Default log level is DEBUG. If {logLevel} == Wrong Status
     *
     * @return JVM Version string
     */
    String setLogLevel(String logLevel);

}
