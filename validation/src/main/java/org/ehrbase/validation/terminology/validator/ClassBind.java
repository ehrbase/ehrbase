package org.ehrbase.validation.terminology.validator;

public class ClassBind {

    private Class validatorClass;

    public ClassBind(Class validatorClass) {
        this.validatorClass = validatorClass;
    }

    public Class rmClass(){
        validatorClass.getDeclaredFields();
        return null;
    }
}
