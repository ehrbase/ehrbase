/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.IterativeNodeConstants.ENV_AQL_ARRAY_DEPTH;
import static org.ehrbase.aql.sql.queryimpl.IterativeNodeConstants.ENV_AQL_ARRAY_IGNORE_NODE;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.util.LocatableHelper;
import org.ehrbase.service.IntrospectService;

/**
 * Created by christian on 5/9/2018.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452", "java:S1075", "java:S135"})
public class IterativeNode implements IIterativeNode {
    public static final String SLASH = "/";
    public static final String ACTIVITIES = "/activities";
    public static final String COMPOSITION = "/composition";
    public static final String EVENTS = "/events";
    public static final String CONTENT = "/content";

    private Optional<Pattern> ignorePattern;
    private final List<String> unbounded;
    private final int depth;
    private final I_DomainAccess domainAccess;

    public IterativeNode(I_DomainAccess domainAccess, String templateId, IntrospectService introspectCache) {
        this.domainAccess = domainAccess;
        unbounded = introspectCache.multiValued(templateId);
        ignorePattern = initIgnorePattern();
        depth = initAqlDepth();
    }

    /**
     * check if node at path is iterative (max > 1)
     *
     * @param segmentedPath
     * @return
     */
    int[] iterativeAt(List<String> segmentedPath) {
        if (unbounded.isEmpty()) {
            return null;
        }

        SortedSet<Integer> retarray = new TreeSet<>();

        for (int i = unbounded.size() - 1; i >= 0; i--) {
            String aqlPath = unbounded.get(i);

            List<String> aqlPathSegments = LocatableHelper.dividePathIntoSegments(aqlPath);

            // check if this path is not excluded
            boolean ignoreThisAqlPath = ignorePattern
                    .map(p -> p.matcher(aqlPathSegments.get(aqlPathSegments.size() - 1)))
                    .map(Matcher::matches)
                    .orElse(false);

            if (ignoreThisAqlPath) {
                continue;
            }

            String path = compact(segmentedPath).stream().collect(Collectors.joining(SLASH, SLASH, ""));

            if (path.startsWith(aqlPath) && !StringUtils.endsWithAny(aqlPath, "value", "name")) {
                int pos = aqlPathInJsonbArray(aqlPathSegments, segmentedPath);
                retarray.add(pos);
                if (retarray.size() >= depth) break;
            }
        }

        return retarray.stream().mapToInt(Integer::intValue).toArray();
    }

    List<String> clipInIterativeMarkers(List<String> segmentedPath, int[] clipPos) {

        List<String> resultingPath = new ArrayList<>(segmentedPath);

        // reverse order so adding entries does not move pending positions
        ArrayUtils.reverse(clipPos);

        Arrays.stream(clipPos).forEach(pos -> {
            if (pos == resultingPath.size()) {
                resultingPath.add(AQL_NODE_ITERATIVE_MARKER);
            } else {
                String segment = resultingPath.get(pos);
                if (segment.equals(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER)) {
                    // skip
                } else if (segment.equals("0")) {
                    // substitution
                    resultingPath.set(pos, AQL_NODE_ITERATIVE_MARKER);
                } else {
                    resultingPath.add(pos, AQL_NODE_ITERATIVE_MARKER);
                }
            }
        });
        return resultingPath;
    }

    public List<String> insertIterativeMarkers(List<String> segmentedPath) {
        int[] pos = iterativeAt(segmentedPath);
        if (pos == null) {
            return null;
        }
        if (ArrayUtils.isEmpty(pos)) {
            return segmentedPath;
        }
        return clipInIterativeMarkers(segmentedPath, pos);
    }

    /**
     * make the path usable to perform JsonPath queries
     *
     * @param segmentedPath
     * @return
     */
    static List<String> compact(List<String> segmentedPath) {
        return segmentedPath.stream()
                .filter(item -> !StringUtils.isNumeric(item))
                .filter(item -> !item.startsWith(COMPOSITION))
                // skip structure containers that are specific to DB encoding (that is:
                // /events/events[openEHR...])
                // this also applies to /activities
                .filter(item -> !Set.of(EVENTS, ACTIVITIES).contains(item))
                .map(item -> StringUtils.removeStart(item, SLASH))
                .collect(Collectors.toList());
    }

    static int aqlPathInJsonbArray(List<String> aqlSegmented, List<String> jsonbSegmented) {
        int jsonbIndex = 0;
        int aqlSegIndex = 0;

        for (; aqlSegIndex < aqlSegmented.size(); jsonbIndex++) {
            String segment = jsonbSegmented.get(jsonbIndex);
            if (segment.startsWith(IterativeNode.COMPOSITION) || StringUtils.isNumeric(segment)) {
                continue;
            }
            if (StringUtils.isNumeric(segment)) {
                continue;
            }
            if (Set.of(EVENTS, ACTIVITIES).contains(segment)) {
                // skip this structural item
                continue;
            }

            try {
                if (segment.startsWith(SLASH)) {
                    assert segment.substring(1).equals(aqlSegmented.get(aqlSegIndex));
                } else {
                    assert segment.equals(aqlSegmented.get(aqlSegIndex));
                }
            } catch (AssertionError e1) {
                throw new AssertionError("Drift in locating array marker: aql:" + aqlSegmented.get(aqlSegIndex)
                        + ", jsonb:" + segment + ", @index:" + jsonbIndex);
            }
            aqlSegIndex++;
        }
        return jsonbIndex;
    }

    private Optional<Pattern> initIgnorePattern() {
        String ignoreIterativeNode = System.getenv(ENV_AQL_ARRAY_IGNORE_NODE);
        if (ignoreIterativeNode == null) {
            ignoreIterativeNode = domainAccess.getServerConfig().getAqlIterationSkipList();
            if (StringUtils.isBlank(ignoreIterativeNode)) {
                ignoreIterativeNode = CONTENT + "," + EVENTS;
            }
        }

        return Optional.ofNullable(ignoreIterativeNode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(s -> s.replace(',', '|'))
                .map(s -> "^(" + s + ").*")
                .map(Pattern::compile);
    }

    private int initAqlDepth() {
        if (System.getenv(ENV_AQL_ARRAY_DEPTH) != null) {
            return Integer.parseInt(System.getenv(ENV_AQL_ARRAY_DEPTH));
        } else if (domainAccess.getServerConfig().getAqlDepth() != null) {
            return domainAccess.getServerConfig().getAqlDepth();
        } else {
            return 1;
        }
    }
}
