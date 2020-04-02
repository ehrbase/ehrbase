package org.ehrbase.application.util;

import org.ehrbase.application.config.SecurityYAMLConfig.AuthTypes;
import org.springframework.core.convert.converter.Converter;

public class StringToEnumConverter implements Converter<String, AuthTypes> {
    @Override
    public AuthTypes convert(String source) {
        try {
            return AuthTypes.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AuthTypes.NONE;
        }
    }
}
