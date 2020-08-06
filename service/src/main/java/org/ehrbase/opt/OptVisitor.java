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


import com.nedap.archie.rm.generic.Participation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.SchemaType;
import org.ehrbase.opt.mapper.Boolean;
import org.ehrbase.opt.mapper.Interval;
import org.ehrbase.opt.mapper.*;
import org.ehrbase.validation.constraints.wrappers.I_CArchetypeConstraintValidate;
import org.openehr.schemas.v1.*;
import org.openehr.schemas.v1.impl.CARCHETYPEROOTImpl;
import org.openehr.schemas.v1.impl.CDEFINEDOBJECTImpl;

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
        return null;

    }


    private Map traverseDomainTypeObject(CDOMAINTYPE cpo, Map<String, TermDefinition> termDef, String path) throws Exception {

        String name = new LocatablePath(path).attribute();
        SchemaType type = I_CArchetypeConstraintValidate.findSchemaType(I_CArchetypeConstraintValidate.getXmlType(cpo));

        switch(type.getName().getLocalPart()) {
            case "C_DV_QUANTITY":
                return new Quantity((CDVQUANTITY) cpo, termDef).toMap(name);

            case "C_CODE_PHRASE":
                return new CodePhrase((CCODEPHRASE) cpo, termDef).toMap(name);

            case "C_DV_ORDINAL":
                return new Ordinal((CDVORDINAL) cpo, termDef).toMap(name);

            default:
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

        Map map = handleArchetypeRoot(def, null, "");

        rootMap.put(Constants.TREE, map);
//		constrainMapper.setTerminology(termTable);
        return rootMap;
    }


    /**
     * @param def
     * @param name
     * @param path
     * @return
     * @throws Exception
     */
    private Map handleArchetypeRoot(CARCHETYPEROOT def, String name, String path) throws Exception {
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
        return handleComplexObject(def, termDef, name, path);
    }

    /**
     * Load complex component
     *
     * @param ccobj
     * @param termDef
     * @param name
     * @return
     * @throws Exception
     */
    private Map handleComplexObject(CCOMPLEXOBJECT ccobj, Map<String, TermDefinition> termDef, String name, String path) throws Exception {

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

                //issue with getting the actual type
                SchemaType type = I_CArchetypeConstraintValidate.findSchemaType(I_CArchetypeConstraintValidate.getXmlType(attr));
//                attr = (CATTRIBUTE) attr.changeType(type);
                //-----

                if (type.getName().getLocalPart().equals("C_SINGLE_ATTRIBUTE")) {
                    if (children != null && children.length > 0) {
                        try {
                            for (COBJECT cobj : children) {
                                Map cobjectMap = handleCObject(cobj, termDef, attrName, pathloop);
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
                } else if (type.getName().getLocalPart().equals("C_MULTIPLE_ATTRIBUTE")) {

                    for (COBJECT cobj : children) {
                        try {

                            Object attrValue = handleCObject(cobj, termDef, attrName, pathloop);
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
        } else if ("PARTICIPATION".equals(rmTypeName)){
            //TODO: handle PARTICIPATION constraint
            nodeMap = null;
        } else if ("DV_IDENTIFIER".equals(rmTypeName)) {
            nodeMap = new ValueType(ccobj, termDef).toMap(rmTypeName, name);
        } else if ("DV_PROPORTION".equals(rmTypeName)) {
            nodeMap = new Proportion(ccobj, termDef).toMap(name);
        } else if ("ISM_TRANSITION".equals(rmTypeName)) {
            nodeMap = new IsmTransition(ccobj, termDef).toMap(name);
        } else if (rmTypeName.matches("COMPOSITION|SECTION|CLUSTER|ITEM_TREE|ACTION|INSTRUCTION|ACTIVITY|EVALUATION|OBSERVATION|SECTION|EVENT|HISTORY|EVENT_CONTEXT|ADMIN_ENTRY|POINT_EVENT|INTERVAL_EVENT")) {
            Structural structural = new Structural(rmTypeName, archetypeNodeId, path, nodeId, ccobj, termDef, childrenNodeMap);
            nodeMap = structural.toMap();
            if (nodeMap != null && "COMPOSITION".matches(rmTypeName)) {
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

    private Map handleCObject(COBJECT cobj, Map<String, TermDefinition> termDef, String attrName, String path) throws Exception {
        // if ( cobj.getOccurrences().isAvailable() ) {
//        log.debug("cobj=" + cobj.getClass() + ":" + cobj.getRmTypeName());

        String rmTypeName = cobj.getRmTypeName();
        SchemaType type = I_CArchetypeConstraintValidate.findSchemaType(I_CArchetypeConstraintValidate.getXmlType(cobj));

        switch (type.getName().getLocalPart()) {
            case "C_ARCHETYPE_ROOT":
               //this is to avoid class cast exception under springboot...
                try {
                    cobj = (CARCHETYPEROOT) cobj.changeType(type);
                   CARCHETYPEROOT carchetyperoot = (CARCHETYPEROOT)cobj;
                   if (!(carchetyperoot.getArchetypeId().getValue().isEmpty())) {
                        path = path + "[" + carchetyperoot.getArchetypeId().getValue() + "]";
                    }
                    log.debug("CARCHETYPEROOT path=" + path);
                    return handleArchetypeRoot(carchetyperoot, attrName, path);
                } catch(ClassCastException e){
                    //ignore for the time being...
                    log.warn("class cast failed for class:"+cobj.getClass().getSimpleName());
                    return null;
                }

            case "C_DOMAIN_TYPE":
                return traverseDomainTypeObject((CDOMAINTYPE) cobj, termDef, path);
            case "C_COMPLEX_OBJECT":
                // Skip when path is /category and /context
                if ("/category".equalsIgnoreCase(path)) {
                    return null;
                } else if ("/context".equalsIgnoreCase(path)) {
                    return handleComplexObject((CCOMPLEXOBJECT) cobj, termDef, attrName, path);
                }
                if (!((CCOMPLEXOBJECT) cobj).getNodeId().isEmpty()) {
                    path = path + "[" + ((CCOMPLEXOBJECT) cobj).getNodeId() + "]";
                }
                log.debug("CONTEXT path=" + path);
                return handleComplexObject((CCOMPLEXOBJECT) cobj, termDef, attrName, path);

            case "ARCHETYPE_SLOT":
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

            case "C_PRIMITIVE_OBJECT":
                return traversePrimitiveTypeObject((CPRIMITIVEOBJECT) cobj);

            default:
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
