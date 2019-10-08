Example directory body

``` json
{
   "_type": "FOLDER",
   "name": {
       "_type": "DV_TEXT",
       "value": "root"
   },
   "uid": {
       "_type": "OBJECT_VERSION_ID",
       "value": "6e13bbdb-893c-4260-b47d-f3585d178667::example.domain.com::1"
   },
   "folders": [
       {
           "_type": "FOLDER",
           "name": {
               "_type": "DV_TEXT",
               "value": "subject"
           }
       },
       {
           "_type": "FOLDER",
           "name": {
               "_type": "DV_TEXT",
               "value": "persistent"
           }
       }
   ],
   "items": [
       {
           "id": {
               "_type": "OBJECT_VERSION_ID",
               "value": "8849182c-82ad-4088-a07f-48ead4180515::example.domain.com::1"
           },
           "namespace": "example.domain.com",
           "type": "VERSIONED_COMPOSITION"
       }
   ]
}
