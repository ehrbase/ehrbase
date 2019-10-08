The following snippet shows an example of the optional `ehr_status` request body:

``` json
{
  "_type": "EHR_STATUS",
  "subject": {
    "external_ref": {
      "id": {
        "_type": "GENERIC_ID",
        "value": "ins01",
        "scheme": "id_scheme"
      },
      "namespace": "ehr_craft",
      "type": "PERSON"
    }
  },
  "other_details": {
    "_type": "ITEM_TREE",
    "items": []
  },
  "is_modifiable": "true",
  "is_queryable": "true"
}
```