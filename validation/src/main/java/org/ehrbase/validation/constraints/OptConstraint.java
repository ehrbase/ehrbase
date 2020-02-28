/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.validation.constraints;

import org.ehrbase.validation.constraints.util.LocatableHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openehr.schemas.v1.*;

import java.util.*;

public class OptConstraint {

    private Logger log = LogManager.getLogger(OptConstraint.class);

    private OptConstraintMapper constrainMapper = new OptConstraintMapper();

    private Map<String, Map<String, String>> termTable = new HashMap<>();

    private Map<String, List<DVORDINAL>> ordinalTable = new HashMap<>();

    //field identifiers
    protected static final String VALUE = "value";
    protected static final String ANY = "$any$";

    /**
     * Generate empty Rm from template
     *
     * @param opt Operational template
     * @return a constraint mapper corresponding to the template
     * @throws IllegalArgumentException
     */
    public OptConstraintMapper map(OPERATIONALTEMPLATE opt) throws IllegalArgumentException {
        CARCHETYPEROOT def = opt.getDefinition();

        handleArchetypeRoot(opt, def, null, "");
        constrainMapper.setTerminology(termTable);
        return constrainMapper;
    }

    /**
     * @param opt
     * @param def
     * @param name
     * @param path
     * @return
     * @throws IllegalArgumentException
     */
    private void handleArchetypeRoot(OPERATIONALTEMPLATE opt,CARCHETYPEROOT def, String name, String path) throws IllegalArgumentException {

        Map<String, String> termDef = new HashMap<>();
        // Keep term definition to map
        for (ARCHETYPETERM term : def.getTermDefinitionsArray()) {
            String code = term.getCode();
            for (StringDictionaryItem item : term.getItemsArray()) {
                if ("text".equals(item.getId())) {
                    // TODO currently keep only text , let's check that should
                    // we keep description?
                    termDef.put(code, item.getStringValue());
                }
            }
        }
        log.debug("CARCHETYPEROOT path=" + path);
        termTable.put(path, termDef);
        // Load complex component
        handleComplexObject(opt, def, termDef, name, path);
    }

    /**
     * Load complex component
     *
     * @param opt
     * @param ccobj
     * @param termDef
     * @param name
     * @return
     * @throws IllegalArgumentException
     */
    private void handleComplexObject(OPERATIONALTEMPLATE opt,
                                       CCOMPLEXOBJECT ccobj, Map<String, String> termDef, String name,
                                       String path) throws IllegalArgumentException {

        String nodeId = ccobj.getNodeId();
        String rmTypeName = ccobj.getRmTypeName();
        log.debug("rmTypeName=" + rmTypeName + ":nodeId=" + nodeId + ":ccobj="+ ccobj);

        constrainMapper.addToValidPath(path);
        constrainMapper.addToExistence(path, ccobj.getOccurrences());

        // Loop Create attributes
        CATTRIBUTE[] cattributes = ccobj.getAttributesArray();
        if (cattributes != null && cattributes.length > 0) {
            for (CATTRIBUTE attr : cattributes) {
                String pathloop = path + "/" + attr.getRmAttributeName();
                COBJECT[] children = attr.getChildrenArray();
                String attrName = attr.getRmAttributeName();
                if (attr instanceof CSINGLEATTRIBUTE) {
                    if (children != null && children.length > 0) {
                        try {
                            COBJECT cobj = children[0];
                            if (children.length > 1) {
                                log.debug("Multiple children in CATTRIBUTE:" + children.length);
                            }
                            handleCObject(opt, cobj, termDef, attrName, pathloop);
                            log.debug("attrName=" + attrName );
                        } catch (Exception e) {
                            log.error("Cannot create attribute name " + attrName + " on path " + pathloop, e);

                        }
                    }
                } else if (attr instanceof CMULTIPLEATTRIBUTE) {
                    CMULTIPLEATTRIBUTE cma = (CMULTIPLEATTRIBUTE) attr;

                    for (COBJECT cobj : children) {
                        try {

                            handleCObject(opt, cobj, termDef, attrName, pathloop);
                            log.debug("attrName=" + attrName );

                        } catch (Exception e) {
                            log.error("Cannot create attribute name " + attrName + " on path " + pathloop, e);
                        }
                    }
                    constrainMapper.addToCardinalityList(path + "/" + attr.getRmAttributeName(), cma);
                }
            }
        }

        if ("HISTORY".equals(rmTypeName)) {
            //test only for now
            constrainMapper.bind(path, ccobj);

        }  else if ("ELEMENT".equals(rmTypeName)) {

            if (ccobj.getAttributesArray().length > 0) {
                Object obj = ccobj.getAttributesArray()[0];
                if (ccobj.getAttributesArray().length > 1) {
                    log.debug("Multiple CCOBJ ELEMENT:" + ccobj.getAttributesArray().length);
                }
                if (obj instanceof CSINGLEATTRIBUTE) {
                    CSINGLEATTRIBUTE attr = (CSINGLEATTRIBUTE) obj;

                    if (VALUE.equals(attr.getRmAttributeName())) {
                        if (attr.getChildrenArray().length > 0) {
                            List<String> children = new ArrayList<>();
                            for (COBJECT cobj : attr.getChildrenArray()) {
                                children.add(cobj.getRmTypeName());
                            }
                            log.debug("ELEMENT children length:" + attr.getChildrenArray().length);
                            if (attr.getChildrenArray().length > 1) {
                                log.debug("CHOICE:" + attr.getChildrenArray().length);
                            }
                            COBJECT cobj = attr.getChildrenArray()[0]; //TODO: check if iteration is needed
                            handleCObject(opt, cobj, termDef, name, path);
                        } else
                            log.debug("ELEMENT without child, assuming ANY type");
                    } else {
                        log.debug("additional attribute found for element attr.getRmAttributeName()=" + attr.getRmAttributeName());
                    }
                    constrainMapper.addToWatchList(path, attr);
                } else {
                    log.debug("Other type for obj:" + obj);
                }
            }
            else {
                log.debug("Empty attribute list for ELEMENT, assuming ANY type:" + ccobj.toString());
            }
            constrainMapper.bind(LocatableHelper.simplifyPath(path), ccobj);
        }
    }

    private void handleCObject(OPERATIONALTEMPLATE opt, COBJECT cobj, Map<String, String> termDef, String attrName, String path) throws IllegalArgumentException {
        // if ( cobj.getOccurrences().isAvailable() ) {
        log.debug("cobj=" + cobj.getClass() + ":" + cobj.getRmTypeName());

        if (cobj instanceof CARCHETYPEROOT) {
            if (!((CARCHETYPEROOT) cobj).getArchetypeId().getValue().isEmpty()) {
                path = path + "[" + ((CARCHETYPEROOT) cobj).getArchetypeId().getValue() + "]";
            }
            log.debug("CARCHETYPEROOT path=" + path);
            handleArchetypeRoot(opt, (CARCHETYPEROOT) cobj, attrName, path);
        } else if (cobj instanceof CDOMAINTYPE) {
            handleDomainTypeObject((CDOMAINTYPE) cobj, termDef, path);
        } else if (cobj instanceof CCOMPLEXOBJECT) {
            // Skip when path is /category and /context
            if ("/category".equalsIgnoreCase(path)) {
                //do nothing
            } else if ("/context".equalsIgnoreCase(path)) {
                handleComplexObject(opt, (CCOMPLEXOBJECT) cobj, termDef, attrName, path);
            }
            if (cobj.getNodeId() != null && !cobj.getNodeId().isEmpty()) {
                path = path + "[" + cobj.getNodeId() + "]";
            }
            log.debug("CONTEXT path=" + path);
            handleComplexObject(opt, (CCOMPLEXOBJECT) cobj, termDef, attrName, path);
        } else if (cobj instanceof ARCHETYPESLOT) {
            if (!cobj.getNodeId().isEmpty()) {
                path = path + "[" + cobj.getNodeId() + "]";
            }


            // slot.getIncludes().get(0).
            log.debug("ARCHETYPESLOT path=" + path);
            // return handleComplexObject(opt, (CCOMPLEXOBJECT) cobj, termDef,
            // attrName, path);
        } else if (cobj instanceof CPRIMITIVEOBJECT) {
            //do nothing
        } else {
            if (cobj.getNodeId() == null) {
                log.debug("NodeId is null : " + cobj);
            }
            log.debug("Some value cannot process because is not CARCHETYPEROOT or CCOMPLEXOBJECT : "+ cobj);
        }

    }

    public ConstraintMapper getConstraintMapper() {
        return constrainMapper;
    }

    private DVORDINAL getOrdinalTermDef(String path, int index) {
        for (String keyTerm : ordinalTable.keySet()) {
            if (keyTerm != null && !keyTerm.isEmpty() && path.startsWith(keyTerm)) {
                List<DVORDINAL> ordinalList = ordinalTable.get(keyTerm);
                for (DVORDINAL ord : ordinalList) {
                    if (ord != null && index == ord.getValue()) {
                        //return getTermDef(path,ord.getSymbol().getDefiningCode().getCodeString());
                        return ord;
                    }
                }
            }
        }
        return null;
    }

    private void handleDvOrdinal(CDVORDINAL cdo, Map<String, String> termDef, String path) throws IllegalArgumentException {

        List<DVORDINAL> list = Arrays.asList(cdo.getListArray());
        if (list.size() == 0) {
            throw new IllegalArgumentException("empty list of ordinal");
        }
        ordinalTable.put(path, list);
    }

    private void handleDomainTypeObject(CDOMAINTYPE cpo, Map<String, String> termDef, String path) throws IllegalArgumentException {

        if (cpo instanceof CDVORDINAL) {
            if (((CDVORDINAL) cpo).isSetAssumedValue()) {
                //do nothing
            }
            else
                handleDvOrdinal((CDVORDINAL) cpo, termDef, path);

        }
    }
}
