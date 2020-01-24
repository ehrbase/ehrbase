/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.api.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Diese Exception wird geworfen, wenn in einem Switch-Block ein Case eingetreten ist, der nicht korrekt behandelt werden kann. Eigentlich
 * sollte dieser Fall nicht auftreten (außer man ignoriert die Eclipse-Warning zum unvollständigen Switch).
 * </p>
 * <p>
 * Typische Verwendungen in einer Switch-Definition:
 * <ol>
 * <li>Im unerwarteten default-Case</li>
 * <li>Im explizit nicht sinnvoll behandelbaren Case</li>
 * <li>Im unerwarteten default-Case eines Switch mit Fall-Through von nicht sinnvoll behandelbaren Cases (Kombination aus 1. und 2.)</li>
 * </ol>
 * <p>
 * Siehe dazu auch: <a href="https://owl.symeda/wiki/doku.php?id=code-conventions#vollstaendige_switch-statements">Code Conventions -
 * Vollständige Switch-Statements</a>
 * </p>
 *
 * @author Jan Falkenstern, Stefan Kock
 */
public class UnexpectedSwitchCaseException extends RuntimeException {

    private static final long serialVersionUID = 5695009820197756438L;

    /**
     * Erzeugt aus dem übergebenen enumValue eine message.<br />
     * Ausgegeben in der Exception-Message wird <code>enumValue.name()</code>,
     * damit z.B. bei lokalisierten Enums immer denselbe Wert ausgegeben wird.
     *
     * @param enumValue must not be null
     */
    public UnexpectedSwitchCaseException(Enum<?> enumValue) {
        super(formatMessage(enumValue, null));
    }

    /**
     * Erzeugt aus dem übergebenen enumValue eine message.<br />
     * Ausgegeben in der Exception-Message wird <code>enumValue.name()</code>,
     * damit z.B. bei lokalisierten Enums immer denselbe Wert ausgegeben wird.
     *
     * @param enumValue         must not be null
     * @param additionalMessage additional text for generated message
     */
    public UnexpectedSwitchCaseException(Enum<?> enumValue, String additionalMessage) {
        super(formatMessage(enumValue, additionalMessage));
    }

    /**
     * Erzeugt aus dem übergebenen intValue eine message.
     *
     * @param intValue
     */
    public UnexpectedSwitchCaseException(Integer intValue) {
        super(formatMessage(intValue, null));
    }

    /**
     * Erzeugt aus dem übergebenen intValue eine message.
     *
     * @param intValue
     * @param additionalMessage additional text for generated message
     */
    public UnexpectedSwitchCaseException(Integer intValue, String additionalMessage) {
        super(formatMessage(intValue, additionalMessage));
    }

    /**
     * Erzeugt aus dem übergebenen stringValue eine message.
     *
     * <strong>
     * <h2>Achtung: Breaking Change!</h2>
     * <p>Mit Version 0.1.3-SNAPSHOT hat sich die Bedeutung des Konstruktors geändert!<br/>
     * Statt der Meldung wird jetzt nur der Wert angegeben!</p>
     * </strong>
     *
     * @param stringValue
     */
    public UnexpectedSwitchCaseException(String stringValue) {
        super(formatMessage(stringValue, null));
    }

    /**
     * Erzeugt aus dem übergebenen stringValue eine message.
     *
     * @param stringValue
     * @param additionalMessage additional text for generated message
     */
    public UnexpectedSwitchCaseException(String stringValue, String additionalMessage) {
        super(formatMessage(stringValue, additionalMessage));
    }

    /**
     * Erzeugt die Message der {@link UnexpectedSwitchCaseException} anhand der übergebenen Parameter mit Ausgabe von
     * <code>enumValue.getClass().getSimpleName()</code>.<br />
     * Ausgegeben in der Exception-Message wird <code>enumValue.name()</code>,
     * damit z.B. bei lokalisierten Enums immer denselbe Wert ausgegeben wird.
     *
     * @param enumValue         must not be null
     * @param additionalMessage additional text for generated message (optional)
     * @return
     */
    public static String formatMessage(Enum<?> enumValue, String additionalMessage) {

        return formatMessage(enumValue.getClass().getSimpleName(), enumValue.name(), additionalMessage);
    }

    /**
     * Erzeugt die Message der {@link UnexpectedSwitchCaseException} anhand der übergebenen Parameter.
     *
     * @param value
     * @param additionalMessage additional text for generated message (optional)
     * @return
     */
    public static String formatMessage(Object value, String additionalMessage) {

        return formatMessage(null, value, additionalMessage);
    }

    /**
     * Erzeugt die Message der {@link UnexpectedSwitchCaseException} anhand der übergebenen Parameter.
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