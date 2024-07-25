/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.openehr.dbformat.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.junit.jupiter.api.Test;

public class RmDbJsonTest {

    private record ByteArrayTest(byte[] data) {}

    @Test
    void readTreeByteArrayBase64() throws JsonProcessingException {

        JsonNode jsonNode =
                RmDbJson.MARSHAL_OM.readTree("""
                {"data":"U2hhbGwgQmUgQmFzZTY0IGVuY29kZWQ="}""");
        assertThat(jsonNode.get("data").asText()).isEqualTo("U2hhbGwgQmUgQmFzZTY0IGVuY29kZWQ=");
        ByteArrayTest byteArrayTest = RmDbJson.MARSHAL_OM.convertValue(jsonNode, ByteArrayTest.class);
        assertThat(byteArrayTest.data).containsExactly("Shall Be Base64 encoded".getBytes());
    }

    @Test
    void readTreeByteArrayBase64UTF8() throws JsonProcessingException {

        JsonNode jsonNode = RmDbJson.MARSHAL_OM.readTree("""
                {"data":"U29tZUTDpHTDtg=="}""");
        assertThat(jsonNode.get("data").asText()).isEqualTo("U29tZUTDpHTDtg==");
        ByteArrayTest byteArrayTest = RmDbJson.MARSHAL_OM.convertValue(jsonNode, ByteArrayTest.class);
        assertThat(byteArrayTest.data).containsExactly("SomeDätö".getBytes());
    }

    @Test
    void readTreeDvMultimediaType() throws JsonProcessingException {

        JsonNode jsonNode = RmDbJson.MARSHAL_OM.readTree(
                """
                {"_type":"DV_MULTIMEDIA","data":"VGVzdERhdGE=","media_type":{"_type":"CODE_PHRASE","terminology_id":{"_type":"TERMINOLOGY_ID","value":"IANA_media-type"},"code_string":"application/pdf"},"size":8}""");
        assertThat(jsonNode.get("data").asText()).isEqualTo("VGVzdERhdGE=");
        DvMultimedia dvMultimedia = RmDbJson.MARSHAL_OM.convertValue(jsonNode, DvMultimedia.class);
        assertThat(dvMultimedia.getMediaType())
                .isEqualTo(new CodePhrase(new TerminologyId("IANA_media-type"), "application/pdf"));
        assertThat(dvMultimedia.getSize()).isEqualTo(8);
        assertThat(dvMultimedia.getData()).containsExactly("TestData".getBytes());
    }

    @Test
    void valueToTreeByteArrayBase64() {

        var data = "Shall Be Base64 encoded";
        JsonNode jsonNode = RmDbJson.MARSHAL_OM.valueToTree(new ByteArrayTest(data.getBytes()));
        assertThat(jsonNode.get("data").asText()).isEqualTo("U2hhbGwgQmUgQmFzZTY0IGVuY29kZWQ=");
        assertThat(jsonNode).hasToString("""
                {"data":"U2hhbGwgQmUgQmFzZTY0IGVuY29kZWQ="}""");
    }

    @Test
    void valueToTreeByteArrayBase64UTF8() {

        var data = "SomeDätö";
        JsonNode jsonNode = RmDbJson.MARSHAL_OM.valueToTree(new ByteArrayTest(data.getBytes()));
        assertThat(jsonNode.get("data").asText()).isEqualTo("U29tZUTDpHTDtg==");
        assertThat(jsonNode).hasToString("""
                {"data":"U29tZUTDpHTDtg=="}""");
    }

    @Test
    void valueToTreeDvMultimediaType() {

        var data = "TestData";
        DvMultimedia multimedia = new DvMultimedia();
        multimedia.setMediaType(new CodePhrase(new TerminologyId("IANA_media-type"), "application/pdf"));
        multimedia.setData(data.getBytes());
        multimedia.setSize(data.getBytes().length);

        JsonNode jsonNode = RmDbJson.MARSHAL_OM.valueToTree(multimedia);
        assertThat(jsonNode.get("data").asText()).isEqualTo("VGVzdERhdGE=");
        assertThat(jsonNode)
                .hasToString(
                        """
                {"_type":"DV_MULTIMEDIA","data":"VGVzdERhdGE=","media_type":{"_type":"CODE_PHRASE","terminology_id":{"_type":"TERMINOLOGY_ID","value":"IANA_media-type"},"code_string":"application/pdf"},"size":8}""");
    }
}
