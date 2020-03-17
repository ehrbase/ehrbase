/*
 * Copyright (c) 2020 Stefan Spiska (Vitasystems GmbH) and Luis Marco-Ruiz (Hannover Medical School).
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

package org.ehrbase.aql.compiler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ehrbase.ServiceModuleConfiguration;
import org.ehrbase.api.ApiModuleConfiguration;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.I_VariableDefinitionHelper;
import org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
//import org.ehrbase.application.EhrBase;

import com.nedap.archie.rm.datavalues.DvCodedText;

//@ExtendWith(SpringExtension.class)
//@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
//@RunWith(SpringRunner.class)
//@EnableConfigurationProperties(value=FhirTsPropsImpl.class)
//@TestPropertySource("classpath:application.yml")
//@SpringBootTest//(classes= {org.ehrbase.application.EhrBase.class})
//@ActiveProfiles("test")
public class InvokeVisitorTest {
	
	//@Autowired
	//private FhirTsPropsImpl props;
	
	//@Ignore
	/*
	 * @Test public void shouldPopulateMyConfigurationProperties() {
	 * assertThat(props.getCodePath()).isEqualTo("default");
	 * assertThat(props.getDisplayPath()).isEqualTo("default");
	 * assertThat(props.getSystemPath()).isEqualTo("default"); }
	 */
	
	/*
	 * @EnableConfigurationProperties(FhirTsProps.class) public static class
	 * TestConfiguration { // nothing }
	 */

	//@Ignore("This test runs against ontoserver sample inteance. It is deactivated until we have a test FHIR terminology server.")
	
	 @Autowired
	    private I_OpenehrTerminologyServer<DvCodedText, String> tsserver;
	 
    @Ignore
	 @Test
    public void shouldVisitInvokeExpression() {
    	
    	System.out.println("En el test el TS client es: "+tsserver);

            WhereVisitor cut = new WhereVisitor();
            String aql = "SELECT o/data[at0002]/events[at0003] AS systolic " +
                    "FROM EHR [ehr_id/value='1234'] " +
                    "CONTAINS COMPOSITION c " +
                    "CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.blood_pressure.v1] " +
                    "WHERE c/archetype_details/template_id/value matches {'Flormidal', INVOKE(https://r4.ontoserver.csiro.au/fhir/ValueSet/942e1d78-d481-416f-bebd-5754ba4d0b69/$expand/), 'Kloralhidrat'}";
            ParseTree tree = QueryHelper.setupParseTree(aql);
            cut.visit(tree);

            List<Object> whereExpression = cut.getWhereExpression();
            assertThat(whereExpression).size().isEqualTo(29);

            I_VariableDefinition where1 = (I_VariableDefinition) whereExpression.get(0);
            I_VariableDefinition expected1 = I_VariableDefinitionHelper.build("archetype_details/template_id/value", null, "c", false, false, false);
            I_VariableDefinitionHelper.checkEqualWithoutFuncParameters(where1, expected1);

            assertThat(whereExpression.get(1)).isEqualTo(" IN ");

            assertThat(whereExpression.get(2)).isEqualTo("(");

            assertThat(whereExpression.get(3)).isEqualTo("'Flormidal'");

            assertThat(whereExpression.get(4)).isEqualTo(",");

            assertThat(whereExpression.get(5)).isEqualTo("O");
            
            assertThat(whereExpression.get(6)).isEqualTo(",");
            
            assertThat(whereExpression.get(7)).isEqualTo("M");
            
            assertThat(whereExpression.get(8)).isEqualTo(",");

            assertThat(whereExpression.get(9)).isEqualTo("DO");
            
            assertThat(whereExpression.get(10)).isEqualTo(",");
            
           assertThat(whereExpression.get(11)).isEqualTo("L");
            
            assertThat(whereExpression.get(12)).isEqualTo(",");
            
           assertThat(whereExpression.get(13)).isEqualTo("I");
            
            assertThat(whereExpression.get(14)).isEqualTo(",");
            
           assertThat(whereExpression.get(15)).isEqualTo("V");
            
            assertThat(whereExpression.get(16)).isEqualTo(",");
            
           assertThat(whereExpression.get(17)).isEqualTo("MOD");
            
            assertThat(whereExpression.get(18)).isEqualTo(",");
            
           assertThat(whereExpression.get(19)).isEqualTo("MO");
            
            assertThat(whereExpression.get(20)).isEqualTo(",");
            
           assertThat(whereExpression.get(21)).isEqualTo("D");
            
            assertThat(whereExpression.get(22)).isEqualTo(",");
            
           assertThat(whereExpression.get(23)).isEqualTo("DI");
            
            assertThat(whereExpression.get(24)).isEqualTo(",");
            
           assertThat(whereExpression.get(25)).isEqualTo("B");
            
            assertThat(whereExpression.get(26)).isEqualTo(",");
           
            assertThat(whereExpression.get(27)).isEqualTo("'Kloralhidrat'");

            assertThat(whereExpression.get(28)).isEqualTo(")");

    }
    
	/*
	 * @SpringBootApplication
	 * 
	 * @Import({ServiceModuleConfiguration.class,
	 * RestEHRScapeModuleConfiguration.class, RestOpenehrModuleConfiguration.class,
	 * ApiModuleConfiguration.class}) public class EhrBase {
	 * 
	 * public static void main(String[] args) { SpringApplication.run(EhrBase.class,
	 * args); } }
	 */
    
}