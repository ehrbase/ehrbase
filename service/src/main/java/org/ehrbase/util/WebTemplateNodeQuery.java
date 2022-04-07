package org.ehrbase.util;

import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.model.WebTemplateNode;
import org.ehrbase.webtemplate.parser.AqlPath;

import java.util.ArrayDeque;
import java.util.Deque;

public class WebTemplateNodeQuery {
    
    private final WebTemplateNode webTemplateNode;
    private final WebTemplate webTemplate;
    private final StringBuilder pathBuilder = new StringBuilder();

    public WebTemplateNodeQuery(WebTemplate webTemplate, WebTemplateNode webTemplateNode) {
        this.webTemplate = webTemplate;
        this.webTemplateNode = webTemplateNode;
    }
    
    public boolean requiresNamePredicate(){
        for (AqlPath.AqlNode aqlNode: webTemplateNode.getAqlPathDto().getNodes()){
            pathBuilder.append("/");
            pathBuilder.append(aqlNode.getName());
            pathBuilder.append("[");
            pathBuilder.append(aqlNode.getAtCode());
            pathBuilder.append("]");

            if (webTemplate.findAllByAqlPath(pathBuilder.toString(), true).size() > 1)
                return true;
        }
        return false;
    }
}
