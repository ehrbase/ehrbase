package org.ehrbase.rest.openehr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component("requestAwareAuditResultMapHolder")
public class RequestAwareAuditResultMapHolder {
  private Map<String, Set<Object>> auditResultMap = new HashMap<>();
  
  public void setAuditResultMap(Map<String, Set<Object>> map) {
    if(map == null) {
      auditResultMap = map;
      return;
    }
    
    auditResultMap.clear();
    auditResultMap.putAll(map);
  }
  
  public Map<String, Set<Object>> getAuditResultMap() {
    return auditResultMap;
  }
}
