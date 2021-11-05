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
| 82272006    | SNOMED-CT      | NULL                    | NULL                         | accepted | |


### 2.3.2. Test case DV_CODED_TEXT with local codes

> NOTE: having C_CODE_PHRASE.terminology_id = local and C_CODE_PHRASE.code_list = EMPTY, would be possible at the archetype level, but would be invalid at the template level, so that case is not considered here since it should be validated when the template is uploaded to the SUT.

| code_string | terminology_id | C_CODE_PHRASE.code_list | C_CODE_PHRASE.terminology_id | expected | constraints violated |
|:------------|:---------------|-------------------------|------------------------------|----------|----------------------|
| NULL        | NULL           | [ABC, OPQ]              | local                        | rejected | RM/Schema mandatory both code_String and terminology_id |
| ABC         | NULL           | [ABC, OPQ]              | local                        | rejected | RM/Schema mandatory terminology_id |
| NULL        | local          | [ABC, OPQ]              | local                        | rejected | RM/Schema mandatory code_string |
| ABC         | local          | [ABC, OPQ]              | local                        | accepted | |
| 82272006    | SNOMED-CT      | [ABC, OPQ]              | local                        | rejected | C_CODE_PHRASE.terminology_id |


### 2.3.3. Test case DV_CODED_TEXT with external terminology (constraint reference)

In this case the DV_CODED_TEXT is constrained by a CONSTRAINT_REF. For the CONSTRAINT_REF to be valid in the template, there shoudld be a constraint_binding entry in the template ontology for the acNNNN code of the CONSTRAINT_REF. Without that, the SUT doesn't know which terminology_id can be used in that DV_CODED_TEXT. Note that multiple bindings are possible, so there could be more than one terminology_id for the coded text. The cases where there are no constraint_bindings are not tested here, that should be part of the OPT validation.

> NOTE: the COSNTRAINT_REF in ADL is transformed by the Template Designer into a C_CODE_REFERENCE in OPT, which is a C_CODE_PHRASE subclass with an extra referenceSetUri attribute.

| code_string | terminology_id | CONSTRAINT_REF.reference | constraint_bindings | expected | constraints violated |
|:------------|:---------------|--------------------------|---------------------|----------|----------------------|
| NULL        | NULL           | ac0001                   | [SNOMED_CT]         | rejected | RM/Schema mandatory both code_String and terminology_id |
| ABC         | NULL           | ac0001                   | [SNOMED_CT]         | rejected | RM/Schema mandatory terminology_id |
| NULL        | local          | ac0001                   | [SNOMED_CT]         | rejected | RM/Schema mandatory code_string |
| ABC         | local          | ac0001                   | [SNOMED_CT]         | rejected | constraint_binding: terminology_id not found |
| 82272006    | SNOMED-CT      | ac0001                   | [SNOMED_CT]         | accepted | |


## 2.4. text.DV_PARAGRAPH

// TBD


# 3. quantity

## 3.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.svg)


## 3.2. quantity.DV_ORDINAL

DV_ORDINAL is constrained by C_DV_ORDINAL from AOP (https://specifications.openehr.org/releases/1.0.2/architecture/am/openehr_archetype_profile.pdf), which contains a list of DV_ORDINAL that could be empty.


### 3.3.1. Test case DV_ORDINAL without constraints

> NOTE: at the OPT level this case should be invalid, since is like defining a constraint for a DV_CODED_TEXT with terminology_id `local` but no given codes, since all codes in a C_DV_ORDINAL have terminology_id `local`, at least one code in the list is required at the OPT level. This constraint is valid at the archetypel evel. See commend on 2.3.2.

| symbol         | value | C_DV_ORDINAL.list | expected | constraints violated |
|:---------------|:------|-------------------|----------|----------------------|
| NULL           | NULL  | []                | rejected | RM/Schema both value and symbol are mandatory |
| NULL           | 1     | []                | rejected | RM/Schema symbol is mandatory |
| local::at0005  | NULL  | []                | rejected | RM/Schema value is mandatory |
| local::at0005  | 1     | []                | ? | ? |
| local::at0005  | 666   | []                | ? | ? |


### 3.3.2. Test case DV_ORDINAL with constraints

| symbol         | value | C_DV_ORDINAL.list                    | expected | constraints violated |
|:---------------|:------|--------------------------------------|----------|----------------------|
| NULL           | NULL  | 1|[local::at0005], 2|[local::at0006] | rejected | RM/Schema both value and symbol are mandatory |
| NULL           | 1     | 1|[local::at0005], 2|[local::at0006] | rejected | RM/Schema symbol is mandatory         |
| local::at0005  | NULL  | 1|[local::at0005], 2|[local::at0006] | rejected | RM/Schema value is mandatory          |
| local::at0005  | 1     | 1|[local::at0005], 2|[local::at0006] | accepted |                                       |
| local::at0005  | 666   | 1|[local::at0005], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value  |
| local::at0666  | 1     | 1|[local::at0005], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching symbol |



## 3.3. quantity.DV_SCALE

DV_SCALE was introduced to the RM 1.1.0 (https://openehr.atlassian.net/browse/SPECRM-19), it is analogous to DV_ORDINAL with a Real value. So test cases for DV_SCALE and DV_ORDINAL are similar.

// TBD



## quantity.DV_PROPORTION

## quantity.DV_COUNT

## quantity.DV_QUANTITY

## quantity.DV_INTERVAL



# 4. quantity.date_time

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.date_time.svg)

## quantity.date_time.DV_DURATION

## quantity.date_time.DV_TIME

## quantity.date_time.DV_DATE

## quantity.date_time.DV_DATE_TIME



# 5. time_specification

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.time_specification.svg)

## DV_GENERAL_TIME_SPECIFICATION

## DV_PERIODIC_TIME_SPECIFICATION



# 6. encapsulated

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.encapsulated.svg)

## DV_PARSABLE

## DV_MULTIMEDIA



# 7. uri

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.uri.svg)

## DV_URI


## DV_EHR_URI



