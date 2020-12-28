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

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class reads an XML file representing a collection of all the terminologies used in the
 * editor and their corresponding values and places them into tables to be used by the editor.
 *
 * @author Mattias Forss
 * @author Christian Chevalley: added parsing of Territory Elements
 */
public class TerminologyReader extends DefaultHandler {
  private InputStream source;
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
      final SAXParser saxParser = factory.newSAXParser();
      // saxParser.parse(new InputSource(source), this);
      saxParser.parse(source, this);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initialiseTables() {
    languageTable = new Hashtable<String, String>();
    primaryRubricTable = new Hashtable<Integer, String>();
    conceptTable = new Hashtable<String, Hashtable<Integer, String>>();
    grouperTable = new Hashtable<Integer, Integer>();
    groupedConceptTable = new Hashtable<Integer, Vector<Integer>>();
    terminologyIdTable = new Hashtable<String, String>();
    territoryTable = new Hashtable<Integer, Vector<String>>();
  }

  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    try {
      // System.out.println(attributes.getQName(0));
      // System.out.println(attributes.getValue(2));
      // System.out.println(qName);
      // attrib.add(attributes);
      // System.out.println(attributes.getValue("id"));
      if (qName.equals("Language")) {
        final String code = attributes.getValue("code");
        final String description = attributes.getValue("Description");

        if (code != null && description != null) {
          languageTable.put(code, description);
        }
      } else if (qName.equals("PrimaryRubric")) {
        final Integer id = Integer.parseInt(attributes.getValue("Id"));
        final String language = attributes.getValue("Language");

        if (id != null && language != null) {
          primaryRubricTable.put(id, language);
        }
      } else if (qName.equals("Concept")) {
        final String language = attributes.getValue("Language");
        final int id = Integer.parseInt(attributes.getValue("ConceptID"));
        final String rubric = attributes.getValue("Rubric");

        if (conceptTable.containsKey(language)) {
          final Hashtable<Integer, String> rubrics = conceptTable.get(language);
          rubrics.put(id, rubric);
        } else {
          final Hashtable<Integer, String> rubrics = new Hashtable<Integer, String>();
          rubrics.put(id, rubric);
          conceptTable.put(language, rubrics);
        }
      } else if (qName.equals("Grouper")) {
        final Integer id = Integer.parseInt(attributes.getValue("id"));
        final Integer conceptID = Integer.parseInt(attributes.getValue("ConceptID"));

        grouperTable.put(conceptID, id);
      } else if (qName.equals("GroupedConcept")) {
        final Integer grouperID = Integer.parseInt(attributes.getValue("GrouperID"));
        final Integer childID = Integer.parseInt(attributes.getValue("ChildID"));

        if (groupedConceptTable.containsKey(grouperID)) {
          final Vector<Integer> v = groupedConceptTable.get(grouperID);
          v.add(childID);
        } else {
          final Vector<Integer> v = new Vector<Integer>();
          v.add(childID);
          groupedConceptTable.put(grouperID, v);
        }
      } else if (qName.equals("TerminologyIdentifiers")) {
        final String vsab = attributes.getValue("VSAB");
        final String sourceName = attributes.getValue("SourceName");

        terminologyIdTable.put(vsab, sourceName);
      } else if (qName.equals("Territory")) {
        final Integer territoryId = Integer.parseInt(attributes.getValue("NumericCode"));
        final String twoLetter = attributes.getValue("TwoLetter");
        final String threeLetter = attributes.getValue("ThreeLetter");
        final String text = attributes.getValue("Text");

        final Vector<String> vattr = new Vector<String>();
        vattr.add(twoLetter);
        vattr.add(threeLetter);
        vattr.add(text);

        territoryTable.put(territoryId, vattr);
      }
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  public Hashtable<String, String> getLanguageTable() {
    return languageTable;
  }

  public Hashtable<Integer, String> getPrimaryRubricTable() {
    return primaryRubricTable;
  }

  public Hashtable<String, Hashtable<Integer, String>> getConceptTable() {
    return conceptTable;
  }

  public Hashtable<Integer, Integer> getGrouperTable() {
    return grouperTable;
  }

  public Hashtable<Integer, Vector<Integer>> getGroupedConceptTable() {
    return groupedConceptTable;
  }

  public Hashtable<Integer, Vector<String>> getTerritoryTable() {
    return territoryTable;
  }

  public Hashtable<String, String> getTerminologyIdTable() {
    return terminologyIdTable;
  }
}
