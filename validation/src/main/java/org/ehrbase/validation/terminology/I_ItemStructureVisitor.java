package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemStructure;

public interface I_ItemStructureVisitor {

    void validate(Composition composition) throws Throwable;

    void validate(ItemStructure itemStructure) throws Throwable;

    void validate(Locatable locatable) throws Throwable;
}
