package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.LateralJoinDefinition;
import org.ehrbase.aql.sql.queryimpl.MultiFields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MultiFieldsList {

    private final List<MultiFields> multiFieldsArrayList = new ArrayList<>();

    public MultiFieldsList(){}

    public MultiFieldsList(Collection<MultiFields> multiFieldsCollection){
        multiFieldsArrayList.addAll(multiFieldsCollection);
    }

    public void add(MultiFields multiFields){
        multiFieldsArrayList.add(multiFields);
    }

    /**
     * traverse the list of existing definition and identify a lateral join matching this variable path, template and SQL expression
     * @param templateId
     * @param candidateLateralExpression
     * @return
     */
    public LateralJoinDefinition matchingLateralJoin(String templateId, String candidateLateralExpression) {

        for (MultiFields multiFields: multiFieldsArrayList){
            if (!multiFields.getVariableDefinition().isLateralJoinsEmpty(templateId)){
                I_VariableDefinition inListVariableDefinition = multiFields.getVariableDefinition();
                if (inListVariableDefinition.getLastLateralJoin(templateId).getSqlExpression().equals(candidateLateralExpression))
                    return inListVariableDefinition.getLastLateralJoin(templateId);
            }
        }
        return null;
    }

    public Iterator<MultiFields> iterator(){
        return multiFieldsArrayList.iterator();
    }
}
