package org.ehrbase.aql.sql.queryimpl;

import org.jooq.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ehrbase.jooq.pg.Tables.ENTRY;


public class ActiveTemplates {

    private Set<String> templates = new HashSet<>();
    private final DSLContext context;
    private boolean isInitialized = false; //to allow tests going through!

    public ActiveTemplates(DSLContext context){
        this.context = context;
    }

    public void init(){
        templates = context.selectDistinct(ENTRY.TEMPLATE_ID).from(ENTRY).fetch().stream().map(Record1::value1).collect(Collectors.toSet());
        isInitialized = true;
    }

    public boolean isActive(String templateId){
        return templates.contains(templateId);
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
