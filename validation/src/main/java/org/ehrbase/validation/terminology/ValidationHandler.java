package org.ehrbase.validation.terminology;

import java.lang.invoke.MethodHandle;

public class ValidationHandler {

    private final Class clazz;
    private final MethodHandle check;

    public ValidationHandler(Class clazz, MethodHandle check) {
        this.clazz = clazz;
        this.check = check;
    }

    public Class getClazz() {
        return clazz;
    }

    public MethodHandle check() {
        return check;
    }
}
