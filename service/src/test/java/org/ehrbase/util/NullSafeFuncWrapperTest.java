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
package org.ehrbase.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NullSafeFuncWrapperTest {

    @SuppressWarnings("unchecked")
    private final Function<String, Map<String, Object>> toMap = f -> {
        try {
            return (Map<String, Object>) new ObjectMapper().readValue(f, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    };

    private static final String SINGLE_IN =
            """
	{
	"salami": "no",
	"i": [
		{
	        "T": "OR",
	        "X": {
	          "T": "HX",
	          "V": "5dbae2b6-bfad-41d5-b0a9-cd8a409dd807"
	        },
	        "ns": "my.system.id",
	        "tp": "VERSIONED_COMPOSITION"
	    }
	]
    }
	""";

    @Test
    @SuppressWarnings("unchecked")
    void single() {
        NullSafeFuncWrapper<String, List<String>> func = NullSafeFuncWrapper.of(toMap)
                .after(m -> (List<Map<String, Object>>) m.get("i"))
                .after(l ->
                        l.stream().map(s -> (Map<String, Object>) s.get("X")).toList())
                .after(l -> l.stream().map(s -> (String) s.get("V")).toList());

        List<String> extr1 = func.apply(SINGLE_IN);
        Assertions.assertTrue(null != extr1);
        Assertions.assertTrue(1 == extr1.size());
        Assertions.assertTrue(StringUtils.isNotEmpty(extr1.get(0)));
    }

    private static final String MULTI_IN =
            """
	{
	"salami": "no",
	"i": [
		{
	        "T": "OR",
	        "X": {
	          "T": "HX",
	          "V": "22222222-bfad-41d5-b0a9-cd8a409dd807"
	        },
	        "ns": "my.system.id",
	        "tp": "VERSIONED_COMPOSITION"
	    },
		{
	        "T": "OR",
	        "X": {
	          "T": "HX",
	          "V": "11111111-bfad-41d5-b0a9-cd8a409dd807"
	        },
	        "ns": "my.system.id",
	        "tp": "VERSIONED_COMPOSITION"
	    }
	]
    }
	""";

    @Test
    @SuppressWarnings("unchecked")
    void multi() {
        NullSafeFuncWrapper<String, List<String>> func = NullSafeFuncWrapper.of(toMap)
                .after(m -> (List<Map<String, Object>>) m.get("i"))
                .after(l ->
                        l.stream().map(s -> (Map<String, Object>) s.get("X")).toList())
                .after(l -> l.stream().map(s -> (String) s.get("V")).toList());

        List<String> extr1 = func.apply(MULTI_IN);
        Assertions.assertTrue(null != extr1);
        Assertions.assertTrue(2 == extr1.size());
    }

    private static final String NO_V =
            """
	{
	"salami": "no",
	"i": [
		{
	        "T": "OR",
	        "X": {
	          "T": "HX"
	        },
	        "ns": "my.system.id",
	        "tp": "VERSIONED_COMPOSITION"
	    }
	]
    }
	""";

    @Test
    @SuppressWarnings("unchecked")
    void noValue() {
        NullSafeFuncWrapper<String, List<String>> func = NullSafeFuncWrapper.of(toMap)
                .after(m -> (List<Map<String, Object>>) m.get("i"))
                .after(l ->
                        l.stream().map(s -> (Map<String, Object>) s.get("X")).toList())
                .after(l -> l.stream().map(s -> (String) s.get("V")).toList())
                .after(l -> l.stream().filter(s -> null != s).toList());

        List<String> extr1 = func.apply(NO_V);
        Assertions.assertTrue(null != extr1);
        Assertions.assertTrue(0 == extr1.size());
    }

    private static final String NO_I = """
	{
	"salami": "no",
	"tofu": "yes"
    }
	""";

    @Test
    @SuppressWarnings("unchecked")
    void noI() {
        NullSafeFuncWrapper<String, List<String>> func = NullSafeFuncWrapper.of(toMap)
                .after(m -> (List<Map<String, Object>>) m.get("i"))
                .after(l ->
                        l.stream().map(s -> (Map<String, Object>) s.get("X")).toList())
                .after(l -> l.stream().map(s -> (String) s.get("V")).toList())
                .after(l -> l.stream().filter(s -> null != s).toList());

        List<String> extr1 = func.apply(NO_I);
        Assertions.assertTrue(null == extr1);
    }
}
