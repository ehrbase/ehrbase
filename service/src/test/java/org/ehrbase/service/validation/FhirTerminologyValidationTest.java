/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonContext;
import com.nedap.archie.rm.datavalues.DvCodedText;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.java.quickcheck.generator.PrimitiveGenerators;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.ehrbase.service.validation.FhirTerminologyValidation.ValueSetConverter;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FhirTerminologyValidationTest {

    @Test
    public void guaranteePrefix() {
        Assertions.assertEquals("url=ABC", FhirTerminologyValidation.guaranteePrefix("url=", "url=ABC"));
        Assertions.assertEquals("url=ABC", FhirTerminologyValidation.guaranteePrefix("url=", "ABC"));
        Assertions.assertNull(FhirTerminologyValidation.guaranteePrefix("url=", ""));
        Assertions.assertEquals(
                "xyz=XYZ&url=ABC", FhirTerminologyValidation.guaranteePrefix("url=", "xyz=XYZ&url=ABC"));
    }

    @Test
    public void renderTempl() {
        String ref = String.format(FhirTerminologyValidation.SUPPORTS_CODE_SYS_TEMPL, "abc", "123");
        String render1 =
                FhirTerminologyValidation.renderTempl(FhirTerminologyValidation.SUPPORTS_CODE_SYS_TEMPL, "abc", "123");
        Assertions.assertEquals(ref, render1);

        String render2 = FhirTerminologyValidation.renderTempl(
                FhirTerminologyValidation.SUPPORTS_CODE_SYS_TEMPL, "abc", "123", "xyz");
        Assertions.assertEquals(ref, render2);
        Assertions.assertThrows(
                IllegalFormatException.class,
                () -> FhirTerminologyValidation.renderTempl(FhirTerminologyValidation.SUPPORTS_CODE_SYS_TEMPL, "abc"));
    }

    @Test
    void valueSetConverter() throws Exception {
        ValueSet values = anyValueSet();

        String json = FhirContext.forR4().newJsonParser().encodeResourceToString(values);
        DocumentContext ctx = JsonPath.parse(json);

        List<DvCodedText> dv = ValueSetConverter.convert(ctx);

        Assertions.assertEquals(values.getExpansion().getContains().size(), dv.size());
    }

    @Test
    void stringjoin() {
        List<String> params = new ArrayList<>();
        String reqParam = params.stream().collect(Collectors.joining("&"));
        Assertions.assertEquals("", reqParam);

        params = List.of("a");
        reqParam = params.stream().collect(Collectors.joining("&"));
        Assertions.assertEquals("a", reqParam);

        params = List.of("a", "b");
        reqParam = params.stream().collect(Collectors.joining("&"));
        Assertions.assertEquals("a&b", reqParam);
    }

    @Test
    void supports_ValidParams_ReturnsTrue() {
        String baseUrl = "http://terminology.local";
        String valueSetUrl = "http://snomed.info/sct?fhir_vs=ecl/%3C306206005";

        FhirTerminologyValidation validation = spy(new FhirTerminologyValidation(baseUrl));
        TerminologyParam param =
                TerminologyParam.ofFhir("//fhir.hl7.org/ValueSet/$expand?url=" + valueSetUrl + "&activeOnly=true");

        JsonContext jsonContext = mock(JsonContext.class);
        when(jsonContext.read("$.total", int.class)).thenReturn(1);

        doReturn(jsonContext).when(validation).internalGet(Mockito.anyString());

        assertTrue(validation.supports(param));

        verify(validation)
                .internalGet(FhirTerminologyValidation.renderTempl(
                        FhirTerminologyValidation.SUPPORTS_VALUE_SET_TEMPL, baseUrl, valueSetUrl));
    }

    @Test
    void supports_InvalidParams_ReturnsFalse() {
        FhirTerminologyValidation validation = spy(new FhirTerminologyValidation("http://terminology.local"));

        assertFalse(validation.supports(TerminologyParam.ofFhir("//fhir.hl7.org/ValueSet/$expand?activeOnly=true")));
        assertFalse(validation.supports(TerminologyParam.ofFhir(
                "//invalid/ValueSet/$expand?url=http://snomed.info/sct?fhir_vs=ecl/%3C306206005&activeOnly=true")));

        verify(validation, never()).internalGet(Mockito.anyString());
    }

    static ValueSet anyValueSet() {
        List<ValueSetExpansionContainsComponent> values = IntStream.range(0, 16)
                .mapToObj(i -> anyValueSetExpansionContainsComponent())
                .collect(Collectors.toList());

        ValueSetExpansionComponent ext = new ValueSetExpansionComponent();
        ext.setId(anyString());
        ext.setContains(values);

        ValueSet valueSet = new ValueSet();
        valueSet.setId(anyString());
        valueSet.setExpansion(ext);

        return valueSet;
    }

    static ValueSetExpansionContainsComponent anyValueSetExpansionContainsComponent() {
        ValueSetExpansionContainsComponent cmp = new ValueSetExpansionContainsComponent();
        cmp.setId(anyString());
        cmp.setCode(anyString());
        cmp.setSystem(anyString());
        cmp.setDisplay(anyString());
        return cmp;
    }

    static String anyString() {
        return PrimitiveGenerators.letterStrings(1, 16).next();
    }
}
