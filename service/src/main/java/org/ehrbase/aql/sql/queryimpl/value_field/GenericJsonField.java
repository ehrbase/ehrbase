package org.ehrbase.aql.sql.queryimpl.value_field;

import org.ehrbase.aql.sql.queryimpl.Function2;
import org.ehrbase.aql.sql.queryimpl.Function4;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.*;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.*;
import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;
import static org.ehrbase.aql.sql.queryimpl.value_field.Functions.apply;

@SuppressWarnings({"java:S3776","java:S3740"})
public class GenericJsonField extends RMObjectAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    private boolean isJsonDataBlock = true; //by default, can be overriden

    private static final String ITERATIVE_MARKER = "'"+AQL_NODE_ITERATIVE_MARKER+"'";

    public GenericJsonField(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    public Field hierObjectId(Field<UUID> uuidField){
        String rmType = "HIER_OBJECT_ID";
        Function<Field<UUID>, Field<JSON>> function = Routines::jsCanonicalHierObjectId1;
        return jsonField(rmType, function, (TableField)uuidField);
    }

    public Field dvCodedText(Field<DvCodedTextRecord> dvCodedTextRecordTableField){
        String rmType = "DV_CODED_TEXT";
        Function<Field<DvCodedTextRecord>, Field<JSON>> function = Routines::jsDvCodedTextInner1;
        return jsonField(rmType, function, (TableField)dvCodedTextRecordTableField);
    }

    public Field eventContext(Field<UUID> uuidField){
        String rmType = "EVENT_CONTEXT";
        Function<Field<UUID>, Field<JSON>> function = Routines::jsContext;
        return jsonField(rmType, function, (TableField)uuidField);
    }

    public Field participations(Field<UUID> uuidField){
        String rmType = "PARTICIPATION";
        Function<Field<UUID>, Field<JSONB[]>> function = Routines::jsParticipations;
        return jsonField(rmType, function, (TableField)uuidField);
    }

    public Field partyRef(Field<String> namespace, Field<String> type, Field<String> scheme, Field<String> value ){
        String rmType = "PARTY_REF";
        Function4<Field<String>, Field<String>, Field<String>, Field<String>, Field<JSON>> function = Routines::jsPartyRef;
        return jsonField(rmType, function, (TableField)namespace, (TableField)type, (TableField)scheme, (TableField)value);
    }

    public Field dvDateTime(Field<Timestamp> dateTime, Field<String> timeZoneId ){
        String rmType = "DV_DATE_TIME";
        Function2<Field<Timestamp>, Field<String>, Field<JSON>> function = Routines::jsDvDateTime;
        return jsonField(rmType, function, (TableField)dateTime, (TableField)timeZoneId);
    }

    public Field ehrStatus(Field<UUID> uuidField){
        String rmType = null;
        Function<Field<UUID>, Field<JSON>> function = Routines::jsEhrStatus;
        return jsonField(rmType, function, (TableField)uuidField);
    }

    public Field canonicalPartyIdendified(Field<UUID> uuidField){
        String rmType = "PARTY_IDENTIFIED";
        Function<Field<UUID>, Field<JSON>> function = Routines::jsCanonicalPartyIdentified;
        return jsonField(rmType, function, (TableField)uuidField);
    }

    public Field feederAudit(Field<?> feederAudit){
        String rmType = "FEEDER_AUDIT";
        return jsonField(rmType, null, (TableField)feederAudit);
    }

    public Field jsonField(String rmType, Object function, TableField... tableFields){
        fieldContext.setJsonDatablock(isJsonDataBlock);
        fieldContext.setRmType(rmType);
        //query the json representation of a node and cast the result as TEXT
        StringBuilder sqlExpression = new StringBuilder();
        Configuration configuration = fieldContext.getContext().configuration();

        Field jsonField;

        if (jsonPath.isPresent()) {
            List<String> tokenized = Arrays.asList(jsonpathParameters(jsonPath.get()));

            if (tokenized.contains(ITERATIVE_MARKER))
                jsonField = fieldWithJsonArrayIteration(configuration, tokenized, function, tableFields);
            else
                jsonField =
                        DSL.field(jsonpathItemAsText(configuration, DSL.field(apply(function, tableFields).toString()).cast(JSONB.class), tokenized.toArray(new String[]{})));

        } else
            jsonField = DSL.field(apply(function, tableFields).toString()).cast(String.class);

        if (sqlExpression.toString().contains(QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION) && fieldContext.getClause().equals(IQueryImpl.Clause.WHERE))
            jsonField = DSL.field(DSL.select(jsonField));

        return as(DSL.field(jsonField));
    }

    private Field fieldWithJsonArrayIteration(Configuration configuration, List<String> tokenized, Object function, TableField... tableFields){

        String[] prefix = tokenized.subList(0, tokenized.indexOf(ITERATIVE_MARKER)).toArray(new String[]{});
        String[] remaining = tokenized.subList(tokenized.indexOf(ITERATIVE_MARKER)+1, tokenized.size()).toArray(new String[]{});

        //initial
        Field field = jsonpathItem(
                            configuration,
                            DSL.field(apply(function, tableFields).toString()).cast(JSONB.class),
                            prefix
                        );

        while (remaining.length > 0){
            List<String> tokens = Arrays.asList(remaining.clone());
            if (tokens.contains(ITERATIVE_MARKER)) {
                prefix = tokens.subList(0, tokens.indexOf(ITERATIVE_MARKER)).toArray(new String[]{});
                remaining = tokens.subList(tokens.indexOf(ITERATIVE_MARKER) + 1, tokens.size()).toArray(new String[]{});
            }
            else {
                prefix = remaining;
                remaining = new String[]{};
            }

            field = DSL.field(
                        jsonpathItemAsText(
                            configuration,
                            jsonArraySplitElements(configuration, field.cast(JSONB.class)),
                            prefix
                        )
                );
        }

        return field;
    }

    @Override
    public Field sqlField(){
        return null;
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
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
