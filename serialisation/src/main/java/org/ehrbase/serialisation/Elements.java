package org.ehrbase.serialisation;

import com.nedap.archie.rm.datastructures.Element;

public class Elements {

    private Element element;

    public Elements(Element element) {
        this.element = element;
    }

    /**
     * check if an element is containing any significant values to be serialized (e.g. stored) to DB
     * @return
     */
    public boolean isVoid(){
        return (element.getValue() == null &&
                element.getNullFlavour() == null);
    }
}
