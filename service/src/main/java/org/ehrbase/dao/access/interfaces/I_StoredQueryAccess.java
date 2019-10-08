package org.ehrbase.dao.access.interfaces;

import java.sql.Timestamp;


public interface I_StoredQueryAccess {
    I_StoredQueryAccess commit(Timestamp transactionTime);

    I_StoredQueryAccess commit();

    Boolean update(Timestamp transactionTime);

    Boolean update(Timestamp transactionTime, boolean force);

    Integer delete();

    String getQualifiedName();

    String getReverseDomainName();

    String getSemanticId();

    String getSemver();

    String getQueryText();

    void setQueryText(String queryText);

    Timestamp getCreationDate();

    String getQueryType();
}
