#V COMPOSITION class testing
##V.1 Validation test of COMPOSITION class
###Preconditions:
* Modify the example
* Create EHR

###Steps:
* Upload the template into DB
* Modify composition example according to the table below
* Create composition

|  Num.  |	 language |	territory | category |composer   |  Expected result&Expected code    |
|--------|------------|-----------|----------|-----------|-----------------------------------|
|    1	 |   exist    |  exist    |  exist   |  exist    |   composition was created	201  |
|    2   |   exist    |  not exist| not exist| not exist |   validation error	        422  |
|    3   |   exist    |  invalid  | invalid  | invalid   |   validation error	        400  |
|    4   |   not exist|  not exist| invalid  |  exist    |   validation error	        400  |
|    5   |   not exist|  invalid  |  exist   | invalid   |   validation error	        400  |
|    6   |   not exist|  exist    | invalid  | invalid   |   validation error	        400  |
|    7   |   invalid  |  invalid  | not exist| exist     |   validation error	        400  |
|    8   |   invalid  |  exist    | not exist| not exist |   validation error	        400  |
|    9   |   invalid  |  not exist|  exist   |  invalid  |   validation error	        400  |


##V.2.b Cardinality of SECTION class testing
###Preconditions:
* In template's section composition.content has cardinality ([ 0..* ])
* Required attributes of SECTION was filled by default
* Create EHR

###Steps:
* Upload the template into DB
* Modify composition example according to the table below
* Create composition

|  Num.  |	 SECTION cardinality |	Number of objects passed to the input   |  Expected result&Expected code    |
|--------|-----------------------|-----------------------|-----------------------------------|
|    1	 |       [0..*]          |    	 0               |   composition was created	201  |
|    2   |                       |       1               |   composition was created	201  |
|    3   |                       |       3               |   composition was created	201  |
|    4   |       [0..1]          |       0               |   composition was created	201  |
|    5   |                       |       1               |   composition was created	201  |
|    6   |                       |       3               |   validation error	        422  |
|    7   |       [1..*]          |       0               |   validation error	        422  |
|    8   |                       |       1               |   composition was created	201  |
|    9   |                       |       3               |   composition was created	201  |
|    10  |       [1..1]          |       0               |   validation error	        422  |
|    11  |                       |       1               |   composition was created	201  |
|    12  |                       |       3               |   validation error	        422  |
|    13  |       [3..*]          |       0               |   validation error	        422  |
|    14  |                       |       1               |   validation error	        422  |
|    15  |                       |       3               |   composition was created	201  |
|    16  |       [3..5]          |       0               |   validation error	        422  |
|    17  |                       |       1               |   validation error	        422  |
|    18  |                       |       3               |   composition was created	201  |


##V.2.d Cardinality of ENTRY class inside the SECTION
###Preconditions:
* In template's section composition.content has cardinality ([ 0..* ])
* Required attributes of SECTION and ENTRY was filled by default
* Create EHR

###Steps:
* Upload the template into DB
* Modify composition example according to the table below
* Create composition

|  Num.  |	 ENTRY cardinality   |	Number of objects passed to the input   |  Expected result&Expected code    |
|--------|-----------------------|-----------------------|-----------------------------------|
|    1	 |       [0..*]          |    	 0               |   composition was created	201  |
|    2   |                       |       1               |   composition was created	201  |
|    3   |                       |       3               |   composition was created	201  |
|    4   |       [0..1]          |       0               |   composition was created	201  |
|    5   |                       |       1               |   composition was created	201  |
|    6   |                       |       3               |   validation error	        422  |
|    7   |       [1..*]          |       0               |   validation error	        422  |
|    8   |                       |       1               |   composition was created	201  |
|    9   |                       |       3               |   composition was created	201  |
|    10  |       [1..1]          |       0               |   validation error	        422  |
|    11  |                       |       1               |   composition was created	201  |
|    12  |                       |       3               |   validation error	        422  |
|    13  |       [3..*]          |       0               |   validation error	        422  |
|    14  |                       |       1               |   validation error	        422  |
|    15  |                       |       3               |   composition was created	201  |
|    16  |       [3..5]          |       0               |   validation error	        422  |
|    17  |                       |       1               |   validation error	        422  |
|    18  |                       |       3               |   composition was created	201  |