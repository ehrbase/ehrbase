/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School, Luis Marco-Ruiz (Hannover Medical School).
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

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;

import java.sql.SQLException;
import java.sql.Timestamp;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION;

public class CompAccessTestMockDataProvider implements MockDataProvider{

@Override
public MockResult[] execute(MockExecuteContext ctx) throws SQLException {
	
    DSLContext create = DSL.using(SQLDialect.POSTGRES);
	MockResult[] mock = new MockResult[1];
    String sql2 = ctx.sql();

    if (sql2.toUpperCase().startsWith("DROP")) {
        throw new SQLException("Statement not supported: " + sql2);
    }
    else if (sql2.toUpperCase().startsWith("UPDATE")) {
        throw new SQLException("Statement not supported: " + sql2);
    }
    else if (sql2.toUpperCase().startsWith("CREATE")) {
        throw new SQLException("Statement not supported: " + sql2);
    }else if(sql2.toUpperCase().contains("SELECT 1 AS \"ONE\"")) {
    	//irrespectively of the "ehr"."composition_history"."id" always returns "1"    	
    	Field<Integer> c = DSL.count();
    	Result<Record1< Integer>> result2 = create.newResult(c);
    	result2.add(create
                .newRecord(c)
                .values(1));
        mock[0] = new MockResult(1, result2);
        return mock;
        
    }else if(sql2.toUpperCase().startsWith("SELECT MAX(\"EHR\".\"COMPOSITION\".\"SYS_TRANSACTION\") AS \"MOSTRECENTINTABLE\" FROM \"EHR\".\"COMPOSITION\" WHERE \"EHR\".\"COMPOSITION\".\"ID\"")) {
    	System.out.println("SQL2 is: "+sql2.toUpperCase());
    	System.out.println("SELECT MAX  is selected");
    	//Field<Timestamp> c = DSL.count();
    	Result<Record1<Timestamp>> result2 = create.newResult(COMPOSITION.SYS_TRANSACTION);
    	result2.add(create
                .newRecord(COMPOSITION.SYS_TRANSACTION)
                .values(new Timestamp(Long.parseLong("1557225716546"))));//1557225716546, that is  2019-05-07 12:41:56.546
        mock[0] = new MockResult(1, result2);
        return mock;

	}else if(sql2.toUpperCase().startsWith("SELECT COUNT(*) FROM \"EHR\".\"COMPOSITION_HISTORY\" WHERE \"EHR\".\"COMPOSITION_HISTORY\".\"ID\" =")) {
    	Field<Integer> c = DSL.count();
    	Result<Record1< Integer>> result2 = create.newResult(c);
    	result2.add(create
                .newRecord(c)
                .values(13));//composition history contains 13 rows for the id 8701233c-c8fd-47ba-91b5-ef9ff23c259b
        mock[0] = new MockResult(1, result2);
	}
    else if(sql2.toUpperCase().startsWith("SELECT COUNT(*) AS \"COUNTVERSIONINTABLE\" FROM \"EHR\".\"COMPOSITION_HISTORY\" WHERE (\"EHR\".\"COMPOSITION_HISTORY\".\"SYS_TRANSACTION\"")){//7-May-2019 11:42
    	
    	if(((Timestamp)ctx.bindings()[0]).compareTo(new Timestamp(Long.parseLong("1557222155000")))==0) {
    		
    		
        	Field<Integer> c = DSL.count();
        	Result<Record1< Integer>> result2 = create.newResult(c);
        	result2.add(create
                    .newRecord(c)
                    .values(13));
            mock[0] = new MockResult(1, result2);
    	}else if(((Timestamp)ctx.bindings()[0]).compareTo(new Timestamp(Long.parseLong("1557157680000")))==0) {//6-may-2019 17:48
    		
        	Field<Integer> c = DSL.count();
        	Result<Record1< Integer>> result2 = create.newResult(c);
        	result2.add(create
                    .newRecord(c)
                    .values(11));
            mock[0] = new MockResult(1, result2);
    	}else if(((Timestamp)ctx.bindings()[0]).compareTo(new Timestamp(Long.parseLong("1546344000000")))==0) {//1-jan-2019 13:00
    		
        	Field<Integer> c = DSL.count();
        	Result<Record1< Integer>> result2 = create.newResult(c);
        	result2.add(create
                    .newRecord(c)
                    .values(0));
            mock[0] = new MockResult(1, result2);
    	}else if(((Timestamp)ctx.bindings()[0]).compareTo(new Timestamp(Long.parseLong("1562673600000")))==0) {//2019-7-9 14:00   1562673600000
    		
        	Field<Integer> c = DSL.count();
        	Result<Record1< Integer>> result2 = create.newResult(c);
        	result2.add(create
                    .newRecord(c)
                    .values(13));//if a timestamp bigger than any in the composition_history table is received return the max num of rows, 13
            mock[0] = new MockResult(1, result2);
    	}else if(sql2.toUpperCase().startsWith("select row_id, in_contribution, ehr_id, language, territory, composer, sys_transaction from \n" + 
        		"  (select ROW_NUMBER() OVER (ORDER BY sys_transaction ASC ) AS row_id, * from ehr.composition_history WHERE")) {      	
        }else {
        	throw new SQLException("time stamp not mocked, add it for appropiate mocking");
    	}
    }
    
    return mock;
}
}
