/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * This exception is thrown when a case has occurred in a switch block that cannot be handled correctly.
 * </p>
 * <p>
 * Typical uses in a switch statement:
 * <ol>
 * <li>unexpected default case</li>
 * <li>explicitly not sensibly treatable case</li>
 * <li>unexpected default case with fall-through of not sensibly treatable case (combination of 1. and 2.)</li>
 * </ol>
 *
 * @author Jan Falkenstern, Stefan Kock
 */
public class UnexpectedSwitchCaseException extends RuntimeException {

    private static final long serialVersionUID = 5695009820197756438L;

    /**
     * Creates a message from the <code>enumValue</code> parameter.<br />
     * The message is based on <code>enumValue.name()</code>,
     * so that the format is stable, e.g. in case of i18n.
     *
     * @param enumValue must not be null
     */
    public UnexpectedSwitchCaseException(Enum<?> enumValue) {
        super(formatMessage(enumValue, null));
    }

    /**
     * Creates a message from the <code>enumValue</code> parameter.<br />
     * The message is based on <code>enumValue.name()</code>,
     * so that the format is stable, e.g. in case of i18n.
     *
     * @param enumValue         must not be null
     * @param additionalMessage additional text for generated message
     */
    public UnexpectedSwitchCaseException(Enum<?> enumValue, String additionalMessage) {
        super(formatMessage(enumValue, additionalMessage));
    }

    /**
     * Creates a message from the <code>intValue</code> parameter
     *
     * @param intValue
     */
    public UnexpectedSwitchCaseException(Integer intValue) {
        super(formatMessage(intValue, null));
    }

    /**
     * Creates a message from the <code>intValue</code> parameter
     *
     * @param intValue
     * @param additionalMessage additional text for generated message
     */
    public UnexpectedSwitchCaseException(Integer intValue, String additionalMessage) {
        super(formatMessage(intValue, additionalMessage));
    }

    /**
     * Creates a message from the <code>stringValue</code> parameter
     *
     * @param stringValue
     */
    public UnexpectedSwitchCaseException(String stringValue) {
        super(formatMessage(stringValue, null));
    }

    /**
     * Creates a message from the <code>stringValue</code> parameter
     *
     * @param stringValue
     * @param additionalMessage additional text for generated message
     */
    public UnexpectedSwitchCaseException(String stringValue, String additionalMessage) {
        super(formatMessage(stringValue, additionalMessage));
    }

    /**
     * Creates the message of a {@link UnexpectedSwitchCaseException} containing the simple class name of the <code>enumValue</code> parameter.
     * The message is based on <code>enumValue.name()</code>,
     * so that the format is stable, e.g. in case of i18n.
     *
     * @param enumValue         must not be null
     * @param additionalMessage additional text for generated message (optional)
     * @return
     */
    public static String formatMessage(Enum<?> enumValue, String additionalMessage) {

        return formatMessage(enumValue.getClass().getSimpleName(), enumValue.name(), additionalMessage);
    }

    /**
     * Creates the message of a {@link UnexpectedSwitchCaseException} based on the given parameters.
     *
     * @param value
     * @param additionalMessage additional text for generated message (optional)
     * @return
     */
    public static String formatMessage(Object value, String additionalMessage) {

        return formatMessage(null, value, additionalMessage);
    }

    /**
     * Creates the message of a {@link UnexpectedSwitchCaseException} based on the given parameters.
     *
     * @param type              e.g. <code>enumValue.getClass().getSimpleName()</code>
     * @param value             unsupported value in switch
     * @param additionalMessage additional text for generated message (optional)
     * @return
     */
    public static String formatMessage(String type, Object value, String additionalMessage) {

        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected value");

        if (StringUtils.isNotBlank(type)) {
            sb.append(" of ");
            sb.append(type);
        }

        sb.append(": '");
        sb.append(value);
        sb.append("'");

        if (StringUtils.isNotBlank(additionalMessage)) {
            sb.append("; ");
            sb.append(additionalMessage);
        }

        return sb.toString();
    }
}
