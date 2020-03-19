package org.ehrbase.dao.access.jooq.rmdatavalue;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;

public class JooqDvCodedText {

    DvCodedTextRecord dvCodedTextRecord;

    public JooqDvCodedText(DvCodedTextRecord dvCodedTextRecord) {
        this.dvCodedTextRecord = dvCodedTextRecord;
    }

    public DvCodedText toRmInstance(){
        return new DvCodedText(
                dvCodedTextRecord.getValue(),
                new JooqCodePhrase(dvCodedTextRecord.getDefiningCode()).toRmInstance()
        );
    }
}
