package org.ehrbase.service;

import org.ehrbase.api.definitions.FhirTsProps;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
/**
 * 
 * @author luis
 *
 */
//@Component
@Configuration
@ConfigurationProperties(prefix="terminology_server")
public class FhirTsPropsImpl implements FhirTsProps{
	private String codePath = "default";// = "$[\"expansion\"][\"contains\"][*][\"code\"]";
	private String systemPath = "default";// = "$[\"expansion\"][\"contains\"][*][\"system\"]";
	private String displayPath = "default";// = "$[\"expansion\"][\"contains\"][*][\"display\"]";
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
