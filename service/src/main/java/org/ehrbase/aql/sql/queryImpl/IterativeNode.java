/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.ehr.util.LocatableHelper;
import org.ehrbase.service.IntrospectService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 5/9/2018.
 */
public class IterativeNode implements I_IterativeNode {

    private final List<String> ignoreIterativeNode; //f.e. '/content' '/events' etc.
    private final List<Map> unbounded;
    private final int depth;

    public IterativeNode(String templateId, IntrospectService introspectCache, List<String> ignoreIterativeNode, int depth) throws Exception {
        this.ignoreIterativeNode = ignoreIterativeNode;
        unbounded = introspectCache.getQueryOptMetaData(templateId).upperNotBounded();
        this.depth = depth;
    }

    /**
     * check if node at path is iterative (max > 1)
     *
     * @param segmentedPath
     * @return
     */
    public Integer[] iterativeAt(List<String> segmentedPath) throws Exception {

        int marked = 0; //exit loop when counter > depth

        List<Integer> retarray = new ArrayList<>();

        if (unbounded.size() == 0) {
            retarray.add(-1);
        } else {
            String path = "/" + String.join("/", compact(segmentedPath));

            for (int i = unbounded.size() - 1; i >= 0; i--) {
                Map mapEntry = unbounded.get(i);
                String aql_path = (String) mapEntry.get("aql_path");
                //check if this path is not excluded
                List<String> aqlPathSegments = LocatableHelper.dividePathIntoSegments(aql_path);

                boolean ignoreThisAqlPath = false;
                if (ignoreIterativeNode != null && ignoreIterativeNode.size() > 0) {
                    for (String ignoreItemRegex : ignoreIterativeNode) {
                        if (aqlPathSegments.get(aqlPathSegments.size() - 1).matches(ignoreItemRegex)) {
                            ignoreThisAqlPath = true;
                            break;
                        }

                    }
                }

                if (ignoreThisAqlPath)
                    continue;

                if (path.startsWith(aql_path)) {
                    int pos = aqlPathInJsonbArray(aqlPathSegments, segmentedPath);
                    retarray.add(pos);
                    if (++marked >= depth)
                        break;
                }

            }
        }

        retarray.sort(Comparator.<Integer>naturalOrder());

        return retarray.toArray(new Integer[0]);
    }

    public List<String> clipInIterativeMarker(List<String> segmentedPath, Integer[] clipPos) throws Exception {

        List<String> resultingPath = new ArrayList<>();
        resultingPath.addAll(segmentedPath);

        for (Integer pos : clipPos) {
            resultingPath.set(pos, I_QueryImpl.AQL_NODE_ITERATIVE_MARKER);
        }
        return resultingPath;

    }

    /**
     * make the path usable to perform JsonPath queries
     *
     * @param segmentedPath
     * @return
     */
    List<String> compact(List<String> segmentedPath) {
        List<String> resultPath = new ArrayList<>();
        for (String item : segmentedPath) {
            try {
                Integer.parseInt(item);
            } catch (Exception e) {
                //not an index, add into the list
                if (!item.startsWith("/composition")) {
                    if (item.startsWith("/")) {
                        //skip structure containers
                        if (!item.equals("/events") && !item.equals("/activities")) {
                            resultPath.add(item.substring(1));
                        }
                    } else
                        resultPath.add(item);
                }
            }
        }
        return resultPath;
    }

    int aqlPathInJsonbArray(List<String> aqlSegmented, List<String> jsonbSegmented) {
        int retval = 0;
        int aqlSegIndex = 0;

        for (int i = 0; aqlSegIndex < aqlSegmented.size(); i++) {
            if (jsonbSegmented.get(i).startsWith("/composition")) {
                retval++;
                continue;
            }
            try {
                Integer.parseInt(jsonbSegmented.get(i));
                retval++;
                continue;
            } catch (Exception e) {

                if (jsonbSegmented.get(retval).equals("/events") || jsonbSegmented.get(retval).equals("/activities")) {
                    retval++; //skip this structural item
                    continue;
                }

                try {
                    if (jsonbSegmented.get(retval).startsWith("/"))
                        assert jsonbSegmented.get(retval).substring(1).equals(aqlSegmented.get(aqlSegIndex));
                    else
                        assert jsonbSegmented.get(retval).equals(aqlSegmented.get(aqlSegIndex));
                } catch (Exception e1) {
                    throw new IllegalArgumentException("Drift in locating array marker: aql:" + aqlSegmented.get(aqlSegIndex) + ", jsonb:" + jsonbSegmented.get(retval) + ", @index:" + retval);
                }

                retval++;
                aqlSegIndex++;
            }
        }
        return retval;
    }

}
