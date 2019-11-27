package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.junit.Test;

import static org.junit.Assert.*;

public class ItemValidatorTest {

    @Test
    public void matchValidator() throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {

        ItemValidator itemValidator = new ItemValidator();

        itemValidator
                .add(DvCodedText.class);

        DvCodedText dvCodedText = new DvCodedText("zz", new CodePhrase(new TerminologyId("openehr"), "234"));

        assertTrue(itemValidator.isValidatedRmObjectType(dvCodedText));

        try {
            itemValidator.validate(dvCodedText, "test", dvCodedText);
        } catch (Throwable throwable){
            fail();
        }

    }
}