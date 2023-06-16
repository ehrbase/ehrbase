package org.ehrbase.api.audit.msg;

import java.util.Set;

public class AuditMsg {
    private final String location;
    private final Object[] ehrIds;
    private final Integer version;
    private final String query;
    private final String queryId;
    private final String compositionId;
    private final String templateId;
    private final String contributionId;
    private final Set<String> removedPatients;

    private AuditMsg(Builder builder) {
        this.location = builder.location;
        this.ehrIds = builder.ehrIds;
        this.version = builder.version;
        this.query = builder.query;
        this.queryId = builder.queryId;
        this.compositionId = builder.compositionId;
        this.templateId = builder.templateId;
        this.contributionId = builder.contributionId;
        this.removedPatients = builder.removedPatients;
    }

    public String getLocation() {
        return location;
    }

    public Object[] getEhrIds() {
        return ehrIds;
    }

    public Integer getVersion() {
        return version;
    }

    public String getQuery() {
        return query;
    }

    public String getQueryId() {
        return queryId;
    }

    public String getCompositionId() {
        return compositionId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getContributionId() {
        return contributionId;
    }

    public Set<String> getRemovedPatients() {
        return removedPatients;
    }

    static class Builder {
        private String location;
        private Object[] ehrIds;
        private Integer version;
        private String query;
        private String queryId;
        private String compositionId;
        private String templateId;
        private String contributionId;
        private Set<String> removedPatients;

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder ehrIds(Object... ehrIds) {
            this.ehrIds = ehrIds;
            return this;
        }

        public Builder version(Integer version) {
            this.version = version;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder queryId(String queryId) {
            this.queryId = queryId;
            return this;
        }

        public Builder compositionId(String compositionId) {
            this.compositionId = compositionId;
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder contributionId(String contributionId) {
            this.contributionId = contributionId;
            return this;
        }

        public Builder removedPatients(Set<String> removedPatients) {
            this.removedPatients = removedPatients;
            return this;
        }

        public AuditMsg build() {
            return new AuditMsg(this);
        }
    }
}
