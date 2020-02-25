package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Pathable;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.validation.terminology.validator.ItemField;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.List;

public class Pathables {

    private ItemValidator itemValidator;
    private TerminologyInterface terminologyInterface;
    private AttributeCodesetMapping codesetMapping;
    private String language;

    Pathables(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, ItemValidator itemValidator, String language) {
        this.terminologyInterface = terminologyInterface;
        this.itemValidator = itemValidator;
        this.codesetMapping = codesetMapping;
        this.language = language;
    }

    public void traverse(Pathable pathable, String... excludes) throws IllegalArgumentException, InternalError {

        for (Field field: pathable.getClass().getDeclaredFields()){
            if (!field.getType().equals(List.class)) {
                try {
                    if (field.getType() == field.getType().asSubclass(Pathable.class)) {

                        if (isFieldExcluded(excludes, field.getName()))
                            continue;

                        RMObject object = new ItemField<RMObject>(pathable).objectForField(field);

                        if (object instanceof Pathable) {
                            new Pathables(terminologyInterface, codesetMapping, itemValidator, language).traverse((Pathable) object, excludes);
                        } else if (object != null)
                            throw new IllegalArgumentException("Internal: couldn't handle object retrieved using getter");
                    }
                } catch (ClassCastException e) {
                    //check if object is handled for validation
                    if (itemValidator.isValidatedRmObjectType(field.getType())) {
                        RMObject object =new ItemField<RMObject>(pathable).objectForField(field);
                        itemValidator.validate(terminologyInterface, codesetMapping, field.getName(), object, language);
                    }
                }
            }//continue
            else { //iterate the array
                List iterable = new ItemField<List>(pathable).objectForField(field);

                for (Object item: iterable){
                    if (item instanceof RMObject){
                        itemValidator.validate(terminologyInterface, codesetMapping, field.getName(), (RMObject)item, language);
                    }
                    else if (item instanceof Pathable){
                        traverse((Pathable)item, excludes);
                    }
                    else
                        throw new IllegalStateException("Could not handle item in list:"+item);
                }
            }
        }
    }

    private boolean isFieldExcluded(String[] excludes, String fieldName){
        for (String exclude: excludes){
            if (exclude.equals(fieldName))
                return true;
        }

        return false;
    }
}
