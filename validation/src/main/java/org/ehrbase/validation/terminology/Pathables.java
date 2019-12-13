package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Pathable;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class Pathables {

    private ItemValidator itemValidator;
    private TerminologyInterface terminologyInterface;
    private AttributeCodesetMapping codesetMapping;
    private String language;

    public Pathables(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, ItemValidator itemValidator, String language) {
        this.terminologyInterface = terminologyInterface;
        this.itemValidator = itemValidator;
        this.codesetMapping = codesetMapping;
        this.language = language;
    }

    public void traverse(Pathable pathable, String... excludes) throws IllegalArgumentException, InternalError {

        for (Field field: pathable.getClass().getDeclaredFields()){
            try {
                if (field.getType() == field.getType().asSubclass(Pathable.class)) {

                    if (isFieldExcluded(excludes, field.getName()))
                        continue;

                    Object object = objectForField(pathable, field);

                    if (object != null && object instanceof Pathable) {
                        new Pathables(terminologyInterface, codesetMapping, itemValidator, language).traverse((Pathable) object, excludes);
                    }
                    else
                        if (object != null)
                            throw new IllegalArgumentException("Internal: couldn't handle object retrieved using getter");
                }
            }
            catch (ClassCastException e){
                //check if object is handled for validation
                if (itemValidator.isValidatedRmObjectType(field.getType())){
                    RMObject object = objectForField(pathable, field);
                    itemValidator.validate(terminologyInterface, codesetMapping, field.getName(), object, language);
                }
            } //continue
        }

    }

    private boolean isFieldExcluded(String[] excludes, String fieldName){
        for (String exclude: excludes){
            if (exclude.equals(fieldName))
                return true;
        }

        return false;
    }

    private RMObject objectForField(Pathable pathable, Field field) throws IllegalArgumentException, InternalError {
        String getterName = "get"+ StringUtils.capitalize(field.getName());
        MethodHandle methodHandle;
        try {
            methodHandle = MethodHandles.lookup().findVirtual(pathable.getClass(), getterName, MethodType.methodType(field.getType()));
        }
        catch (NoSuchMethodException | IllegalAccessException e){
            throw new InternalError("Internal error:"+e.getMessage());
        }
        try {
            Object object = methodHandle.invoke(pathable);
            if (object != null && !(object instanceof RMObject))
                throw new IllegalArgumentException("Internal: object is not of class RMObject:" + object.toString());

            return (RMObject) object;
        }
        catch (Throwable throwable){
            throw new InternalError("Internal:"+throwable.getMessage());
        }
    }

}
