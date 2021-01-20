package org.ehrbase.aql.sql.queryimpl.value_field;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.RMObjectAttribute;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.toJson;

/**
 * use to format a result using a function (f.e. to generate a correct ISO date/time
 */
@SuppressWarnings({"java:S3776","java:S3740"})
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
        Field formattedField = DSL.field(
                    toJson(fieldContext.getContext().configuration(),
                            DSL.field(StringUtils.join(tableFields, separator)).cast(DSL.val(sqlType)).toString())
        );

        return as(DSL.field(formattedField));
    }


    @Override
    public Field sqlField() {
        return null;
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
