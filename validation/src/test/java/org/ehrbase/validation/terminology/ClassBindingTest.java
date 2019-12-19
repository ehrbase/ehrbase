package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.datatypes.CodePhrase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassBindingTest {

    @Test
    public void bindingTest(){
        assertEquals(CodePhrase.class, new  org.ehrbase.validation.terminology.validator.CodePhrase().rmClass());
    }
}
