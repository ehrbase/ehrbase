package org.ehrbase.dao.access.interfaces;

import java.sql.Timestamp;
import java.util.UUID;

public interface I_Compensatable {
  Timestamp getSysTransaction();
  UUID getContributionId();
  UUID getId();
}
