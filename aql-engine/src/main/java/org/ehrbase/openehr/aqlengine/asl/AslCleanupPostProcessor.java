package org.ehrbase.openehr.aqlengine.asl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslFilteringQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslPathDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRmObjectDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.jooq.TableField;

public class AslCleanupPostProcessor implements AslPostProcessor {

    @Override
    public void afterBuildAsl(final AslRootQuery aslRootQuery, final AqlQuery aqlQuery, final AqlQueryWrapper aqlQueryWrapper, final AqlQueryRequest aqlQueryRequest) {
        findUsedFields(aslRootQuery, new HashSet<>());
    }

    private static void findUsedFields(AslQuery q, Map<AslQuery, Set<String>> usedFields){
        switch (q){
            case AslPathDataQuery aslDataQuery -> {
            }
            case AslRootQuery rq -> {
            }
            case AslFilteringQuery fq -> usedFields.compute(fq.getSourceField().getOwner(),(k, v) -> {
                Set<String> names = v != null ? v : new HashSet<>();
                getFieldNames(fq.getSourceField()).forEach(names::add);
                return names;
            });
            case AslStructureQuery __ -> {
            }
            case AslRmObjectDataQuery rodq -> throw new IllegalArgumentException("unexpected AslRmObjectDataQuery");
            }
            case AslEncapsulatingQuery aslEncapsulatingQuery -> {
            }
        }
    }

    private static Stream<String> getFieldNames(final AslField field) {
        return switch (field) {
            case AslColumnField cf -> Stream.of(cf.getColumnName());
            case AslRmPathField pf -> Stream.of(pf.getSrcField().getColumnName());
            case AslConstantField __ -> Stream.empty();
            case AslAggregatingField af -> getFieldNames(af.getBaseField());
            case AslComplexExtractedColumnField ecf -> ecf.getExtractedColumn().getColumns().stream();
            case AslSubqueryField sqf ->
                    Stream.concat(AslUtils.getTargetType(sqf.getBaseQuery()).getPkeyFields().stream().map(TableField::getName),
                            Stream.of(AslStructureColumn.NUM.getFieldName(), AslStructureColumn.NUM_CAP.getFieldName()));
            case AslFolderItemIdVirtualField fidf -> Stream.of(fidf.getFieldName());
        };
    }

    private static void cleanupSelect(AslStructureQuery sq, Set<String> usedFieldNames){
        sq.getSelect().removeIf(f -> f instanceof AslColumnField cf && !usedFieldNames.contains(cf.getColumnName()));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
