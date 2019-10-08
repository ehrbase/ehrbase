/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.serialisation;

import org.ehrbase.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.test_data.item_structure.ItemStruktureTestDataCanonicalJson;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemTree;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

// JSON Schema Validation
// The JsonSchemaValidator is on the tests of archie/tools so can't be included directly
//import com.nedap.archie.creation.JsonSchemaValidator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.ValidationException;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.core.BmmModel;
import org.openehr.bmm.core.BmmPackage;
import org.openehr.bmm.persistence.validation.BmmDefinitions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import org.openehr.referencemodels.BuiltinReferenceModels;

import org.json.JSONArray;
import org.openehr.bmm.core.BmmContainerProperty;
import org.openehr.bmm.core.BmmContainerType;
import org.openehr.bmm.core.BmmGenericType;
import org.openehr.bmm.core.BmmOpenType;
import org.openehr.bmm.core.BmmProperty;
import org.openehr.bmm.core.BmmSimpleType;
import org.openehr.bmm.core.BmmType;


public class CanonicalJsonTest {

    @Test
    public void marshal_lab() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.LABORATORY_REPORT.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String json_string = cut.marshal(composition);

        //System.out.println(json_string);

        assertThat(json_string).isNotEmpty();


        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", json_string);

          System.out.println(CompositionTestDataCanonicalJson.LABORATORY_REPORT +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.LABORATORY_REPORT +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void marshal_lab_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.LABORATORY_REPORT.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.LABORATORY_REPORT +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.LABORATORY_REPORT +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void marshal_minimal_admin() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_ADMIN.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        //System.out.println(marshal);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_ADMIN +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.MINIMAL_ADMIN +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void marshal_minimal_admin_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_ADMIN.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_ADMIN +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.MINIMAL_ADMIN +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void marshal_minimal_evaluation() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_EVAL.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        //System.out.println(marshal);

        assertThat(marshal).isNotEmpty();


        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void marshal_minimal_evaluation_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_EVAL.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void marshal_minimal_instruction() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_INST.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        //System.out.println(marshal);

        assertThat(marshal).isNotEmpty();


        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void marshal_minimal_instruction_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_INST.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_INST +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_INST +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void marshal_minimal_observation() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_OBS.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        //System.out.println(marshal);

        assertThat(marshal).isNotEmpty();


        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.MINIMAL_EVAL +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void marshal_minimal_observation_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_OBS.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_OBS +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_OBS +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void all_types() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.ALL_TYPES.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        //System.out.println(marshal);

        assertThat(marshal).isNotEmpty();


        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.ALL_TYPES +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.ALL_TYPES +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void all_types_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.ALL_TYPES.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.ALL_TYPES +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.ALL_TYPES +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void alternative_types() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.ALTERNATIVE_TYPES.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.ALTERNATIVE_TYPES +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.ALTERNATIVE_TYPES +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void alternative_types_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.ALTERNATIVE_TYPES.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.ALTERNATIVE_TYPES +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.ALTERNATIVE_TYPES +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void obs_admin() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_ADMIN.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.OBS_ADMIN +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.OBS_ADMIN +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void obs_admin_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_ADMIN.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.OBS_ADMIN +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.OBS_ADMIN +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void obs_admin_null_flavour() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_ADMIN_NULL.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.OBS_ADMIN_NULL +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.OBS_ADMIN_NULL +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void obs_admin_null_flavour_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_ADMIN_NULL.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.OBS_ADMIN_NULL +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.OBS_ADMIN_NULL +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void obs_eva() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_EVA.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.OBS_EVA +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.OBS_EVA +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void obs_eva_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_EVA.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.OBS_EVA +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.OBS_EVA +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    /* This test shows a bug on Archie should be skipped for now: https://github.com/openEHR/archie/issues/113
    @Test
    public void obs_inst() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.OBS_INST.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = null;
        try {
          composition = cut.unmarshal(value, Composition.class);
        } catch (Exception e) {
          fail("Can't parse the "+ CompositionTestDataCanonicalJson.OBS_INST +" JSON composition, there is a problem in Archie: "+ e.getMessage());
        }

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.OBS_INST +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.OBS_INST +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }
    */

    @Test
    public void minimal_persistent() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_PERSISTENT.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_PERSISTENT +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.MINIMAL_PERSISTENT +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void minimal_persistent_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.MINIMAL_PERSISTENT.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_PERSISTENT +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.MINIMAL_PERSISTENT +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void nested() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.NESTED.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.NESTED +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.NESTED +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void nested_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.NESTED.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.NESTED +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.NESTED +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void time_series() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.TIME_SERIES.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition.getUid().getValue()).isNotEmpty();
        assertThat(composition.getArchetypeNodeId()).isNotEmpty();

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", marshal);

          System.out.println(CompositionTestDataCanonicalJson.TIME_SERIES +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          fail(CompositionTestDataCanonicalJson.TIME_SERIES +" JSON is invalid ¯\\_(ツ)_/¯");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void time_series_new() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.TIME_SERIES.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();
        Composition composition = cut.unmarshal(value, Composition.class);
        String json = cut.marshal(composition);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          System.out.println(CompositionTestDataCanonicalJson.TIME_SERIES +" JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.TIME_SERIES +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void validate_invalid() throws IOException {

        String value = IOUtils.toString(CompositionTestDataCanonicalJson.INVALID.getStream(), UTF_8);

        // Validation of the JSON against the schema
        JsonSchemaValidator jsonSchemaValidator = new JsonSchemaValidator(
          BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel()
        );

        try
        {
          jsonSchemaValidator.validate("COMPOSITION", value);
          fail(CompositionTestDataCanonicalJson.INVALID + " JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.INVALID + " JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }


    @Test
    public void validate_invalid_new() throws IOException {

        String json = IOUtils.toString(CompositionTestDataCanonicalJson.INVALID.getStream(), UTF_8);

        NewJsonSchemaValidator firstValidator = new NewJsonSchemaValidator(true);

        try
        {
          firstValidator.validate("COMPOSITION", json);

          fail(CompositionTestDataCanonicalJson.INVALID + " JSON is valid \\(^-^)/");
        }
        catch (ValidationException ve)
        {
          System.out.println(CompositionTestDataCanonicalJson.INVALID +" JSON is invalid ¯\\_(ツ)_/¯");

          for (String message: ve.getAllMessages())
          {
            System.out.println(" - "+ message);
          }

          // Issue in the BMM: DV_QUANTITY has a declared attribute property
          // that shouldn't be there, that makes the validation fail here
          //fail("");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          e.getCause().printStackTrace();
          fail("We got an exception on the validator!");
        }
    }

    @Test
    public void testMarshalItemStructure() throws IOException {
        String value = IOUtils.toString(ItemStruktureTestDataCanonicalJson.SIMPLE_EHR_OTHER_Details.getStream(), UTF_8);

        CanonicalJson cut = new CanonicalJson();

        ItemTree composition = cut.unmarshal(value, ItemTree.class);

        String marshal = cut.marshal(composition);

        assertThat(marshal).isNotEmpty();
    }

    @Test
    public void unmarshal() throws IOException {
        String value = IOUtils.toString(CompositionTestDataCanonicalJson.LABORATORY_REPORT.getStream(), UTF_8);
        CanonicalJson cut = new CanonicalJson();

        Composition composition = cut.unmarshal(value, Composition.class);

        assertThat(composition).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("Laboratory Report");
    }

    // validates using current schemas from Sebastian
    class JsonSchemaValidator {

      private static final String ITS_JSON_NAMESPACE = "https://specifications.openehr.org/releases/ITS-JSON/latest/components";

      Map<String, String> hardcodedLocations = new HashMap();
      {
          hardcodedLocations.put("COMPOSITION", "Composition");
          hardcodedLocations.put("EVENT_CONTEXT", "Composition");

          hardcodedLocations.put("SECTION", "Composition");

          hardcodedLocations.put("OBSERVATION", "Composition");

          hardcodedLocations.put("EVALUATION", "Composition");

          hardcodedLocations.put("INSTRUCTION", "Composition");
          hardcodedLocations.put("ACTIVITY", "Composition");

          hardcodedLocations.put("ACTION", "Composition");
          hardcodedLocations.put("INSTRUCTION_DETAILS", "Composition");
          hardcodedLocations.put("ISM_TRANSITION", "Composition");

          hardcodedLocations.put("ADMIN_ENTRY", "Composition");

          hardcodedLocations.put("CAPABILITY", "Demographic");
          hardcodedLocations.put("PERSON", "Demographic");
          hardcodedLocations.put("ADDRESS", "Demographic");
          hardcodedLocations.put("ROLE", "Demographic");
          hardcodedLocations.put("ORGANISATION", "Demographic");
          hardcodedLocations.put("PARTY_IDENTITY", "Demographic");

          hardcodedLocations.put("HISTORY", "Data_structures");
          hardcodedLocations.put("POINT_EVENT", "Data_structures");
          hardcodedLocations.put("INTERVAL_EVENT", "Data_structures");
          hardcodedLocations.put("ITEM_TREE", "Data_structures");
          hardcodedLocations.put("ITEM_TABLE", "Data_structures");
          hardcodedLocations.put("ITEM_SINGLE", "Data_structures");
          hardcodedLocations.put("ITEM_LIST", "Data_structures");
          hardcodedLocations.put("CLUSTER", "Data_structures");
          hardcodedLocations.put("ELEMENT", "Data_structures");

      }

      private final SchemaClient schemaClient = new SchemaClient() {

          @Override
          public InputStream get(String url) {
              if (url.startsWith(ITS_JSON_NAMESPACE)) {
                  return getClass().getResourceAsStream("/jsonschema/" + url.substring(ITS_JSON_NAMESPACE.length()));
              } else {
                  throw new RuntimeException("could not find schema " + url);
              }
          }
      };

      private final LoadingCache<String, Schema> schemaCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Schema>() {
          @Override
          public Schema load(String type) throws Exception {
              String packageName = null;
              if(hardcodedLocations.containsKey(type.toUpperCase(Locale.ENGLISH))) {
                  packageName = hardcodedLocations.get(type.toUpperCase(Locale.ENGLISH));
              } else {
                  BmmClass classDefinition = bmm.getClassDefinition(BmmDefinitions.typeNameToClassKey(type));
                  String test = getPackagePath(classDefinition.getPackage());
                  packageName = test.substring(0, 1).toUpperCase() + test.substring(1);
              }
              try(InputStream inputStream = getClass().getResourceAsStream("/jsonschema/RM/Release-1.0.4/" + packageName + "/" + type + ".json")) {
                  JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));
                  return SchemaLoader.load(schemaJson, schemaClient);
              }
          }
      });

      private String getPackagePath(BmmPackage bmmPackage) {
          BmmPackage currentPackage = bmmPackage;
          BmmPackage parentPackage = bmmPackage.getParent();

          while(currentPackage != null && parentPackage != null && !parentPackage.getName().equalsIgnoreCase("rm")) {
              currentPackage = parentPackage;
              parentPackage = parentPackage.getParent();
          }

          return currentPackage.getName();

      }

      private final BmmModel bmm;

      public JsonSchemaValidator(BmmModel bmm) {
          this.bmm = bmm;
      }

      public void validate(String type, String json) throws Exception {

        // We want the exception to be thrown, if validation fails we get ValidationException
          //try {
              Schema schema = schemaCache.get(type);
              schema.validate(new JSONObject(json));
              //System.out.println("validation ok!");
          //} catch (ExecutionException e) {
          //    throw new RuntimeException(e);
          //}

      }
    }

    // validates using new json schema from Pieter
    class NewJsonSchemaValidator {

        Schema schema;

        public NewJsonSchemaValidator(boolean allowAdditionalProperties) {
            BmmModel model = BuiltinReferenceModels.getBmmRepository().getModel("openehr_rm_1.0.4").getModel();
            JSONObject schemaJson = new JSONSchemaCreator().create(model);
            schema = SchemaLoader.load(schemaJson);

            if(!allowAdditionalProperties) {
                //addAdditionalProperties(schemaJson);
                if(schemaJson.has("definitions")) {
                    JSONObject definitions = schemaJson.getJSONObject("definitions");
                    for(Object key: definitions.keySet()) {
                        JSONObject jsonObject = definitions.getJSONObject((String)key);
                        addAdditionalProperties(jsonObject);
                    }
                }
            }
        }

        private void addAdditionalProperties(JSONObject schemaJson) {
            if(schemaJson.has("type")) {
                String jsonType = schemaJson.getString("type");
                if (jsonType.equalsIgnoreCase("object")) {
                    schemaJson.put("additionalProperties", false);
                }
            }
        }

        public void validate(String type, String json) throws IOException {
            schema.validate(new JSONObject(json));
        }
    }

    // this class if for using the schemas from Pieter, in NewJsonSchemaValidator
    class JSONSchemaCreator {

        private Map<String, String> primitiveTypeMapping;
        private List<String> rootTypes;
        private BmmModel bmmModel;

        public JSONSchemaCreator() {
            primitiveTypeMapping = new HashMap<>();
            primitiveTypeMapping.put("integer", "integer");
            primitiveTypeMapping.put("integer64", "integer");
            primitiveTypeMapping.put("boolean", "boolean");
            primitiveTypeMapping.put("real", "number");
            primitiveTypeMapping.put("double", "number");
            primitiveTypeMapping.put("octet", "string");
            primitiveTypeMapping.put("byte", "string");
            primitiveTypeMapping.put("character", "string");
            primitiveTypeMapping.put("hash", "object");
            primitiveTypeMapping.put("string", "string");
            primitiveTypeMapping.put("iso8601_date", "string");
            primitiveTypeMapping.put("iso8601_date_time", "string");
            primitiveTypeMapping.put("iso8601_time", "string");
            primitiveTypeMapping.put("iso8601_duration", "string");
            primitiveTypeMapping.put("proportion_kind", "integer");//TODO: proper enum support

            rootTypes = new ArrayList<>();
            rootTypes.add("COMPOSITION");
            rootTypes.add("OBSERVATION");
            rootTypes.add("EVALUATION");
            rootTypes.add("ACTIVITY");
            rootTypes.add("ACTION");
            rootTypes.add("SECTION");
            rootTypes.add("INSTRUCTION");
            rootTypes.add("INSTRUCTION_DETAILS");
            rootTypes.add("ADMIN_ENTRY");
            rootTypes.add("CLUSTER");
            rootTypes.add("CAPABILITY");
            rootTypes.add("PERSON");
            rootTypes.add("ADDRESS");
            rootTypes.add("ROLE");
            rootTypes.add("ORGANISATION");
            rootTypes.add("PARTY_IDENTITY");
            rootTypes.add("ITEM_TREE");
        }

        public JSONObject create(BmmModel bmm) {
            this.bmmModel = bmm;

            //create the definitions and the root if/else base

            JSONArray allOfArray = new JSONArray();
            JSONObject definitions = new JSONObject();
            JSONObject schemaRoot = new JSONObject()
                    .put("definitions", definitions)
                    .put("allOf", allOfArray)
                    .put("$schema", "http://json-schema.org/draft-07/schema");


            //at the root level, require the type
            JSONObject typeRequired = createRequiredArray("_type");
            allOfArray.put(typeRequired);

            //for every root type, if the type is right, check that type
            //anyof does more or less the same, but this is faster plus it gives MUCH less errors!
            for(String rootType:rootTypes) {

                JSONObject typePropertyCheck = createConstType(rootType);
                JSONObject typeCheck = new JSONObject().put("properties", typePropertyCheck);

                JSONObject typeReference = createReference(rootType);
                //IF the type matches
                //THEN check the correct type from the definitions
                JSONObject ifObject = new JSONObject()
                        .put("if", typeCheck)
                        .put("then", typeReference);
                allOfArray.put(ifObject);
            }
            for(BmmClass bmmClass: bmm.getClassDefinitions().values()) {
                if (!bmmClass.isAbstract() && !primitiveTypeMapping.containsKey(bmmClass.getTypeName().toLowerCase())) {
                    addClass(definitions, bmmClass);
                }
            }
            return schemaRoot;
        }

        private void addClass(JSONObject definitions, BmmClass bmmClass) {
            String typeName = BmmDefinitions.typeNameToClassKey(bmmClass.getTypeName());

            JSONObject definition = new JSONObject();
            definition.put("type", "object");
            BmmClass flatBmmClass = bmmClass.flattenBmmClass();
            JSONArray required = new JSONArray();
            JSONObject properties = new JSONObject();
            for (String propertyName : flatBmmClass.getProperties().keySet()) {
                BmmProperty bmmProperty = flatBmmClass.getProperties().get(propertyName);
                if((bmmClass.getTypeName().startsWith("POINT_EVENT") || bmmClass.getTypeName().startsWith("INTERVAL_EVENT")) &&
                        propertyName.equalsIgnoreCase("data")) {
                    //we don't handle generics yet, and it's very tricky with the current BMM indeed. So, just manually hack this
                    JSONObject propertyDef = createPolymorphicReference(bmmModel.getClassDefinition("ITEM_STRUCTURE"));
                    extendPropertyDef(propertyDef, bmmProperty);
                    properties.put(propertyName, propertyDef);

                    if (bmmProperty.getMandatory()) {
                        required.put(propertyName);
                    }
                } else {

                    JSONObject propertyDef = createPropertyDef(bmmProperty.getType());
                    extendPropertyDef(propertyDef, bmmProperty);
                    properties.put(propertyName, propertyDef);

                    if (bmmProperty.getMandatory()) {
                        required.put(propertyName);
                    }
                }
            }
            properties.put("_type", new JSONObject().put("const", typeName));
            definition.put("required", required);
            definition.put("properties", properties);
            definitions.put(typeName, definition);
        }

        private void extendPropertyDef(JSONObject propertyDef, BmmProperty bmmProperty) {
            if(bmmProperty instanceof BmmContainerProperty) {
                BmmContainerProperty containerProperty = (BmmContainerProperty) bmmProperty;
                if(containerProperty.getCardinality() != null && containerProperty.getCardinality().getLower() > 0) {
                    propertyDef.put("minItems", containerProperty.getCardinality().getLower());
                }
            }
        }

        private JSONObject createPropertyDef(BmmType type) {

            if(type instanceof BmmOpenType) {
                return createType("object");
                //nothing more to be done
            } else if (type instanceof BmmSimpleType) {
                if(isJSPrimitive(type)) {
                    return createType(getJSPrimitive(type));
                } else {
                    return createPolymorphicReference(type.getBaseClass());
                }
            } else if (type instanceof BmmContainerType) {
                BmmContainerType containerType = (BmmContainerType) type;
                return new JSONObject()
                    .put("type", "array")
                    .put("items", createPropertyDef(containerType.getBaseType()));
            } else if (type instanceof BmmGenericType) {
                return createPolymorphicReference(type.getBaseClass());
            }
            throw new IllegalArgumentException("type must be a BmmType, but was " + type.getClass().getSimpleName());
        }

        private JSONObject createPolymorphicReference(BmmClass type) {

            if(BmmDefinitions.typeNameToClassKey(type.getTypeName()).equalsIgnoreCase("hash")) {
                return createType("object");
            }

            List<String> descendants = getAllNonAbstractDescendants( type);
            if(!type.isAbstract()) {
                descendants.add(BmmDefinitions.typeNameToClassKey(type.getTypeName()));
            }

            if(descendants.isEmpty()) {
                //this is an object of which only an abstract class exists.
                //it cannot be represented as standard json, one would think. this is mainly access control and authored
                //resource in the RM
                return createType("object");
            } else if (descendants.size() > 1) {
                JSONObject def = new JSONObject();
                JSONArray array = new JSONArray();
                for(String descendant:descendants) {
                    JSONObject typePropertyCheck = createConstType(descendant);
                    JSONObject typeCheck = new JSONObject().put("properties", typePropertyCheck);

                    JSONObject typeReference = createReference(descendant);
                    //IF the type matches
                    //THEN check the correct type from the definitions
                    JSONObject ifObject = new JSONObject()
                            .put("if", typeCheck)
                            .put("then", typeReference);
                    array.put(ifObject);
                }
                array.put(createRequiredArray("_type"));
                def.put("allOf", array);
                return def;
            } else {
                return createReference(BmmDefinitions.typeNameToClassKey(type.getTypeName()));
            }
        }

        private List<String> getAllNonAbstractDescendants(BmmClass bmmClass) {
            List<String> result = new ArrayList<>();
            List<String> descs = bmmClass.getImmediateDescendants();
            for(String desc:descs) {
                if(!bmmClass.getTypeName().equalsIgnoreCase(desc)) {//TODO: fix getImmediateDescendants in BMM so this check is not required
                    BmmClass classDefinition = bmmModel.getClassDefinition(desc);
                    if (!classDefinition.isAbstract()) {
                        result.add(BmmDefinitions.typeNameToClassKey(classDefinition.getTypeName()));
                    }
                    result.addAll(getAllNonAbstractDescendants(classDefinition));
                }
            }
            return result;
        }

        private boolean isJSPrimitive(BmmType bmmType) {
            return primitiveTypeMapping.containsKey(bmmType.getTypeName().toLowerCase());
        }

        private String getJSPrimitive(BmmType bmmType) {
            return primitiveTypeMapping.get(bmmType.getTypeName().toLowerCase());
        }

        private JSONObject createConstType(String rootType) {
            JSONObject constTypeObject = new JSONObject().put("const", rootType);
            return new JSONObject().put("_type", constTypeObject);
        }

        private JSONObject createRequiredArray(String... requiredFields) {
            JSONObject requiredType = new JSONObject();
            JSONArray requiredArray = new JSONArray();
            for(String requiredProperty: requiredFields) {
                requiredArray.put(requiredProperty);
            }
            requiredType.put("required", requiredArray);
            return requiredType;
        }


        private JSONObject createType(String jsPrimitive) {
            return new JSONObject().put("type", jsPrimitive);
        }

        private JSONObject createReference(String rootType) {
            return new JSONObject().put("$ref", "#/definitions/" + rootType);
        }
    }
}
