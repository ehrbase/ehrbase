package org.ehrbase.aql.sql.queryImpl.value_field;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.sql.queryImpl.attribute.*;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.util.Optional;

/**
 * use to format a result using a function (f.e. to generate a correct ISO date/time
 */
@SuppressWarnings("unchecked")
public class FormattedField extends RMObjectAttribute {


    public FormattedField(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    public Field using(String sqlType, String separator, String resultType, String plpgsqlFunction, Field... tableFields) {
        //query the json representation of a node and cast the result as resultType
        Field formattedField = DSL.field(plpgsqlFunction + "((" + StringUtils.join(tableFields, separator) + ")::"+sqlType+")::"+resultType);

        return as(DSL.field(formattedField));
    }

    public Field usingToJson(String sqlType, String separator, Field... tableFields) {
        //query the json representation of a node and cast the result as resultType
        Field formattedField = DSL.field("to_json" + "((" + StringUtils.join(tableFields, separator) + ")::"+sqlType+")"+"#>>'{}'");

        return as(DSL.field(formattedField));
    }


    @Override
    public Field sqlField() {
        return null;
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
