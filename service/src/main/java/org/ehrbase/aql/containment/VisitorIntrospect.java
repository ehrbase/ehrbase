package org.ehrbase.aql.containment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.opt.OptVisitor;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.Map;
import java.util.Optional;

public class VisitorIntrospect {

    I_KnowledgeCache knowledgeCache;

    public VisitorIntrospect(I_KnowledgeCache knowledgeCache) {
        this.knowledgeCache = knowledgeCache;
    }

    public Map<String, Object> represent(String templateId) throws IllegalStateException {

        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledgeCache.retrieveOperationalTemplate(templateId);

        if (operationaltemplate.isPresent()) {
            try {
                Map map = new OptVisitor().traverse(operationaltemplate.get());
                return map;
            }
            catch (Exception e){
                throw new IllegalStateException("Could not generate visitor for template:"+templateId+", error:"+e);
            }
        }
        else
            return null;

    }

    public String representAsString(String templateId) throws IllegalStateException {
        Map introspectMap = represent(templateId);

        if (introspectMap != null)
            return toJson(introspectMap);
        else
            return null;
    }

    private String toJson(Map<String, Object> map) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(map);
    }

    public boolean jsonPathEval(String json, String jsonPathExpression){
        DocumentContext jsonPathContext = JsonPath.parse(json);

        Object result = jsonPathContext.read(JsonPath.compile(jsonPathExpression));

        return result != null;
    }
}
