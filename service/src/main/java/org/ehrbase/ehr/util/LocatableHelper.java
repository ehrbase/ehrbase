/*
* Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

* This file is part of Project EHRbase

* Copyright (c) 2015 Christian Chevalley
* This file is part of Project Ethercis
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
package org.ehrbase.ehr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/** ETHERCIS Project ehrservice Created by Christian Chevalley on 8/18/2015. */
public class LocatableHelper {

  public static List<String> dividePathIntoSegments(String path) {
    List<String> segments = new ArrayList<String>();
    StringTokenizer tokens = new StringTokenizer(path, "/");
    while (tokens.hasMoreTokens()) {
      String next = tokens.nextToken();
      if (next.matches(".+\\[.+[^\\]]$")) {
        do {
          next = next + "/" + tokens.nextToken();
        } while (!next.matches(".*]$"));
      }
      segments.add(next);
    }
    return segments;
  }
}
