package org.ehrbase.api.audit.msg;

import java.util.Set;

public class AuditMsgBuilder {
    private String location;
    private Object[] ehrIds;
    private Integer version;
    private String query;
    private String queryId;
    private String compositionId;
    private String templateId;
    private String contributionId;
    private Set<String> removedPatients;
    private static final ThreadLocal<AuditMsgBuilder> auditMsgTL =
            ThreadLocal.withInitial(AuditMsgBuilder::new);


    public static AuditMsgBuilder getInstance() {
        return auditMsgTL.get();
    }

    public static void removeInstance() {
        auditMsgTL.remove();
    }

    public AuditMsgBuilder setEhrIds(Object... ehrIds) {
        this.ehrIds = ehrIds;
        return this;
    }

    public AuditMsgBuilder setRemovedPatients(Set<String> removedPatients) {
        this.removedPatients = removedPatients;
        return this;
    }

    public AuditMsgBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    public AuditMsgBuilder setQueryId(String queryId) {
        this.queryId = queryId;
        return this;
    }

    public AuditMsgBuilder setVersion(int version) {
        this.version = version;
        return this;
    }

    public AuditMsgBuilder setLocation(String location) {
        this.location = location;
        return this;
    }

    public AuditMsgBuilder setTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }

    public AuditMsgBuilder setCompositionId(String compositionId) {
        this.compositionId = compositionId;
        return this;
    }

    public AuditMsgBuilder setContributionId(String contributionId) {
        this.contributionId = contributionId;
        return this;
    }

    public void clean() {
        this.setEhrIds();
        this.setCompositionId(null);
        this.setLocation(null);
        this.setQuery(null);
        this.setQueryId(null);
        this.setTemplateId(null);
        this.setContributionId(null);
        this.setVersion(0);
        this.setRemovedPatients(null);
    }

    public AuditMsg build() {
        return new AuditMsg.Builder()
                .location(this.location)
                .ehrIds(this.ehrIds)
                .version(this.version)
                .query(this.query)
                .queryId(this.queryId)
                .compositionId(this.compositionId)
                .templateId(this.templateId)
                .contributionId(this.contributionId)
                .removedPatients(this.removedPatients)
                .build();
    }
}
