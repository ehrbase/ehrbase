package org.ehrbase.api.definitions;

public interface FhirTsProps {
	public void setCodePath(String codePath);
	
	public String getSystemPath();
	
	public void setSystemPath(String systemPath);
	
	public String getDisplayPath();
	
	public void setDisplayPath(String displayPath);
	
	public String getValidationResultPath();
	
	public void setValidationResultPath(String validationResultPath);
}