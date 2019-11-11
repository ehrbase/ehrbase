``` json
{
  "_type": "EHR_STATUS",
  "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
  "name": {
    "value": "EHR Status"
  },
  "uid": {
    "_type": "OBJECT_VERSION_ID",
    "value": "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::2"
  },
  "subject": {
    "external_ref": {
      "id": {
        "_type": "HIER_OBJECT_ID",
        "value": "324a4b23-623d-4213-cc1c-23f233b24234"
      },
      "namespace": "DEMOGRAPHIC",
      "type": "PERSON"
    }
  },
  "other_details": {
    "_type": "ITEM_TREE",
    "archetype_node_id": "at0001",
    "name": {
      "value": "Details"
    },
    "items": []
  },
  "is_modifiable": true,
  "is_queryable": true
}
```