/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
 * This file is part of Project Ethercis
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
package org.ehrbase.opt;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.opt.mapper.Boolean;
import org.ehrbase.opt.mapper.Interval;
import org.ehrbase.opt.mapper.*;
import org.openehr.schemas.v1.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import OptConstraintMapper;

public class OptVisitor {

    Logger log = LogManager.getLogger(OptVisitor.class);

    private Map<String, Map<String, TermDefinition>> termTable = new HashMap<>();
    //map path with children
    private Map<String, List<Map<String, Object>>> childrenNodeMap = new HashMap<>();

    public OptVisitor() {

    }


    private Map traversePrimitiveTypeObject(CPRIMITIVEOBJECT cpo) {

        CPRIMITIVE cp = cpo.getItem();

//        if (cp instanceof CBOOLEAN) {
//            if (((CBOOLEAN) cp).isSetAssumedValue())
//                return ((CBOOLEAN) cp).getAssumedValue();
//
//            if (((CBOOLEAN) cp).getTrueValid()) {
//                return "true";
//            } else {
//                return "false";
//            }
//
//        } else if (cp instanceof CSTRING) {
//            if (((CSTRING) cp).isSetAssumedValue())
//                return ((CSTRING) cp).getAssumedValue();
//
//            if (((CSTRING) cp).isSetPattern())
//                return ((CSTRING) cp).getPattern();
//
//            return createString((CSTRING) cp);
//
//        } else if (cp instanceof CDATE) {
//            if (((CDATE) cp).isSetAssumedValue())
//                return ((CDATE) cp).getAssumedValue();
//            return DEFAULT_DATE;
//
//        } else if (cp instanceof CTIME) {
//            if (((CTIME) cp).isSetAssumedValue())
//                return ((CTIME) cp).getAssumedValue();
//            return DEFAULT_TIME;
//
//        } else if (cp instanceof CDATETIME) {
//            if (((CDATETIME) cp).isSetAssumedValue())
//                return ((CDATETIME) cp).getAssumedValue();
//            return DEFAULT_DATE_TIME;
//
//        } else if (cp instanceof CINTEGER) {
//            if (((CINTEGER) cp).isSetAssumedValue())
//                return ((CINTEGER) cp).getAssumedValue();
//            return new Integer(0);
//
//        } else if (cp instanceof CREAL) {
//            if (((CREAL) cp).isSetAssumedValue())
//                return new Double(((CREAL) cp).getAssumedValue());
//
//            return new Double(0);
//
//        } else if (cp instanceof CDURATION) {
//            CDURATION cd = (CDURATION) cp;
//            DvDuration duration = null;
//
//            if (cd.isSetAssumedValue()) {
//                duration = new DvDuration(cd.getAssumedValue());
//            } else if (cd.getRange() != null) {
//                if (cd.getRange().getLower() != null) {
//                    duration = new DvDuration(cd.getRange().getLower());
//                } else if (cd.getRange().getUpper() != null) {
//                    duration = new DvDuration(cd.getRange().getUpper());
//                }
//            }
//            if (duration == null) {
//                return DEFAULT_DURATION;
//            } else {
//                return duration.toString();
//            }
//
//        }
        return null;

    }


    private Map traverseDomainTypeObject(CDOMAINTYPE cpo, Map<String, TermDefinition> termDef, String path) throws Exception {

        String name = new LocatablePath(path).attribute();

        if (cpo instanceof CDVQUANTITY) {
            return new Quantity((CDVQUANTITY) cpo, termDef).toMap(name);

        } else if (cpo instanceof CCODEPHRASE) {
            return new CodePhrase((CCODEPHRASE) cpo, termDef).toMap(name);

        } else if (cpo instanceof CDVORDINAL) {
            return new Ordinal((CDVORDINAL) cpo, termDef).toMap(name);

        } else {
            throw new RuntimeException("unsupported c_domain_type: " + cpo.getClass());
        }
    }

    /**
     * Visit the template and return a map of definitions and attributes
     *
     * @param opt
     * @return
     * @throws Exception
     */
    public Map traverse(OPERATIONALTEMPLATE opt) throws Exception {
        CARCHETYPEROOT def = opt.getDefinition();

        Map<String, Object> rootMap = new HashMap<>();

        rootMap.put(Constants.TEMPLATE_ID, opt.getTemplateId().getValue());
        rootMap.put(Constants.DEFAULT_LANGUAGE, opt.getLanguage().getCodeString());

        List<String> languages = new ArrayList<>();
        for (RESOURCEDESCRIPTIONITEM resourcedescriptionitem : opt.getDescription().getDetailsArray()) {
            if (resourcedescriptionitem.getLanguage() != null) {
                languages.add(resourcedescriptionitem.getLanguage().getCodeString());
            }
        }
        if (languages.size() > 0)
            rootMap.put(Constants.LANGUAGES, languages);

        rootMap.put(Constants.UID, opt.getUid().getValue());
        rootMap.put(Constants.CONCEPT, opt.getConcept());

        Map map = handleArchetypeRoot(opt, def, null, "");

        rootMap.put(Constants.TREE, map);
//		constrainMapper.setTerminology(termTable);
        return rootMap;
    }


    /**
     * @param opt
     * @param def
     * @param name
     * @param path
     * @return
     * @throws Exception
     */
    private Map handleArchetypeRoot(OPERATIONALTEMPLATE opt, CARCHETYPEROOT def, String name, String path) throws Exception {
        Map<String, Object> archetypeRootMap = new HashMap<>();

        Map<String, TermDefinition> termDef = new HashMap<>();
        // Keep term definition to map
        for (ARCHETYPETERM term : def.getTermDefinitionsArray()) {
            String code = term.getCode();
            String value = null, description = null;
            for (StringDictionaryItem item : term.getItemsArray()) {
                if ("text".equals(item.getId())) {
                    value = item.getStringValue();
                }
                if ("description".equals(item.getId()))
                    description = item.getStringValue();
            }
            termDef.put(code, new TermDefinition(code, value, description));

        }


        log.debug("CARCHETYPEROOT path=" + path);
        termTable.put(path, termDef);
        // Load complex component
        return handleComplexObject(opt, def, termDef, name, path);
    }

    /**
     * Load complex component
     *
     * @param opt
     * @param ccobj
     * @param termDef
     * @param name
     * @return
     * @throws Exception
     */
    private Map handleComplexObject(OPERATIONALTEMPLATE opt, CCOMPLEXOBJECT ccobj, Map<String, TermDefinition> termDef, String name, String path) throws Exception {

        Map<String, Object> nodeMap = new HashMap<>();


        String nodeId = ccobj.getNodeId();
        String rmTypeName = ccobj.getRmTypeName();
        log.debug("rmTypeName=" + rmTypeName + ":nodeId=" + nodeId + ":ccobj=" + ccobj);

        String archetypeNodeId = null;
        int min = ccobj.getOccurrences().getLowerUnbounded() ? -1 : ccobj.getOccurrences().getLower();
        int max = ccobj.getOccurrences().getUpperUnbounded() ? -1 : ccobj.getOccurrences().getUpper();

        //node attribute including termDef
        if (nodeId != null && nodeId.trim().length() > 0) {
            if (ccobj instanceof CARCHETYPEROOT) {
                log.debug("set archetype_node_id=" + (((CARCHETYPEROOT) ccobj)).getArchetypeId().getValue());

                archetypeNodeId = (((CARCHETYPEROOT) ccobj)).getArchetypeId().getValue();
            }
        }

        // Loop Create children
        CATTRIBUTE[] cattributes = ccobj.getAttributesArray();
        List<Map<String, Object>> childrenList = new ArrayList<>();
        if (cattributes != null && cattributes.length > 0) {
            for (CATTRIBUTE attr : cattributes) {
                String pathloop = path + "/" + attr.getRmAttributeName();
                COBJECT[] children = attr.getChildrenArray();
                String attrName = attr.getRmAttributeName();
                if (attr instanceof CSINGLEATTRIBUTE) {
                    if (children != null && children.length > 0) {
                        try {
                            for (COBJECT cobj : children) {
                                Map cobjectMap = handleCObject(opt, cobj, termDef, attrName, pathloop);
                                if (cobjectMap != null) {
                                    //although an item tree is an attribute, the node id should be specified (ex. data, protocol etc.)
                                    if (cobj.getRmTypeName().equals(Constants.ITEM_TREE) && !pathloop.endsWith("]")) {
                                        pathloop = pathloop + "[" + cobj.getNodeId() + "]";
                                    }
                                    cobjectMap.put(Constants.AQL_PATH, pathloop);
                                    childrenList.add(cobjectMap);
                                }
//                            log.debug("attrName=" + attrName + ": attrValue=" + attrValue);
                            }
                        } catch (Exception e) {
                            log.error("Cannot create attribute name " + attrName + " on path " + pathloop, e);

                        }
                    }
                } else if (attr instanceof CMULTIPLEATTRIBUTE) {

                    for (COBJECT cobj : children) {
                        try {

                            Object attrValue = handleCObject(opt, cobj, termDef, attrName, pathloop);
//                            log.debug("attrName=" + attrName + ": attrValue=" + attrValue);

                        } catch (Exception e) {
                            log.error("Cannot create attribute name " + attrName + " on path " + pathloop, e);

                        }
                    }
                    log.debug("valueMap.put " + attr.getRmAttributeName());
//					constrainMapper.addToCardinalityList(path+"/"+attr.getRmAttributeName(), cma);
                }
            }
        } else
            log.debug("leaf..");

        if ("DV_TEXT".equals(rmTypeName)) {
            nodeMap = new ValueType(ccobj, termDef).toMap(rmTypeName, name);
        } else if ("DV_CODED_TEXT".equals(rmTypeName)) {
            nodeMap = new CodedText(ccobj, termDef).toMap(name);
        } else if ("DV_URI".equals(rmTypeName) || "DV_EHR_URI".equals(rmTypeName)) {
            nodeMap = new ValueType(ccobj, termDef).toMap(rmTypeName, name);
        } else if (rmTypeName.startsWith("DV_INTERVAL")) {
            nodeMap = new Interval(ccobj, termDef).toMap(name);
        } else if ("DV_DATE_TIME".equals(rmTypeName)) {
            nodeMap = new DateTime(ccobj, termDef).toMap(name);
        } else if ("DV_DATE".equals(rmTypeName)) {
            nodeMap = new Date(ccobj, termDef).toMap(name);
        } else if ("DV_TIME".equals(rmTypeName)) {
            nodeMap = new Time(ccobj, termDef).toMap(name);
        } else if ("DV_PARSABLE".equals(rmTypeName)) {
//            nodeMap = new ValueType(ccobj, termDef).toMap(rmTypeName, name);
//            //get the DvOrdered type defining this interval
//            String dvOrderedTypeName = rmTypeName.substring(rmTypeName.indexOf("<") + 1, rmTypeName.indexOf(">"));
//            Class orderedClass = builder.retrieveRMType(dvOrderedTypeName);
//            nodeMap = new ValueType(ccobj, termDef).toMap("DV_INTERVAL<" + orderedClass.getSimpleName().toUpperCase() + ">", name);
            nodeMap = new Parsable(ccobj, termDef).toMap(rmTypeName);
        } else if ("DV_MULTIMEDIA".equals(rmTypeName)) {
            nodeMap = new ValueType(ccobj, termDef).toMap(rmTypeName, name);
        } else if ("DV_BOOLEAN".equals(rmTypeName)) {
            nodeMap = new Boolean(ccobj, termDef).toMap(name);
        } else if ("DV_COUNT".equals(rmTypeName)) {
            nodeMap = new Count(ccobj, termDef).toMap(name);
        } else if ("DV_DURATION".equals(rmTypeName)) {
            nodeMap = new Duration(ccobj, termDef).toMap(name);
        } else if ("DV_IDENTIFIER".equals(rmTypeName)) {
            nodeMap = new ValueType(ccobj, termDef).toMap(rmTypeName, name);
        } else if ("DV_PROPORTION".equals(rmTypeName)) {
            nodeMap = new Proportion(ccobj, termDef).toMap(name);
        } else if ("ISM_TRANSITION".equals(rmTypeName)) {
            nodeMap = new IsmTransition(ccobj, termDef).toMap(name);
        } else if (rmTypeName.matches("COMPOSITION|SECTION|CLUSTER|ITEM_TREE|ACTION|INSTRUCTION|ACTIVITY|EVALUATION|OBSERVATION|SECTION|EVENT|HISTORY|EVENT_CONTEXT|ADMIN_ENTRY|POINT_EVENT|INTERVAL_EVENT")) {
            Structural structural = new Structural(rmTypeName, archetypeNodeId, path, nodeId, ccobj, termDef, childrenNodeMap);
            nodeMap = structural.toMap();
            if ("COMPOSITION".matches(rmTypeName)) {
                //check if children contains a context, if not add one
                boolean hasContext = false;
                for (Map<String, Object> child : (List<Map<String, Object>>) nodeMap.get(Constants.CHILDREN)) {
                    if (child.containsKey(Constants.CONTEXT)) {
                        hasContext = true;
                    }
                }

                if (!hasContext) {
                    ((List<Map<String, Object>>) nodeMap.get(Constants.CHILDREN)).add(new EventContextAttributes().toMap());
                }
            }
            childrenNodeMap = structural.trim();
        } else if ("ELEMENT".equals(rmTypeName)) {
//            Map inputsMap = null;
//            if (nodeMap != null && nodeMap.containsKey(Constants.CONSTRAINT)){
//                inputsMap = nodeMap;
//            }
            nodeMap = new Element(ccobj, termDef).toMap(nodeId, path, new NodeChildren(childrenNodeMap).include(path));
            if (!childrenList.isEmpty()) {
                //get the first child of this element (only one child allowed for an element)
                nodeMap.put(Constants.CONSTRAINTS, childrenList);
                String valueItemPath = new ValueItem(childrenList).path();

                //add the SQL path
                //TODO: externalize the creation of jsonb select. Make it 'raw' (no transform, cast, function call...)
//                nodeMap.put(Constants.SQL_PATH, new JsonbQuery(path + "/" + valueItemPath).generate());
            }

            if (childrenList.size() > 1 && heterogeneousTypes(childrenList)) {
                //multiple choice, change the type to reflect this
                if (heterogeneousTypes(childrenList))
                    nodeMap.put(Constants.TYPE, Constants.MULTIPLE);
            }
            //cleaning up
            childrenNodeMap = new NodeChildren(childrenNodeMap).exclude(path);
        } else
            throw new IllegalArgumentException("Could not handle type:" + rmTypeName);

        if (nodeMap != null && nodeMap.size() == 0)
            nodeMap.putAll(new NodeMap(termDef, nodeId).toMap(min, max, path, archetypeNodeId, rmTypeName));

        if (nodeMap != null) {
            //add logic to figure out the stack level
            if (childrenNodeMap.containsKey(path))
                childrenNodeMap.get(path).add(nodeMap);
            else {
                List<Map<String, Object>> children = new ArrayList<>();
                children.add(nodeMap);
                childrenNodeMap.put(path, children);
            }
        }

        return nodeMap;
    }

    private Map handleCObject(OPERATIONALTEMPLATE opt, COBJECT cobj, Map<String, TermDefinition> termDef, String attrName, String path) throws Exception {
        // if ( cobj.getOccurrences().isAvailable() ) {
        log.debug("cobj=" + cobj.getClass() + ":" + cobj.getRmTypeName());

        String rmTypeName = cobj.getRmTypeName();

        if (cobj instanceof CARCHETYPEROOT) {
            if (!((CARCHETYPEROOT) cobj).getArchetypeId().getValue().isEmpty()) {
                path = path + "[" + ((CARCHETYPEROOT) cobj).getArchetypeId().getValue() + "]";
            }
            log.debug("CARCHETYPEROOT path=" + path);
            return handleArchetypeRoot(opt, (CARCHETYPEROOT) cobj, attrName, path);
        } else if (cobj instanceof CDOMAINTYPE) {
            return traverseDomainTypeObject((CDOMAINTYPE) cobj, termDef, path);
        } else if (cobj instanceof CCOMPLEXOBJECT) {
            // Skip when path is /category and /context
            if ("/category".equalsIgnoreCase(path)) {
                return null;
            } else if ("/context".equalsIgnoreCase(path)) {
                return handleComplexObject(opt, (CCOMPLEXOBJECT) cobj, termDef, attrName, path);
            }
            if (!((CCOMPLEXOBJECT) cobj).getNodeId().isEmpty()) {
                path = path + "[" + ((CCOMPLEXOBJECT) cobj).getNodeId() + "]";
            }
            log.debug("CONTEXT path=" + path);
            return handleComplexObject(opt, (CCOMPLEXOBJECT) cobj, termDef, attrName, path);
        } else if (cobj instanceof ARCHETYPESLOT) {
            if (!((ARCHETYPESLOT) cobj).getNodeId().isEmpty()) {
                path = path + "[" + ((ARCHETYPESLOT) cobj).getNodeId() + "]";
            }
            ARCHETYPESLOT slot = (ARCHETYPESLOT) cobj;
            // slot.

            // slot.getIncludes().get(0).
            log.debug("ARCHETYPESLOT path=" + path);
            return null;
            // return handleComplexObject(opt, (CCOMPLEXOBJECT) cobj, termDef,
            // attrName, path);
        } else if (cobj instanceof CPRIMITIVEOBJECT) {
            return traversePrimitiveTypeObject((CPRIMITIVEOBJECT) cobj);
        } else {
            if (cobj.getNodeId() == null) {
                log.debug("NodeId is null : " + cobj);
                return null;
            }
            log.debug("Some value cannot process because is not CARCHETYPEROOT or CCOMPLEXOBJECT : " + cobj);

            return null;
        }

    }

    private String getCompName(String nodeId, String name) {
        if (name != null && !name.isEmpty()) {
            if (nodeId != null && !nodeId.isEmpty()) {
                return name + "[" + nodeId + "]";
            } else {
                return name;
            }
        } else {
            return nodeId;
        }
    }

    private List<Map> simpleInputList(String type) {
        List<Map> inputList = new ArrayList<>();

        Map<String, String> inputMap = new HashMap<>();
        inputMap.put("type", type);
        inputList.add(inputMap);
        return inputList;
    }

    private boolean heterogeneousTypes(List<Map<String, Object>> childrenList) {
        //traverse the list of children and return true if the types are different

        String foundType = null;

        for (Map<String, Object> child : childrenList) {

            if (foundType == null)
                foundType = (String) child.get(Constants.TYPE);
            else {
                //compare
                if (!foundType.equals((String) child.get(Constants.TYPE))) {
                    return true;
                }
            }

        }

        return false;

    }

}
