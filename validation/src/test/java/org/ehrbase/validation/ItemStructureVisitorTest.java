package org.ehrbase.validation;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemTree;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.xml.JAXBUtil;
import org.apache.commons.io.IOUtils;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.test_data.item_structure.ItemStruktureTestDataCanonicalJson;
import org.ehrbase.validation.terminology.ItemStructureVisitor;
import org.junit.Test;

import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;

public class ItemStructureVisitorTest {

    @Test
    public void elementVisitorTest() throws Throwable {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/test_all_types.fixed.v1.xml")));

        ItemStructureVisitor itemStructureVisitor = new ItemStructureVisitor();
        itemStructureVisitor.validate(composition);
        assertEquals(26, itemStructureVisitor.getElementOccurrences()); //25 ELEMENTs + 1 ITEM_SINGLE!

    }

    @Test
    public void elementVisitorTest2() throws Throwable {
        Unmarshaller unmarshaller = JAXBUtil.createRMContext().createUnmarshaller();
        Composition composition = (Composition) unmarshaller.unmarshal(new FileInputStream(new File("./src/test/resources/composition/RIPPLE-ConformanceTest.xml")));

        ItemStructureVisitor itemStructureVisitor = new ItemStructureVisitor();
        itemStructureVisitor.validate(composition);
        assertEquals(57, itemStructureVisitor.getElementOccurrences()); //4 elements are in the context/other_context structure

        itemStructureVisitor.validate(composition.getContext().getOtherContext());
        assertEquals(61, itemStructureVisitor.getElementOccurrences());

    }

    @Test
    public void ehrVisitorTest() throws Throwable {
        String value = IOUtils.toString(ItemStruktureTestDataCanonicalJson.SIMPLE_EHR_OTHER_Details.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        ItemTree otherDetails = cut.unmarshal(value, ItemTree.class);

        EhrStatus ehrStatus = new EhrStatus("ehr_status", new DvText("ehr_status"), new PartySelf(new PartyRef()), true, true, otherDetails);

        ItemStructureVisitor itemStructureVisitor = new ItemStructureVisitor();
        itemStructureVisitor.validate(ehrStatus);
        assertEquals(3, itemStructureVisitor.getElementOccurrences());

    }

}