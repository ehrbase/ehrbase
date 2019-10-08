We will use the relaxed CONTRIBUTION XSD with the following attributes optional:

- time_committed: server will always set it
- UID: if provided will be accepted unless it is already used in which case an error will be returned
- system_id: where provided will be validated


Contribution body:

``` json
{
    "versions": [
        {
            "data": {
                /* optional JSON serialized COMPOSITION, FOLDER or EHR_STATUS object */
            },
            "preceding_version_uid": "<optional string>", 
            "signature": "<optional string>", 
            "lifecycle_state": 0,
            "commit_audit": {
                "change_type": {},
                "description": {},
                "committer": {
                    /* optional structure - will use the outer committer if absent */
                }
            }
        }
    ],
    "audit":{
        "committer":{
            "name": "<optional identifier of the committer>"  ,
            "external_ref":{
                "namespace": "demographic",
                "type": "PERSON",
                "id":{
                    "value": "<OBJECT_ID>"
                }
            }
        }
    }
}
```