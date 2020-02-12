package org.ehrbase.aql.compiler.tsclient;

import java.util.ArrayList;
import java.util.List;

public class FhirTerminologyServerImpl  implements TerminologyServer<String, String>{

	@Override
	public List expand(String valueSetId) {
		List<String> result = new ArrayList();
		result.add("48377-6");
		result.add("27478-7");
		result.add("52539-9");
		return result;
	}

	@Override
	public Boolean validate(String concept, String valueSetId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SubsumptionResult subsumes(String conceptA, String conceptB) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookUp(String conceptId) {
		// TODO Auto-generated method stub
		return null;
	}

}
