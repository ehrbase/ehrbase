package org.ehrbase.aql.sql.queryImpl.value_field;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.sql.queryImpl.attribute.*;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.util.Optional;


public class GenericJsonField extends RMObjectAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    private boolean isJsonDataBlock = true; //by default, can be overriden

    public GenericJsonField(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    public Field jsonField(String rmType, String plpgsqlFunction, TableField... tableFields){
        fieldContext.setJsonDatablock(isJsonDataBlock);
        fieldContext.setRmType(rmType);
        //query the json representation of a node and cast the result as TEXT
        Field jsonContextField;
        if (plpgsqlFunction != null)
            jsonContextField = makeFunctionField(plpgsqlFunction, tableFields);
        else
            jsonContextField = makeFieldToJsonbColumn(tableFields);

        return as(DSL.field(jsonContextField));
    }

    public Field jsonField(String rmType, String plpgsqlFunction, Field... fields){
        fieldContext.setJsonDatablock(isJsonDataBlock);
        fieldContext.setRmType(rmType);
        //query the json representation of a node and cast the result as TEXT
        Field jsonContextField;
        if (plpgsqlFunction != null)
            jsonContextField = makeFunctionField(plpgsqlFunction, fields);
        else
            jsonContextField = makeFieldToJsonbColumn(fields);

        return as(DSL.field(jsonContextField));

    }

    private Field makeFunctionField(String plpgsqlFunction, Field... fields){
        if (jsonPath.isPresent())
            return DSL.field(plpgsqlFunction+"("+StringUtils.join(fields, ",")+")::json #>>"+jsonPath.get());
        else
            return DSL.field(plpgsqlFunction+"("+StringUtils.join(fields, ",")+")::text");
    }

    private Field makeFieldToJsonbColumn(Field... fields){
        if (jsonPath.isPresent())
            return DSL.field("("+StringUtils.join(fields, ",")+")::json #>>"+jsonPath.get());
        else
            return DSL.field("("+StringUtils.join(fields, ",")+")::text");
    }

    @Override
    public Field sqlField(){
        return null;
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public GenericJsonField forJsonPath(String jsonPath){
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }

        this.jsonPath = Optional.of(new GenericJsonPath(jsonPath).jqueryPath());
        return this;
    }

    public GenericJsonField forJsonPath(String root, String jsonPath){
        String actualPath = new AttributePath(root).redux(jsonPath);
        return forJsonPath(actualPath);
    }


    public GenericJsonField setJsonDataBlock(boolean jsonDataBlock) {
        this.isJsonDataBlock = jsonDataBlock;
        return this;
    }
}
