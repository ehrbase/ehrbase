package org.ehrbase.aql.sql.queryImpl.value_field;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryImpl.attribute.RMObjectAttribute;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;


public class GenericJsonField extends RMObjectAttribute {

    public GenericJsonField(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    public Field jsonField(String plpgsqlFunction, TableField... tableFields){
        //query the json representation of a node and cast the result as TEXT
        Field jsonContextField = DSL.field(plpgsqlFunction+"("+StringUtils.join(tableFields, ",")+")::TEXT");
        if (fieldContext.isWithAlias())
            return aliased(DSL.field(jsonContextField));
        else
            return DSL.field(jsonContextField);
    }

    @Override
    public Field sqlField(){
        return null;
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
