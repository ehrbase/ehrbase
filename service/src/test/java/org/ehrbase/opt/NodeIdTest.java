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

package org.ehrbase.opt;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Created by christian on 3/8/2018.
 */
@Ignore
public class NodeIdTest extends TestCase {

    @Test
    @Ignore
    public void testFormatting() {

        assertEquals("uri_resource_identifier", new NodeId("URI - resource identifier").ehrscape());
        assertEquals("relationship_role", new NodeId("Relationship/ role").ehrscape());
        assertEquals("state_definition", new NodeId("State - definition").ehrscape());
        assertEquals("date_time", new NodeId("Date/Time").ehrscape());
        assertEquals("heading1", new NodeId("Heading1").ehrscape());
        assertEquals("heading_1", new NodeId("Heading 1").ehrscape());
        assertEquals("slot_to_contain_other_cluster_archetypes", new NodeId("Slot to contain other Cluster archetypes").ehrscape());
        assertEquals("allergies_and_adverse_reactions", new NodeId("Allergies and adverse reactions").ehrscape());

    }

}