package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Pathable;
import org.apache.commons.lang3.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class Pathables {

    private ItemValidator itemValidator;

    public Pathables(ItemValidator itemValidator) {
        this.itemValidator = itemValidator;
    }

    public void traverse(Pathable pathable, String... excludes) throws Throwable {

        for (Field field: pathable.getClass().getDeclaredFields()){
            try {
                if (field.getType() == field.getType().asSubclass(Pathable.class)) {

                    if (isFieldExcluded(excludes, field.getName()))
                        continue;

                    Object object = objectForField(pathable, field);

                    if (object != null && object instanceof Pathable) {
                        new Pathables(itemValidator).traverse((Pathable) object, excludes);
                    }
                    else
                        if (object != null)
                            throw new IllegalArgumentException("Internal: couldn't handle object retrieved using getter");
                }
            }
            catch (Exception e){
                //check if object is handled for validation
                if (itemValidator.isValidatedRmObjectType(field.getType())){
                    try {
                        RMObject object = objectForField(pathable, field);
                        itemValidator.validate(pathable, field.getName(), object);
                    }
                    catch (Throwable throwable){
                        throw new IllegalArgumentException("Could not resolve object for field:"+field.toGenericString()+", exception:"+e.getMessage());
                    }
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

    private RMObject objectForField(Pathable pathable, Field field) throws Throwable {
        String getterName = "get"+ StringUtils.capitalize(field.getName());
        MethodHandle methodHandle = MethodHandles.lookup().findVirtual(pathable.getClass(), getterName, MethodType.methodType(field.getType()));
        Object object = methodHandle.invoke(pathable);
        if (object != null && !(object instanceof RMObject))
            throw new IllegalArgumentException("Internal: object is not of class RMObject:"+object.toString());

        return (RMObject)object;
    }

}
