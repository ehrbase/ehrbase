/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.schemagen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.ehrbase.schemagen.model.ColumnDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TypeMapperTest {

    @Test
    void dvBoolean_mapsToBooleanColumn() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_BOOLEAN", "is_active");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().name()).isEqualTo("is_active");
        assertThat(cols.getFirst().pgType()).isEqualTo("BOOLEAN");
    }

    @Test
    void dvText_mapsToTextColumn() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_TEXT", "comment");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().pgType()).isEqualTo("TEXT");
    }

    @Test
    void dvCodedText_mapsToThreeColumns() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_CODED_TEXT", "setting");
        assertThat(cols).hasSize(3);
        assertThat(cols.get(0).name()).isEqualTo("setting_value");
        assertThat(cols.get(1).name()).isEqualTo("setting_code");
        assertThat(cols.get(2).name()).isEqualTo("setting_terminology");
    }

    @Test
    void dvQuantity_mapsToMagnitudeUnitsPrecision() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_QUANTITY", "systolic");
        assertThat(cols).hasSize(3);
        assertThat(cols.get(0).name()).isEqualTo("systolic_magnitude");
        assertThat(cols.get(0).pgType()).isEqualTo("DOUBLE PRECISION");
        assertThat(cols.get(1).name()).isEqualTo("systolic_units");
        assertThat(cols.get(1).pgType()).isEqualTo("TEXT");
        assertThat(cols.get(2).name()).isEqualTo("systolic_precision");
        assertThat(cols.get(2).pgType()).isEqualTo("INTEGER");
    }

    @Test
    void dvCount_mapsToBigint() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_COUNT", "count");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().pgType()).isEqualTo("BIGINT");
    }

    @Test
    void dvProportion_mapsToNumeratorDenominatorType() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_PROPORTION", "ratio");
        assertThat(cols).hasSize(3);
        assertThat(cols.get(0).name()).isEqualTo("ratio_numerator");
        assertThat(cols.get(1).name()).isEqualTo("ratio_denominator");
        assertThat(cols.get(2).name()).isEqualTo("ratio_type");
    }

    @Test
    void dvOrdinal_mapsToValueAndSymbol() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_ORDINAL", "severity");
        assertThat(cols).hasSize(3);
        assertThat(cols.get(0).pgType()).isEqualTo("INTEGER");
        assertThat(cols.get(1).name()).isEqualTo("severity_symbol_value");
        assertThat(cols.get(2).name()).isEqualTo("severity_symbol_code");
    }

    @Test
    void dvScale_mapsToDoubleAndSymbol() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_SCALE", "pain_level");
        assertThat(cols).hasSize(3);
        assertThat(cols.get(0).pgType()).isEqualTo("DOUBLE PRECISION");
    }

    @Test
    void dvDate_mapsToTextAndMagnitude() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_DATE", "birth_date");
        assertThat(cols).hasSize(2);
        assertThat(cols.get(0).name()).isEqualTo("birth_date");
        assertThat(cols.get(0).pgType()).isEqualTo("TEXT"); // NOT DATE — supports partial dates
        assertThat(cols.get(1).name()).isEqualTo("birth_date_magnitude");
        assertThat(cols.get(1).pgType()).isEqualTo("DOUBLE PRECISION");
    }

    @Test
    void dvTime_mapsToTextAndMagnitude() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_TIME", "onset_time");
        assertThat(cols).hasSize(2);
        assertThat(cols.get(0).pgType()).isEqualTo("TEXT"); // NOT TIME — supports partial times
        assertThat(cols.get(1).pgType()).isEqualTo("DOUBLE PRECISION");
    }

    @Test
    void dvDateTime_mapsToTextAndMagnitude() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_DATE_TIME", "event_time");
        assertThat(cols).hasSize(2);
        assertThat(cols.get(0).pgType()).isEqualTo("TEXT"); // NOT TIMESTAMPTZ — supports partial
        assertThat(cols.get(1).pgType()).isEqualTo("DOUBLE PRECISION");
    }

    @Test
    void dvDuration_mapsToTextAndMagnitude() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_DURATION", "duration");
        assertThat(cols).hasSize(2);
        assertThat(cols.get(0).pgType()).isEqualTo("TEXT"); // NOT INTERVAL — ISO 8601 string
        assertThat(cols.get(1).pgType()).isEqualTo("DOUBLE PRECISION");
    }

    @Test
    void dvIdentifier_mapsToFourTextColumns() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_IDENTIFIER", "patient_id");
        assertThat(cols).hasSize(4);
    }

    @Test
    void dvMultimedia_mapsToUriAndMediaType() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_MULTIMEDIA", "image");
        assertThat(cols).hasSize(2);
        assertThat(cols.get(0).name()).isEqualTo("image_uri");
        assertThat(cols.get(1).name()).isEqualTo("image_media_type");
    }

    @Test
    void dvParsable_mapsToValueAndFormalism() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_PARSABLE", "formula");
        assertThat(cols).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DV_URI", "DV_EHR_URI"})
    void dvUri_mapsToText(String rmType) {
        List<ColumnDescriptor> cols = TypeMapper.map(rmType, "link");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().pgType()).isEqualTo("TEXT");
    }

    @Test
    void dvState_mapsToJsonb() {
        List<ColumnDescriptor> cols = TypeMapper.map("DV_STATE", "state");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().pgType()).isEqualTo("JSONB");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DV_PERIODIC_TIME_SPECIFICATION", "DV_GENERAL_TIME_SPECIFICATION"})
    void timeSpecification_mapsToJsonb(String rmType) {
        List<ColumnDescriptor> cols = TypeMapper.map(rmType, "schedule");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().pgType()).isEqualTo("JSONB");
    }

    @Test
    void unknownType_fallsBackToJsonb() {
        List<ColumnDescriptor> cols = TypeMapper.map("SOME_FUTURE_TYPE", "data");
        assertThat(cols).hasSize(1);
        assertThat(cols.getFirst().pgType()).isEqualTo("JSONB");
    }
}
