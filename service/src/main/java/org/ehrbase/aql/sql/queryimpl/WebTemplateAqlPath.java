package org.ehrbase.aql.sql.queryimpl;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.util.rmconstants.RmConstants;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.model.WebTemplateInput;
import org.ehrbase.webtemplate.model.WebTemplateNode;

import java.util.Optional;

import static org.ehrbase.aql.sql.queryimpl.EntryAttributeMapper.OTHER_PARTICIPATIONS;
import static org.ehrbase.aql.sql.queryimpl.NormalizedRmAttributePath.OTHER_CONTEXT;
import static org.ehrbase.aql.sql.queryimpl.NormalizedRmAttributePath.OTHER_DETAILS;
import static org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath.CONTENT;

public final class WebTemplateAqlPath {

    private static final String TERMINAL_NODE_PATH_PART = "]/";

    private WebTemplateAqlPath() {
        //NOOP
    }

    public static boolean isValid(WebTemplate webTemplate, String containerPart, String variablePart) {

        //TODO: WebTemplate doesn't provide details when this ITEM_STRUCTURE is passed in /participations
        if (containerPart.contains(OTHER_PARTICIPATIONS)) {
            return true;
        }

        var nodeAttributrePair = identifyPathParts(variablePart);
        String nodePart = nodeAttributrePair.getLeft();
        String attributePart = nodeAttributrePair.getRight();

        String pathToCheck = containerPart + nodePart;
        //TODO: Simple contains check?
        if (pathToCheck.contains(OTHER_CONTEXT) || pathToCheck.contains(OTHER_DETAILS)) {
            return true; //TODO: ignore basically since the ITEM_STRUCTURE is part of an attribute not reflected in WebTemplate
        }

        Optional<WebTemplateNode> webTemplateNode = webTemplate.findByAqlPath(pathToCheck);

        if (webTemplateNode.isEmpty() && !isMissingAttributeFromWebTemplate(webTemplate, pathToCheck)) {
            //TODO: check if this is an attribute for an ELEMENT see CR #...
            return false;
        }

        if (attributePart == null || !attributePart.startsWith("value")) {
            return true;
        }

        String[] attributePartBits = attributePart.split("value/");
        if (attributePartBits.length == 1) {
            //value not followed by an attribute
            return true;
        }

        // check if the remainder is an input
        return webTemplateNode.stream()
            .map(WebTemplateNode::getInputs)
            .flatMap(Collection::stream)
            .map(WebTemplateInput::getSuffix)
            .anyMatch(attributePartBits[1]::equals);
    }

    /**
     *
     * @param path
     * @return Pair<nodePart, attributrePath>
     */
    private static Pair<String, String> identifyPathParts(String path) {
        final String nodePart;
        final String attributePart;

        if (StringUtils.isEmpty(path)) {
            attributePart = null;
            nodePart = "";

        } else if (path.startsWith(CONTENT)) {
            attributePart = "";
            nodePart = "";

        } else if (path.contains(TERMINAL_NODE_PATH_PART)) {
            int lastNodeSegmentIndex = path.lastIndexOf(TERMINAL_NODE_PATH_PART);
            attributePart = path.substring(lastNodeSegmentIndex + 2).split("/")[0];
            nodePart = "/" + path.substring(0, lastNodeSegmentIndex + 1) + "/" + attributePart;

        } else if (path.contains("/")) {
            attributePart = path.split("/")[0];
            nodePart = attributePart.isEmpty() ? "" : "/" + attributePart;

        } else {
            attributePart = "";
            nodePart = "";
        }
        return Pair.of(nodePart, attributePart);
    }

    /**
     * check whether this path relates to missing attributes in WebTemplate (many Locatable attributes are not reflected)
     * @param pathToCheck
     * @return
     */
    private static boolean isMissingAttributeFromWebTemplate(WebTemplate webTemplate, String pathToCheck){
        if (pathToCheck.lastIndexOf(TERMINAL_NODE_PATH_PART) <= 0) {
            return false;
        }
        int lastNodeSegmentIndex = pathToCheck.lastIndexOf(TERMINAL_NODE_PATH_PART);

        //check if we have an ELEMENT
        return webTemplate.findByAqlPath(pathToCheck.substring(0, lastNodeSegmentIndex + 1))
        .filter(n -> n.getRmType().equals(RmConstants.ELEMENT))
        .isPresent();
    }
}
