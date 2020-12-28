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

package org.ehrbase.aql.sql.queryImpl.value_field;

/** Created by christian on 5/2/2018. */
public class NodePredicate {

  private static final String namedItemPrefix = " and name/value='";
  String nodeId;

  public NodePredicate(String nodeId) {
    this.nodeId = nodeId;
  }

  public String removeNameValuePredicate() {

    String retNodeId = nodeId;

    if (retNodeId.contains(namedItemPrefix)) {
      retNodeId = retNodeId.substring(0, retNodeId.indexOf(namedItemPrefix)) + "]";
    } else if (retNodeId.contains(",")) {
      retNodeId = retNodeId.substring(0, retNodeId.indexOf(",")) + "]";
    }

    return retNodeId;
  }

  public String predicate() {
    String predicate = null;

    if (nodeId.contains(namedItemPrefix)) {
      predicate =
          nodeId.substring(
              nodeId.indexOf(namedItemPrefix) + namedItemPrefix.length(), nodeId.indexOf("]"));
    } else if (nodeId.contains(",")) {
      predicate = nodeId.substring(nodeId.indexOf(",") + 1, nodeId.indexOf("]"));
    }

    return predicate;
  }

  public boolean hasPredicate() {

    boolean retval = false;

    if (nodeId.contains(namedItemPrefix) || nodeId.contains(",")) {
      retval = true;
    }

    return retval;
  }
}
