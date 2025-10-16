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
package org.ehrbase.openehr.dbformat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.Evaluation;
import com.nedap.archie.rm.composition.Observation;
import com.nedap.archie.rm.composition.Section;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datastructures.ItemTree;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalJson;
import org.jooq.JSONB;
import org.jooq.Record2;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DbToRmFormatTest {

    @Test
    void testTypeAlias() {
        assertThat(DbToRmFormat.TYPE_ALIAS).isEqualTo(RmAttributeAlias.getAlias(DbToRmFormat.TYPE_ATTRIBUTE));
    }

    @Test
    void toCompositionFromTestIPS() throws IOException {

        String data = loadDbOneJson("ips");
        Composition composition = DbToRmFormat.reconstructRmObject(Composition.class, data);

        assertThat(composition.getArchetypeDetails()).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue())
                .isEqualTo("International Patient Summary");

        Composition expectedComposition = new CanonicalJson()
                .unmarshal(IOUtils.toString(CompositionTestDataCanonicalJson.IPS.getStream(), StandardCharsets.UTF_8));

        Stream<Function<Composition, ?>> paths = Stream.of(
                c -> c.getArchetypeDetails().getTemplateId().getValue(),
                c -> c.getContent().get(2).getName().getValue(),
                c -> ((PartyIdentified) ((Evaluation) ((Section) c.getContent().get(0))
                                        .getItems()
                                        .get(1))
                                .getOtherParticipations()
                                .get(1)
                                .getPerformer())
                        .getName());

        paths.forEach(p -> assertThat(p.apply(composition)).isEqualTo(p.apply(expectedComposition)));

        compareJson(composition, expectedComposition);
    }

    @Test
    void toCompositionFromTestAllTypes() throws IOException {
        String data = loadDbOneJson("all_types_no_multimedia");
        Composition composition = DbToRmFormat.reconstructRmObject(Composition.class, data);

        assertThat(composition.getArchetypeDetails()).isNotNull();
        assertThat(composition.getArchetypeDetails().getTemplateId().getValue()).isEqualTo("test_all_types.en.v1");

        Composition expectedComposition = new CanonicalJson()
                .unmarshal(IOUtils.toString(
                        CompositionTestDataCanonicalJson.ALL_TYPES.getStream(), StandardCharsets.UTF_8));

        Stream<Function<Composition, ?>> paths = Stream.of(
                c -> c.getArchetypeDetails().getTemplateId().getValue(),
                c -> c.getContent().get(2).getName().getValue(),
                c -> ((DvCount) ((Element) ((Observation) c.getContent().get(0))
                                        .getData()
                                        .getEvents()
                                        .get(0)
                                        .getData()
                                        .getItems()
                                        .get(4))
                                .getValue())
                        .getMagnitude());

        paths.forEach(p -> assertThat(p.apply(composition)).isEqualTo(p.apply(expectedComposition)));

        compareJson(composition, expectedComposition);
    }

    @Test
    void toHistoryFromTestAllTypes() throws IOException {
        String data = loadDbOneJson("all_types_no_multimedia_content1_data");
        ItemTree actual = DbToRmFormat.reconstructRmObject(ItemTree.class, data);

        assertThat(actual.getArchetypeNodeId()).isNotNull();

        Composition sourceComposition = new CanonicalJson()
                .unmarshal(IOUtils.toString(
                        CompositionTestDataCanonicalJson.ALL_TYPES.getStream(), StandardCharsets.UTF_8));

        ItemTree expected =
                (ItemTree) ((Evaluation) sourceComposition.getContent().get(1)).getData();

        Stream<Function<ItemTree, ?>> paths = Stream.of(
                Locatable::getArchetypeNodeId,
                h -> h.getItems().get(1).getName().getValue());

        paths.forEach(p -> assertThat(p.apply(actual)).isEqualTo(p.apply(expected)));

        compareJson(actual, expected);
    }

    private static String loadDbOneJson(String name) {
        var dataPath = "./%s.db_aliased.json".formatted(name);
        try (var resourceStream = DbToRmFormatTest.class.getResourceAsStream(dataPath)) {
            return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void compareJson(RMObject composition, RMObject expectedComposition) {

        JsonNode tree = CanonicalJson.MARSHAL_OM.valueToTree(composition);
        JsonNode expectedTree = CanonicalJson.MARSHAL_OM.valueToTree(expectedComposition);

        List<String> issues = new ArrayList<>();
        compareJsonNode(tree, expectedTree, AqlPath.ROOT_PATH, issues);
        if (!issues.isEmpty()) {
            fail(issues.stream().collect(Collectors.joining("\n")));
        }
    }

    private static void compareJsonNode(JsonNode node, JsonNode expectedNode, AqlPath path, List<String> issues) {
        if (node.getNodeType() != expectedNode.getNodeType()) {
            issues.add("Unexpected node type at %s: %s vs. %s "
                    .formatted(path, node.getNodeType(), expectedNode.getNodeType()));
        } else if (node.isNull() || node.isMissingNode()) {
            issues.add("Unexpected node type at %s: %s".formatted(path, node.getNodeType()));
        } else if (node.isNumber()) {
            if (node.doubleValue() != expectedNode.doubleValue()) {
                issues.add("Unexpected value at %s: %s vs. %s "
                        .formatted(path, node.doubleValue(), expectedNode.doubleValue()));
            }
        } else if (node.isBoolean()) {
            if (node.booleanValue() != expectedNode.booleanValue()) {
                issues.add("Unexpected value at %s: %s vs. %s "
                        .formatted(path, node.booleanValue(), expectedNode.booleanValue()));
            }
        } else if (node.isTextual()) {
            if (!node.textValue().equals(expectedNode.textValue())) {
                issues.add("Unexpected value at %s: %s vs. %s "
                        .formatted(path, node.textValue(), expectedNode.textValue()));
            }
        } else if (node.isArray()) {
            if (node.size() != expectedNode.size()) {
                issues.add("Unexpected number of array elements at %s: %s vs. %s "
                        .formatted(path, node.size(), expectedNode.size()));
            }
            int m = Math.min(node.size(), expectedNode.size());
            for (int i = 0; i < m; i++) {
                compareJsonNode(node.get(i), expectedNode.get(i), path.addEnd("" + i), issues);
            }
        } else if (node.isObject()) {
            List<String> names = IteratorUtils.toList(node.fieldNames());
            List<String> expectedNames = IteratorUtils.toList(expectedNode.fieldNames());
            List<String> sharedNames = ListUtils.intersection(names, expectedNames);
            names.removeAll(sharedNames);
            expectedNames.removeAll(sharedNames);
            if (!names.isEmpty()) {
                issues.add("Unexpected nodes at %s: %s".formatted(path, names));
            }
            if (!expectedNames.isEmpty()) {
                issues.add("Missing nodes at %s: %s".formatted(path, expectedNames));
            }

            for (String name : sharedNames) {
                compareJsonNode(node.get(name), expectedNode.get(name), path.addEnd(name), issues);
            }
        }
    }

    private void roundtripTest(CompositionTestDataCanonicalJson example, UnaryOperator<Composition> roundtrip)
            throws IOException {
        Composition expectedComposition =
                new CanonicalJson().unmarshal(IOUtils.toString(example.getStream(), StandardCharsets.UTF_8));
        Composition composition = roundtrip.apply(expectedComposition);
        compareJson(composition, expectedComposition);
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"INVALID"})
    void roundtripTestOne(CompositionTestDataCanonicalJson example) throws IOException {
        roundtripTest(example, c -> {
            String dbJson = createDbOneJson(c);
            return DbToRmFormat.reconstructRmObject(Composition.class, dbJson);
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"INVALID"})
    void roundtripTestOneNode(CompositionTestDataCanonicalJson example) throws IOException {
        roundtripTest(example, c -> {
            Record2<String, ?>[] dbJson = createDbOneJsonArray(c);
            return DbToRmFormat.reconstructRmObject(Composition.class, dbJson);
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"INVALID"})
    void roundtripTestOneArray(CompositionTestDataCanonicalJson example) throws IOException {
        roundtripTest(example, c -> {
            Record2<String, ?>[] dbJson = createDbOneJsonArray(c);
            return DbToRmFormat.reconstructRmObject(Composition.class, dbJson);
        });
    }

    static String createDbOneJson(Composition composition) {
        List<StructureNode> roots = VersionedObjectDataStructure.createDataStructure(composition);
        Stream<Pair<StructureIndex, JsonNode>> stream = roots.stream()
                .filter(r -> r.getStructureRmType().isStructureEntry())
                .map(s -> Pair.of(s.getEntityIdx(), VersionedObjectDataStructure.applyRmAliases(s.getJsonNode())));
        return aggregateJson(stream);
    }

    static Record2<String, ?>[] createDbOneJsonArray(Composition composition) {
        List<StructureNode> roots = VersionedObjectDataStructure.createDataStructure(composition);
        Stream<Pair<StructureIndex, JsonNode>> stream = roots.stream()
                .filter(r -> r.getStructureRmType().isStructureEntry())
                .map(s -> Pair.of(s.getEntityIdx(), VersionedObjectDataStructure.applyRmAliases(s.getJsonNode())));
        return aggregateJsonArray(stream);
    }

    /**
     * Aggregates the json data for the "one" format like the SQL query:
     * <code>
     *   select jsonb_object_agg(d.entity_idx, d.data) as data
     *   from ehr.comp_one d
     *
     *   where s.comp_id = '...'::uuid
     *   group by d.comp_id;
     * </code>
     */
    static String aggregateJson(Stream<Pair<StructureIndex, JsonNode>> dataRows) {
        ObjectNode root = CanonicalJson.MARSHAL_OM.createObjectNode();

        dataRows.forEach(r -> {
            String key = r.getLeft().printIndexString(false, true);
            root.put(key, r.getRight());
        });

        return root.toPrettyString();
    }

    /**
     * Aggregates the json data for the "one" format like the SQL query:
     * <code>
     *   select array_agg((d.entity_idx, d.data)) as data
     *   from ehr.comp_one d
     *
     *   where s.comp_id = '...'::uuid
     *   group by d.comp_id;
     * </code>
     */
    static Record2<String, ?>[] aggregateJsonArray(Stream<Pair<StructureIndex, JsonNode>> dataRows) {
        DefaultDSLContext defaultDSLContext = new DefaultDSLContext(SQLDialect.POSTGRES);
        return dataRows.map(r -> {
                    Record2<String, JSONB> rec =
                            defaultDSLContext.newRecord(DSL.noField(String.class), DSL.noField(JSONB.class));
                    rec.values(
                            r.getLeft().printIndexString(false, true),
                            JSONB.valueOf(r.getRight().toPrettyString()));
                    return rec;
                })
                .toArray(l -> (Record2<String, ?>[]) new Record2[l]);
    }

    @Test
    void testRemovePrefix() {
        assertThat(DbToRmFormat.remainingPath("ab.123".length(), "ab.12345"))
                .isEqualTo(DbToRmFormat.DbJsonPath.parse("45"));
        assertThat(DbToRmFormat.remainingPath("ab.123".length(), "ab.123.45"))
                .isEqualTo(DbToRmFormat.DbJsonPath.parse("45"));

        assertThat(DbToRmFormat.remainingPath(0, "ab.12345")).isEqualTo(DbToRmFormat.DbJsonPath.parse("ab.12345"));
        assertThat(DbToRmFormat.remainingPath(0, ".12345")).isEqualTo(DbToRmFormat.DbJsonPath.parse("12345"));

        assertThat(DbToRmFormat.remainingPath("12345".length(), "12345")).isEqualTo(DbToRmFormat.DbJsonPath.EMPTY_PATH);
        assertThat(DbToRmFormat.remainingPath("12345".length(), "12345."))
                .isEqualTo(DbToRmFormat.DbJsonPath.EMPTY_PATH);
    }

    @Test
    void testDbJsonPath() {
        assertThat(DbToRmFormat.DbJsonPath.parse("").components()).isEmpty();

        assertThat(DbToRmFormat.DbJsonPath.parse("ab.").components())
                .containsExactly(new DbToRmFormat.PathComponent("ab", -1));
        assertThat(DbToRmFormat.DbJsonPath.parse("ab1234.").components())
                .containsExactly(new DbToRmFormat.PathComponent("ab", 1234));
        assertThat(DbToRmFormat.DbJsonPath.parse("ab123456.de6.gh.ij0.").components())
                .containsExactly(
                        new DbToRmFormat.PathComponent("ab", 123456),
                        new DbToRmFormat.PathComponent("de", 6),
                        new DbToRmFormat.PathComponent("gh", -1),
                        new DbToRmFormat.PathComponent("ij", 0));
    }

    @Test
    void reconstructRmObjectDvMultimediaType() {

        DvMultimedia rmObject = DbToRmFormat.reconstructRmObject(
                DvMultimedia.class,
                """
                {"T": "mu", "d": "VGVzdERhdGE=", "mt": {"T": "C", "cd": "application/pdf", "te": {"T": "T", "V": "IANA_media-types"}}, "si": 8}
                """);
        assertThat(rmObject.getMediaType())
                .isEqualTo(new CodePhrase(new TerminologyId("IANA_media-types"), "application/pdf"));
        assertThat(rmObject.getSize()).isEqualTo(8);
        assertThat(rmObject.getData()).containsExactly("TestData".getBytes());
    }
}
