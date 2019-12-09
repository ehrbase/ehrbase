package org.ehrbase.rest.openehr.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nedap.archie.rm.changecontrol.VersionedObject;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import org.apache.commons.lang3.StringUtils;

@JacksonXmlRootElement(localName = "ehr_status")
public class VersionedObjectResponse<T> {

    @JsonProperty(value = "_type")
    private String type;
    @JsonProperty
    private HierObjectId uid;
    @JsonProperty(value = "owner_id")
    private ObjectRef<HierObjectId> ownerId;
    @JsonProperty(value = "time_created")
    private String timeCreated;

    public VersionedObjectResponse(VersionedObject<T> versionedObject) {
        setType(versionedObject.getClass().toString()); // FIXME VERSIONED_OBJECT_POC: is of type ""class com.nedap.archie.rm.ehr.VersionedEhrStatus" needs to be "VERSIONED_EHR_STATUS" -> create helper method
        setUid(versionedObject.getUid());
        setOwnerId(versionedObject.getOwnerId());
        //setTimeCreated(versionedObject.getTimeCreated().toString());  // FIXME VERSIONED_OBJECT_POC: this line needs to be active in the end, when valid instance is guaranteed
        setTimeCreated("");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HierObjectId getUid() {
        return uid;
    }

    public void setUid(HierObjectId uid) {
        this.uid = uid;
    }

    public ObjectRef<HierObjectId> getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(ObjectRef<HierObjectId> ownerId) {
        this.ownerId = ownerId;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }
}
