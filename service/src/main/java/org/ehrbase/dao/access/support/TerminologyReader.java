/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
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
package org.ehrbase.dao.access.support;

import java.util.Map;
import javax.xml.XMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class reads an XML file representing a collection of all the
 * terminologies used in the editor and their corresponding values and places
 * them into tables to be used by the editor.
 *
 * @author Mattias Forss
 * @author Christian Chevalley: added parsing of Territory Elements
 */
public class TerminologyReader extends DefaultHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InputStream source;

    private Hashtable<String, String> languageTable;
    private Hashtable<Integer, String> primaryRubricTable;
    private Hashtable<String, Hashtable<Integer, String>> conceptTable;
    private Hashtable<Integer, Integer> grouperTable;
    private Hashtable<Integer, Vector<Integer>> groupedConceptTable;
    private Hashtable<String, String> terminologyIdTable;
    private Hashtable<Integer, Vector<String>> territoryTable;

    public TerminologyReader(InputStream source) {
        this.source = source;
    }

    public void read() {
        initialiseTables();
        final SAXParserFactory factory = SAXParserFactory.newInstance();

        try {
            final SAXParser parser = factory.newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            parser.parse(source, this);
        } catch (Exception e) {
            logger.error("Exception occurred", e);
        }
    }

    private void initialiseTables() {
        languageTable = new Hashtable<>();
        primaryRubricTable = new Hashtable<>();
        conceptTable = new Hashtable<>();
        grouperTable = new Hashtable<>();
        groupedConceptTable = new Hashtable<>();
        terminologyIdTable = new Hashtable<>();
        territoryTable = new Hashtable<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        try {
            switch (qName) {
                case "Language":
                    final String code = attributes.getValue("code");
                    final String description = attributes.getValue("Description");

                    if (code != null && description != null) {
                        languageTable.put(code, description);
                    }
                    break;
                case "PrimaryRubric": {
                    final Integer id = Integer.parseInt(attributes.getValue("Id"));
                    final String language = attributes.getValue("Language");

                    if (id != null && language != null) {
                        primaryRubricTable.put(id, language);
                    }
                    break;
                }
                case "Concept": {
                    final String language = attributes.getValue("Language");
                    final int id = Integer.parseInt(attributes.getValue("ConceptID"));
                    final String rubric = attributes.getValue("Rubric");

                    if (conceptTable.containsKey(language)) {
                        final Hashtable<Integer, String> rubrics = conceptTable.get(language);
                        rubrics.put(id, rubric);
                    } else {
                        final Hashtable<Integer, String> rubrics = new Hashtable<>();
                        rubrics.put(id, rubric);
                        conceptTable.put(language, rubrics);
                    }
                    break;
                }
                case "Grouper": {
                    final Integer id = Integer.parseInt(attributes.getValue("id"));
                    final Integer conceptID = Integer.parseInt(attributes.getValue("ConceptID"));

                    grouperTable.put(conceptID, id);
                    break;
                }
                case "GroupedConcept":
                    final Integer grouperID = Integer.parseInt(attributes.getValue("GrouperID"));
                    final Integer childID = Integer.parseInt(attributes.getValue("ChildID"));

                    if (groupedConceptTable.containsKey(grouperID)) {
                        final Vector<Integer> v = groupedConceptTable.get(grouperID);
                        v.add(childID);
                    } else {
                        final Vector<Integer> v = new Vector<>();
                        v.add(childID);
                        groupedConceptTable.put(grouperID, v);
                    }
                    break;
                case "TerminologyIdentifiers":
                    final String vsab = attributes.getValue("VSAB");
                    final String sourceName = attributes.getValue("SourceName");

                    terminologyIdTable.put(vsab, sourceName);
                    break;
                case "Territory":
                    final Integer territoryId = Integer.parseInt(
                        attributes.getValue("NumericCode"));
                    final String twoLetter = attributes.getValue("TwoLetter");
                    final String threeLetter = attributes.getValue("ThreeLetter");
                    final String text = attributes.getValue("Text");

                    final Vector<String> vattr = new Vector<>();
                    vattr.add(twoLetter);
                    vattr.add(threeLetter);
                    vattr.add(text);

                    territoryTable.put(territoryId, vattr);

                    break;
                default:
                    throw new IllegalArgumentException("Unsupported element: " + localName);
            }
        } catch (NullPointerException e) {
            logger.error("NullPointerException occurred", e);
        }
    }

    public Map<String, String> getLanguageTable() {
        return languageTable;
    }

    public Map<Integer, String> getPrimaryRubricTable() {
        return primaryRubricTable;
    }

    public Map<String, Hashtable<Integer, String>> getConceptTable() {
        return conceptTable;
    }

    public Map<Integer, Integer> getGrouperTable() {
        return grouperTable;
    }

    public Map<Integer, Vector<Integer>> getGroupedConceptTable() {
        return groupedConceptTable;
    }

    public Map<Integer, Vector<String>> getTerritoryTable() {
        return territoryTable;
    }


    public Map<String, String> getTerminologyIdTable() {
        return terminologyIdTable;
    }
}


