package org.ehrbase.aql.containment;

import java.util.HashSet;
import java.util.Set;

public abstract class ContainsCheck {
    String label;
    String checkExpression;
    Set<String> templateIds = new HashSet<>(); //list of template_ids satisfying the proposition

    public ContainsCheck(String label) {
        this.label = label;
    }

    public void addTemplateId(String templateId){
        templateIds.add(templateId);
    }

    public abstract String getSymbol();

    public Set<String> addAllTemplateIds(Set<String> templates){
        this.templateIds.addAll(templates);
        return templateIds;
    }

    public Set<String> getTemplateIds(){
        return templateIds;
    }
}
