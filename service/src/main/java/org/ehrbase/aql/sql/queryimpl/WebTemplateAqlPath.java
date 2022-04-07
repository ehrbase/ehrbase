package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.model.WebTemplateInput;
import org.ehrbase.webtemplate.model.WebTemplateNode;

import java.util.Optional;

import static org.ehrbase.aql.sql.queryimpl.EntryAttributeMapper.OTHER_PARTICIPATIONS;
import static org.ehrbase.aql.sql.queryimpl.NormalizedRmAttributePath.OTHER_CONTEXT;
import static org.ehrbase.aql.sql.queryimpl.NormalizedRmAttributePath.OTHER_DETAILS;
import static org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath.CONTENT;

public class WebTemplateAqlPath {

    final WebTemplate webTemplate;
    final String containerPart;
    final String variablePart;

    private String attributePart;
    private String nodePart;

    final static String terminalNodePathPart = "]/";

    public WebTemplateAqlPath(WebTemplate webTemplate, String containerPart, String variablePart) {
        this.webTemplate = webTemplate;
        this.containerPart = containerPart;
        this.variablePart = variablePart;
    }

    public boolean isValid(){

        //TODO: WebTemplate doesn't provide details when this ITEM_STRUCTURE is passed in /participations
        if (containerPart.contains(OTHER_PARTICIPATIONS))
            return true;

        identifyPathParts(variablePart);

        String pathToCheck = containerPart+nodePart;

        if (pathToCheck.contains(OTHER_CONTEXT) || pathToCheck.contains(OTHER_DETAILS))
            return true; //TODO: ignore basically since the ITEM_STRUCTURE is part of an attribute not reflected in WebTemplate

        Optional<WebTemplateNode> webTemplateNode = webTemplate.findByAqlPath(pathToCheck);

        if (webTemplateNode.isEmpty() && !isMissingAttributeFromWebTemplate(pathToCheck)) {
            //TODO: check if this is an attribute for an ELEMENT see CR #...
            return false;
        }

        if (attributePart != null && attributePart.startsWith("value")) {
            String[] attributePartBits = attributePart.split("value/");
            if (attributePartBits.length == 1) //value not followed by an attribute
                return true;

            //check if the remainder is an input
            if (webTemplateNode.isPresent()) {
                for (WebTemplateInput webTemplateInput : webTemplateNode.get().getInputs()) {
                    if (attributePartBits[1].equals(webTemplateInput.getSuffix()))
                        return true;
                }
            }
            return false;
        }

        return true;
    }

    private void identifyPathParts(String path){

        if (path == null) {
            nodePart = "";
            return;
        }

        if (variablePart.startsWith(CONTENT)) {
            nodePart = "";
            attributePart = "";
            return;
        }


        if (!path.contains(terminalNodePathPart)) {
            if (!path.isEmpty())
                if (path.contains("/"))
                    attributePart = path.split("/")[0];
                else
                    attributePart = "";

            nodePart = (attributePart.isEmpty() ? "" : "/"+attributePart);

            return;
        }

        int lastNodeSegmentIndex = path.lastIndexOf(terminalNodePathPart);

        attributePart = path.substring(lastNodeSegmentIndex+2).split("/")[0];

        nodePart = "/"+path.substring(0, lastNodeSegmentIndex+1)+"/"+attributePart;
    }

    /**
     * check whether this path relates to missing attributes in WebTemplate (many Locatable attributes are not reflected)
     * @param pathToCheck
     * @return
     */
    private boolean isMissingAttributeFromWebTemplate(String pathToCheck){
        if (!(pathToCheck.lastIndexOf(terminalNodePathPart) > 0))
            return false;

        int lastNodeSegmentIndex = pathToCheck.lastIndexOf(terminalNodePathPart);

        //check if we have an ELEMENT
        Optional<WebTemplateNode> webTemplateNode = webTemplate.findByAqlPath(pathToCheck.substring(0, lastNodeSegmentIndex+1));

        if (webTemplateNode.isEmpty())
            return false;

        return webTemplateNode.get().getRmType().equals("ELEMENT");
    }
}
