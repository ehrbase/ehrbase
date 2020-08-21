package org.ehrbase.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author luis
 */
@Configuration
@ConfigurationProperties(prefix = "terminology-server")
public class FhirTsProps {
	private String codePath = "$[\"expansion\"][\"contains\"][*][\"code\"]";
	private String systemPath = "$[\"expansion\"][\"contains\"][*][\"system\"]";
	private String displayPath = "$[\"expansion\"][\"contains\"][*][\"display\"]";
	private String tsUrl = "https://r4.ontoserver.csiro.au/fhir/";
	private String validationResultPath = "$.parameter[:1].valueBoolean";

	public String getValidationResultPath() {
		return validationResultPath;
	}

	public void setValidationResultPath(String validationResultPath) {
		this.validationResultPath = validationResultPath;
	}
	public String getTsUrl() {
		return tsUrl;
	}
	public void setTsUrl(String tsUrl) {
		this.tsUrl = tsUrl;
	}
	public String getCodePath() {
		return codePath;
	}
	public void setCodePath(String codePath) {
		this.codePath = codePath;
	}
	public String getSystemPath() {
		return systemPath;
	}
	public void setSystemPath(String systemPath) {
		this.systemPath = systemPath;
	}
	public String getDisplayPath() {
		return displayPath;
	}
	public void setDisplayPath(String displayPath) {
		this.displayPath = displayPath;
	}
}
