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

Internally DV_STATE is constrained by a C_COMPLEX_OBJECT for it's value: DV_CODED_TEXT attribute and by a C_BOOLEAN constraint for it's is_terminal attribute. At the same time, the DV_CODED_TEXT has a C_CODE_PHRASE constraint.

TODO: need to test this since there is also a C_DV_STATE constraint in the archetype profile spect but not sure if the AE/TD support it.



# text

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.text.svg)

## text.DV_TEXT

## text.DV_CODED_TEXT

## text.DV_PARAGRAPH



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



