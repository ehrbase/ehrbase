If the request body already contains a composition uid it must match the `preceding_version_uid` in the URL. The existing `latest version_uid` of `COMPOSITION` resource must be specified in the `If-Match` header.

Example:

``` json
{
    "_type": "COMPOSITION",
    "name": {
        "_type": "DV_TEXT",
        "value": "Vital Signs"
    },
    ...
}
```