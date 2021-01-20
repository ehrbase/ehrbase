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

package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.util.LocatableHelper;
import org.ehrbase.service.IntrospectService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.ehrbase.aql.sql.queryimpl.IterativeNodeConstants.ENV_AQL_ARRAY_DEPTH;
import static org.ehrbase.aql.sql.queryimpl.IterativeNodeConstants.ENV_AQL_ARRAY_IGNORE_NODE;

/**
 * Created by christian on 5/9/2018.
 */
@SuppressWarnings({"java:S3776","java:S3740","java:S1452","java:S1075","java:S135"})
public class IterativeNode implements IIterativeNode {

    private List<String> ignoreIterativeNode; //f.e. '/content' '/events' etc.
    private final List<String> unbounded;
    private Integer depth;
    private final I_DomainAccess domainAccess;

    public IterativeNode(I_DomainAccess domainAccess, String templateId, IntrospectService introspectCache) {
        this.domainAccess = domainAccess;
        unbounded = introspectCache.multiValued(templateId);
        initAqlRuntimeParameters();
    }

    /**
     * check if node at path is iterative (max > 1)
     *
     * @param segmentedPath
     * @return
     */
    public Integer[] iterativeAt(List<String> segmentedPath) {

        SortedSet<Integer> retarray = new TreeSet<>();

        if (unbounded.isEmpty()) {
            retarray.add(-1);
        } else {
            String path = "/" + String.join("/", compact(segmentedPath));

            for (int i = unbounded.size() - 1; i >= 0; i--) {
                String aqlPath = unbounded.get(i);

                //check if this path is not excluded
                List<String> aqlPathSegments = LocatableHelper.dividePathIntoSegments(aqlPath);

                boolean ignoreThisAqlPath = false;
                if (ignoreIterativeNode != null && !ignoreIterativeNode.isEmpty()) {
                    for (String ignoreItemRegex : ignoreIterativeNode) {
                        if (aqlPathSegments.get(aqlPathSegments.size() - 1).matches("^" + ignoreItemRegex + ".*")) {
                            ignoreThisAqlPath = true;
                            break;
                        }

                    }
                }

                if (ignoreThisAqlPath)
                    continue;

                if (path.startsWith(aqlPath)) {
                    int pos = aqlPathInJsonbArray(aqlPathSegments, segmentedPath);
                    retarray.add(pos);
                    if (retarray.size() >= depth)
                        break;
                }

            }
        }


        return retarray.toArray(new Integer[0]);
    }

    public List<String> clipInIterativeMarker(List<String> segmentedPath, Integer[] clipPos) {

        List<String> resultingPath = new ArrayList<>();
        resultingPath.addAll(segmentedPath);

        for (Integer pos : clipPos) {
            resultingPath.set(pos, QueryImplConstants.AQL_NODE_ITERATIVE_MARKER);
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
                        //skip structure containers that are specific to DB encoding (that is: /events/events[openEHR...])
                        //this also applies to /activities
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

    private void initAqlRuntimeParameters(){
        ignoreIterativeNode = new ArrayList<>();
        if (System.getenv(ENV_AQL_ARRAY_IGNORE_NODE) != null) {
            ignoreIterativeNode = Arrays.asList(System.getenv(ENV_AQL_ARRAY_IGNORE_NODE).split(","));
        } else if (domainAccess.getServerConfig().getAqlIterationSkipList() != null && !domainAccess.getServerConfig().getAqlIterationSkipList().isBlank()){
            Arrays.asList(domainAccess.getServerConfig().getAqlIterationSkipList().split(",")).stream().forEach(ignoreIterativeNode::add);
        }
        else
            ignoreIterativeNode = Arrays.asList("^/content.*", "^/events.*");

        if (System.getenv(ENV_AQL_ARRAY_DEPTH) != null) {
            depth = Integer.parseInt(System.getenv(ENV_AQL_ARRAY_DEPTH));
        }
        else if (domainAccess.getServerConfig().getAqlDepth() != null)
            depth = domainAccess.getServerConfig().getAqlDepth();
        else
            depth = 1;
    }

}
