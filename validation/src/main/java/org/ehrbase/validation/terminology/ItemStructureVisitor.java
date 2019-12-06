package org.ehrbase.validation.terminology;


import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.*;

import com.nedap.archie.rm.datastructures.*;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.TermMapping;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.datavalues.quantity.DvOrdered;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.demographic.PartyRelationship;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.Participation;
import com.nedap.archie.rm.integration.GenericEntry;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.ehrbase.terminology.openehr.TerminologyService;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.terminology.openehr.implementation.LocalizedTerminologies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Visitor of an item structure to perform specific validations:
 * - CodePhrase depending on terminology
 * - units
 */
public class ItemStructureVisitor implements I_ItemStructureVisitor {

    protected static Logger log = LoggerFactory.getLogger(ItemStructureVisitor.class);
    private int elementOccurrences = 0; //for statistics and testing
    private ItemValidator itemValidator = new ItemValidator();
    private LocalizedTerminologies localizedTerminologies;
    private AttributeCodesetMapping codesetMapping;
    private String itemStructureLanguage = "en"; //if a composition, the language can be found in the structure

    public ItemStructureVisitor(LocalizedTerminologies localizedTerminologies) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        this.localizedTerminologies = localizedTerminologies;
        this.codesetMapping = localizedTerminologies.codesetMapping();

        itemValidator
                .add(Composition.class)
                .add(DvCodedText.class)
                .add(DvText.class)
                .add(IsmTransition.class)
                .add(DvOrdered.class)
                .add(EventContext.class)
                .add(IntervalEvent.class)
                .add(IsmTransition.class)
                .add(OriginalVersion.class)
                .add(Participation.class)
                .add(PartyRelationship.class)
                .add(TermMapping.class)
                .add(DvMultimedia.class)
                .add(DvOrdinal.class);

    }

    public ItemStructureVisitor(TerminologyService terminologyService) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        this(terminologyService.localizedTerminologies());
    }

    /**
     * main entry method, validate a composition.
     * @param composition
     * @return
     * @throws Exception
     */
    @Override
    public void validate(Composition composition) throws Exception {
        if (composition == null || composition.getContent() == null || composition.getContent().isEmpty())
            return;

        itemStructureLanguage = composition.getLanguage().getCodeString();

        itemValidator.validate(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, "composition", composition, itemStructureLanguage);

        new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(composition, "content");

        for (ContentItem item : composition.getContent()) {
            traverse(item);
        }
    }

    @Override
    public void validate(ItemStructure itemStructure) throws Exception {
            traverse(itemStructure);
    }

    @Override
    public void validate(Locatable locatable) throws Exception {

        if (locatable instanceof Item)
            traverse((Item) locatable);
        else if (locatable instanceof ItemStructure)
            traverse((ItemStructure) locatable);
        else if (locatable instanceof EhrStatus && ((EhrStatus)locatable).getOtherDetails() != null)
            traverse(((EhrStatus)locatable).getOtherDetails());
        else
            throw new IllegalArgumentException("pathable is not an Item or ItemStructure instance...");

    }

    /**
     * main entry method, validate an arbitrary entry (evaluation, observation, instruction, action)
     * @param entry
     * @return
     * @throws Exception
     */
    private void validate(Entry entry) throws Exception {
        traverse(entry);
    }

    /**
     * convenience method for processing an Evaluation
     * @param entry
     * @return
     * @throws Exception
     */
    private void validate(Evaluation entry) throws Exception {
        if (entry == null || entry.getData() == null)
            return;

        traverse(entry);
    }

    /**
     * convenience method for processing an Observation
     * @param entry
     * @return
     * @throws Exception
     */
    private void validate(Observation entry) throws Exception {
        if (entry == null || entry.getData() == null)
            return;

        traverse(entry);
    }

    /**
     * convenience method for processing an Instruction
     * @param entry
     * @return
     * @throws Exception
     */
    private void validate(Instruction entry) throws Exception {
        if (entry == null || entry.getActivities() == null)
            return;

        traverse(entry);
    }

    /**
     * convenience method for processing an Instruction
     * @param entry
     * @return
     * @throws Exception
     */
    private void validate(Action entry) throws Exception {
        if (entry == null || entry.getDescription() == null)
            return;

        traverse(entry);
    }

    /**
     * convenience method for processing an Activity
     * @param entry
     * @return
     * @throws Exception
     */
    private void validate(Activity entry) throws Exception {
        if (entry == null || entry.getDescription() == null)
            return;

        traverse(entry);
    }

//	public void setMode(WalkerOutputMode mode) {
//		this.tag_mode = mode;
//	}


    /**
     * domain level: Observation, evaluation, instruction, action. section, admin etc.
     * @param item
     * @throws Exception
     */
    private void traverse(ContentItem item) throws Exception {

        Map<String, Object> retmap = null;

        if (item == null){
            return;
        }

        log.debug("traverse element of class:"+item.getClass()+", nodeid:"+item.getArchetypeNodeId());

        if (item instanceof Observation) {
            Observation observation = (Observation) item;

            new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(observation, "protocol", "data", "state");

            if (observation.getProtocol() != null)
                traverse(observation.getProtocol());

            if (observation.getData() != null)
                traverse(observation.getData());

            if (observation.getState() != null)
                traverse(observation.getState());

        } else if (item instanceof Evaluation) {
            Evaluation evaluation = (Evaluation) item;

            new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(evaluation, "protocol", "data");

            if (evaluation.getProtocol() != null)
                traverse(evaluation.getProtocol());

            if (evaluation.getData() != null)
                traverse(evaluation.getData());

        } else if (item instanceof Instruction) {
            Instruction instruction = (Instruction) item;

            new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(instruction, "protocol", "activities");

            if (instruction.getProtocol() != null)
                traverse(instruction.getProtocol());

            if (instruction.getActivities() != null) {
                for (Activity activity : instruction.getActivities()) {
                    traverse(activity);
                }
            }

        } else if (item instanceof Action) {
            Action action = (Action) item;

            new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(action, "protocol", "description");

            if (action.getProtocol() != null)
                traverse(action.getProtocol());

            if (action.getDescription() != null)
                traverse(action.getDescription());

        } else if (item instanceof Section) {

            for (ContentItem contentItem : ((Section) item).getItems()) {
                traverse(contentItem);
            }

        } else if (item instanceof AdminEntry) {
            AdminEntry adminEntry = (AdminEntry) item;

            new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(adminEntry, "data");

            if (adminEntry.getData() != null)
                traverse(adminEntry.getData());

        } else if (item instanceof GenericEntry) {
            GenericEntry genericEntry = (GenericEntry)item;

            new Pathables(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, itemValidator, itemStructureLanguage).traverse(genericEntry, "data");

            traverse(genericEntry.getData());

        } else {
            log.warn("This item is not handled!"+item.getName());
        }
    }

    private void traverse(Activity activity) throws Exception {
        if (activity == null)
            return;

        log.debug("traverse activity:"+activity);

        traverse(activity.getDescription()); //don't add a /data in path for description (don't ask me why...)

    }


    /**
     * History level in composition
     * @param item
     * @throws Exception
     */
    private void traverse(History<?> item) throws Exception {
        if (item == null){
            return;
        }

        log.debug("traverse history:"+item);

        //CHC: 160531 add explicit name
        History history = (History)item;

        if (item.getSummary() != null)
            traverse(item.getSummary());

        if (item.getEvents() != null) {

            for (Event<?> event : item.getEvents()) {

                itemValidator.validate(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, "event", event, itemStructureLanguage);

                if (event.getData() != null)
                    traverse(event.getData());
                if (event.getState() != null)
                    traverse(event.getState());

            }
        }
    }

    /**
     * ItemStructure: single, tree or table
     * @param item
     * @throws Exception
     */
    private void traverse(ItemStructure item) throws Exception {

        log.debug("traverse itemstructure:"+item);

        if (item == null){
            return;
        }


        if (item instanceof ItemSingle) {
            ItemSingle itemSingle = (ItemSingle)item;
            if (itemSingle.getItem()!=null){
                traverse(itemSingle.getItem());
            }
        } else if (item instanceof ItemList) {
            ItemList list = (ItemList) item;
            if (list.getItems() != null) {

                for (Element element : list.getItems()) {
                    traverse(element);
                }
            }
        } else if (item instanceof ItemTree) {
            ItemTree tree = (ItemTree) item;

            if (tree.getItems() != null) {

                for (Item subItem : tree.getItems()) {
                    traverse(subItem);
                }
            }

        } else if (item instanceof ItemTable) {
            ItemTable table = (ItemTable) item;
            if (table.getRows() != null) {

                for (Item subItem : table.getRows()) {
                    traverse(subItem);
                }
            }
        }
    }

    protected void validateElement(Element element) throws Exception {
        log.debug("should validate this element:"+element);
        elementOccurrences += 1;

        if (element.getNullFlavour() != null && itemValidator.isValidatedRmObjectType(element.getNullFlavour())){
            itemValidator.validate(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, null, element.getNullFlavour(), itemStructureLanguage);
        }

        if (element.getValue() != null && itemValidator.isValidatedRmObjectType(element.getValue())){
            itemValidator.validate(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, null, element.getValue(), itemStructureLanguage);
        }

        if (element.getName() != null && element.getName() instanceof DvCodedText){
            itemValidator.validate(localizedTerminologies.locale(itemStructureLanguage), codesetMapping, null, (DvCodedText)element.getName(), itemStructureLanguage);
        }
    }

    /**
     * Element level, normally cannot go deeper...
     * @param item
     * @throws Exception
     */
    private void traverse(Item item) throws Exception {
        log.debug("traverse item:"+item);

        if (item == null){
            return;
        }

        if (item instanceof Element) {
            validateElement((Element) item);
        }

        else if (item instanceof Cluster) {

            Cluster c = (Cluster) item;

            if (c.getItems() != null) {

                for (Object clusterItem : c.getItems()) {
                    if (clusterItem instanceof Item)
                        traverse((Item)clusterItem);
                    else 
                        throw new IllegalArgumentException("Cannot handle cluster item:"+clusterItem);
                }
            }
        }
    }

    public int getElementOccurrences() {
        return elementOccurrences;
    }
}
