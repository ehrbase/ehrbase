package org.ehrbase.aql.compiler.tsclient;

import java.util.List;

public interface TerminologyServer<T, ID> {

	List<T> expand(ID valueSetId);
	
	T lookUp(ID conceptId);
	
	Boolean validate(T concept, ID valueSetId);
	
	SubsumptionResult subsumes(T conceptA, T conceptB);
	
	public enum SubsumptionResult{
		
		EQUIVALENT, SUBSUMES, SUBSUMEDBY, NOTSUBSUMED;
	}
	
}
