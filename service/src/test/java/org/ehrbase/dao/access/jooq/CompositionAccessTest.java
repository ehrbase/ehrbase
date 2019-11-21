/*
 * Copyright (c) 2019 Vitasystems GmbH,  Hannover Medical School, Luis Marco-Ruiz (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).
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

package org.ehrbase.dao.access.jooq;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DummyDataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheHelper;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;


/***
*@Created by Luis Marco-Ruiz on Apr 21, 2019
*/
public class CompositionAccessTest {

	protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;
    
	@Before
    public  void beforeClass() {
        /*DSLContext*/ 
        context = getMockingContext();

        try {
            testDomainAccess = new DummyDataAccess(context, null, null, KnowledgeCacheHelper.buildServerConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private DSLContext getMockingContext() {
    	// Initialize  data provider 
    	CompAccessTestMockDataProvider provider = new CompAccessTestMockDataProvider();
    	MockConnection connection = new MockConnection(provider);
    	// Pass the mock connection to a jOOQ DSLContext:
    	return DSL.using(connection, SQLDialect.POSTGRES_9_5);
    	
    }
    
	@Test
	public void shouldReturnVersionByTimestamp() throws Exception {
		CompositionAccess compositionAccess = new CompositionAccess(testDomainAccess);	
		//last item in composition_history table "5/7/2019, 9:43:18 AM.75" VERSION 13
		int version1 = CompositionAccess.getVersionFromTimeStamp(compositionAccess, UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), new Timestamp(Long.parseLong("1557222155000")));// : Tuesday, May 7, 2019 11:42:35 AM GMT+2
		assertEquals(13, version1);
				
		//second-to-last item in composition_history table ""2019-05-06 17:47:36""
		int version2 = CompositionAccess.getVersionFromTimeStamp(compositionAccess, UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), new Timestamp(Long.parseLong("1557157680000")));//2019-05-06  5:48:00 PM GMT +2
		assertEquals(11, version2);
		
		//test that the situation where there is no composition with smaller time stamp is managed appropriately throwing an exception 1546344000000 = 1/1/2019, 1:00:00 PM
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(()->CompositionAccess.getVersionFromTimeStamp(compositionAccess, UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), new Timestamp(Long.parseLong("1546344000000")))).withMessageStartingWith("There are no versions available prior to date");

		//test that the one in the composition table is returned when the timestamp is greater than any existing sys_transaction (including the latest one in the Composition table) 2019-05-07 12:41:56.546
		int version4 = CompositionAccess.getVersionFromTimeStamp(compositionAccess, UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), new Timestamp(Long.parseLong("1562673600000")));//Tuesday, July 9, 2019 2:00:00 PM GMT+02:00 DST
		assertEquals(14, version4);
		
		//timestamp null should return latest as per the openehr specs, the one in composition table 2019-05-07 12:41:56.546
		int version5 = CompositionAccess.getVersionFromTimeStamp(compositionAccess, UUID.fromString("8701233c-c8fd-47ba-91b5-ef9ff23c259b"), null);
		assertEquals(14, version5);
	}
	

}
