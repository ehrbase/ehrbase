Assuming the correct EHR ID is given as path variable, the following example shows a request body:

``` json
{
    "subjectId": "TEST",
    "subjectNamespace": "TEST",
    "queryable": true,
    "modifiable": true,
    "otherDetails": null,
    "otherDetailsTemplateId": null
}
```

Note: `Content-Type` header is overridden to express the body's type due to problematic definition in EhrScape standard.