Composition Datatyps Validation (Conformance Testing)
--------------------------------------------------------

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2021-11-03) | First version |
||||
||||

# 1. basic

## 1.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.basic.svg)

## 1.2. basic.DV_BOOLEAN

Internally DV_BOOLEAN is constrained by C_BOOLEAN.

### 1.2.1. Test case anything allowed

| value     | C_BOOLEAN.true_valid  | C_BOOLEAN.false_valid | expected | constraints violated |
|:----------|:----------------------|-----------------------|----------|----------------------|
| true      | true                  | true                  | accepted |  |
| false     | true                  | true                  | accepted |  |


### 1.2.2. Test case only true allowed

| value     | C_BOOLEAN.true_valid  | C_BOOLEAN.false_valid | expected | constraints violated |
|:----------|:----------------------|-----------------------|----------|----------------------|
| true      | true                  | false                 | accepted |  |
| false     | true                  | false                 | rejected | C_BOOLEAN.false_valid |


### 1.2.3. Test case only false allowed

| value     | C_BOOLEAN.true_valid  | C_BOOLEAN.false_valid | expected | constraints violated |
|:----------|:----------------------|-----------------------|----------|----------------------|
| true      | false                 | true                  | accepted | C_BOOLEAN.true_valid |
| false     | false                 | true                  | accepted |  |


## 1.3. basic.DV_IDENTIFIER

Internally DV_IDENTIFIER attributes are constrainted by C_STRING.

Note the constraints for each attribute are all checked, so the errors are accumulated. If one validation fails for one attribute, the validation for the whole type fails.

### 1.3.1. Test case validating all attributes using the pattern constraint

| issuer     | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| ABC        | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| XYZ        | XYZ.*             | NULL          | accepted |                      |


| assigner   | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| ABC        | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| XYZ        | XYZ.*             | NULL          | accepted |                      |

| id         | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | XYZ.*             | NULL          | rejected | RM/Schema: this is mandatory in the RM |
| ABC        | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| XYZ        | XYZ.*             | NULL          | accepted |                      |

| type       | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| ABC        | XYZ.*             | NULL          | rejected | C_STRING.pattern     |
| XYZ        | XYZ.*             | NULL          | accepted |                      |


### 1.3.2. Test case validating all attributes using the list constraint

| issuer     | C_STRING.pattern | C_STRING.list | expected | constraints violated |
|:-----------|:-----------------|---------------|----------|----------------------|
| NULL       | NULL             | [XYZ]         | rejected | C_STRING.list        |
| ABC        | NULL             | [XYZ]         | rejected | C_STRING.list        |
| XYZ        | NULL             | [XYZ]         | accepted |                      |


| assigner   | C_STRING.pattern | C_STRING.list | expected | constraints violated |
|:-----------|:-----------------|---------------|----------|----------------------|
| NULL       | NULL             | [XYZ]         | rejected | C_STRING.list        |
| ABC        | NULL             | [XYZ]         | rejected | C_STRING.list        |
| XYZ        | NULL             | [XYZ]         | accepted |                      |

| id         | C_STRING.pattern | C_STRING.list | expected | constraints violated |
|:-----------|:-----------------|---------------|----------|----------------------|
| NULL       | NULL             | [XYZ]         | rejected | RM/Schema: this is mandatory in the RM |
| ABC        | NULL             | [XYZ]         | rejected | C_STRING.list        |
| XYZ        | NULL             | [XYZ]         | accepted |                      |

| type       | C_STRING.pattern | C_STRING.list | expected | constraints violated |
|:-----------|:-----------------|---------------|----------|----------------------|
| NULL       | NULL             | [XYZ]         | rejected | C_STRING.list        |
| ABC        | NULL             | [XYZ]         | rejected | C_STRING.list        |
| XYZ        | NULL             | [XYZ]         | accepted |                      |


## 1.3. basic.DV_STATE

<!--
Internally DV_STATE is constrained by a C_COMPLEX_OBJECT for it's value: DV_CODED_TEXT attribute and by a C_BOOLEAN constraint for it's is_terminal attribute. At the same time, the DV_CODED_TEXT has a C_CODE_PHRASE constraint.
-->

TODO: need to test this since there is also a C_DV_STATE constraint in the archetype profile spect but not sure if the AE/TD support it.

NOTE: I have tested in AE and LinkEHR and this DV is not supported. Also asked on the forum to verify https://discourse.openehr.org/t/is-dv-state-and-its-profile-constraint-c-dv-state-used-anywhere-in-the-specs/2026



# 2. text

## 2.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.text.svg)


## 2.2. text.DV_TEXT

Internally DV_TEXT can be constrained by a C_STRING. This type also allows an instance of the subclass DV_CODED_TEXT at runtime.


### 2.2.1. Test case DV_TEXT with open constraint

In ADL this would mean the C_OBJECT for DV_TEXT matches {\*}, but different Archetype Editors might model this differently, for instance LinkEHR does a DV_TEXT.value matches {'.*'} which is using the C_STRING pattern that matches anything.

| value      | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | NULL              | NULL          | rejected | RM/Schema mandatory  |
| ABC        | NULL              | NULL          | accepted |                      |
| XYZ        | NULL              | NULL          | accepted |                      |


### 2.2.2. Test case DV_TEXT with pattern constraint

> NOTE: if the type is DV_CODED_TEXT at runtime, the value attribte still needs to comply with the C_STRING constraint.

| value      | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | XYZ               | NULL          | rejected | RM/Schema mandatory  |
| ABC        | XYZ               | NULL          | rejected | C_STRING.pattern     |
| XYZ        | XYZ               | NULL          | accepted |                      |


### 2.2.3. Test case DV_TEXT with list constraint

> NOTE: if the type is DV_CODED_TEXT at runtime, the value attribte still needs to comply with the C_STRING constraint.

| value      | C_STRING.pattern  | C_STRING.list | expected | constraints violated |
|:-----------|:------------------|---------------|----------|----------------------|
| NULL       | NULL              | [XYZ, OPQ]    | rejected | RM/Schema mandatory  |
| ABC        | NULL              | [XYZ, OPQ]    | rejected | C_STRING.list        |
| XYZ        | NULL              | [XYZ, OPQ]    | accepted |                      |



## 2.3. text.DV_CODED_TEXT

Internally the DV_CODED_TEXT can be constrained by a C_CODE_PHRASE. Note that in the cases for DV_TEXT we already tested when the type is constrained by a C_STRING (when the declared type is DV_TEXT but the runtime type is DV_CODED_TEXT).

### 2.3.1. Test case DV_CODED_TEXT with open constraint

In ADL this would mean the C_OBJECT for DV_CODED_TEXT matches {\*}.

| code_string | terminology_id | C_CODE_PHRASE.code_list | C_CODE_PHRASE.terminology_id | expected | constraints violated |
|:------------|:---------------|-------------------------|------------------------------|----------|----------------------|
| NULL        | NULL           | NULL                    | NULL                         | rejected | RM/Schema mandatory both code_String and terminology_id |
| ABC         | NULL           | NULL                    | NULL                         | rejected | RM/Schema mandatory terminology_id |
| NULL        | local          | NULL                    | NULL                         | rejected | RM/Schema mandatory code_string |
| ABC         | local          | NULL                    | NULL                         | accepted | |
| 82272006    | SNOMED CT      | NULL                    | NULL                         | accepted | |


### 2.3.2. Test case DV_CODED_TEXT with local codes

> NOTE: having C_CODE_PHRASE.terminology_id = local and C_CODE_PHRASE.code_list = EMPTY, would be possible at the archetype level, but would be invalid at the template level, so that case is not considered here since it should be validated when the template is uploaded to the SUT.

| code_string | terminology_id | C_CODE_PHRASE.code_list | C_CODE_PHRASE.terminology_id | expected | constraints violated |
|:------------|:---------------|-------------------------|------------------------------|----------|----------------------|
| NULL        | NULL           | [ABC, OPQ]              | local                        | rejected | RM/Schema mandatory both code_String and terminology_id |
| ABC         | NULL           | [ABC, OPQ]              | local                        | rejected | RM/Schema mandatory terminology_id |
| NULL        | local          | [ABC, OPQ]              | local                        | rejected | RM/Schema mandatory code_string |
| ABC         | local          | [ABC, OPQ]              | local                        | accepted | |
| 82272006    | SNOMED CT      | [ABC, OPQ]              | local                        | rejected | C_CODE_PHRASE.terminology_id |


### 2.3.3. Test case DV_CODED_TEXT with external terminology (constraint reference)




## 2.4. text.DV_PARAGRAPH

// TBD

# quantity

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.svg)

## quantity.DV_SCALE

## quantity.DV_ORDINAL

## quantity.DV_PROPORTION

## quantity.DV_COUNT

## quantity.DV_QUANTITY

## quantity.DV_INTERVAL


# quantity.date_time

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.date_time.svg)

## quantity.date_time.DV_DURATION

## quantity.date_time.DV_TIME

## quantity.date_time.DV_DATE

## quantity.date_time.DV_DATE_TIME



# time_specification

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.time_specification.svg)

## DV_GENERAL_TIME_SPECIFICATION

## DV_PERIODIC_TIME_SPECIFICATION



# encapsulated

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.encapsulated.svg)

## DV_PARSABLE

## DV_MULTIMEDIA



# uri

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.uri.svg)

## DV_URI


## DV_EHR_URI



