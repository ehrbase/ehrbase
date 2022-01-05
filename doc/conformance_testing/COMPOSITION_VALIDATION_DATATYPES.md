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

NOTE: this datatype is not used and not supported by modeling tools. See https://discourse.openehr.org/t/is-dv-state-and-its-profile-constraint-c-dv-state-used-anywhere-in-the-specs/2026


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

// TBD: see https://discourse.openehr.org/t/is-dv-paragraph-used/2187


# 3. quantity

## 3.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.svg)


## 3.2. quantity.DV_ORDINAL

DV_ORDINAL is constrained by C_DV_ORDINAL from AP (https://specifications.openehr.org/releases/1.0.2/architecture/am/openehr_archetype_profile.pdf), which contains a list of DV_ORDINAL that could be empty.

> NOTE: in ADL it is possible to have a C_DV_ORDINAL constraint with an empty list constraint. At the OPT level this case should be invalid, since is like defining a constraint for a DV_CODED_TEXT with terminology_id `local` but no given codes, since all codes in a C_DV_ORDINAL have terminology_id `local`, at least one code in the list is required at the OPT level. This constraint is valid at the archetypel evel. See commend on 2.3.2.


### 3.2.1. Test case DV_ORDINAL open constraint

This case is when the ADL has `DV_ORDINAL matches {*}`

| symbol         | value | expected | constraints violated |
|:---------------|:------|----------|----------------------|
| NULL           | NULL  | rejected | RM/Schema value and symbol are mandatory |
| NULL           | 1     | rejected | RM/Schema symbol is mandatory            |
| local::at0005  | NULL  | rejected | RM/Schema value is mandatory             |
| local::at0005  | 1     | accepted |                                          |
| local::at0005  | 666   | accepted |                                          |


### 3.2.2. Test case DV_ORDINAL with constraints

| symbol         | value | C_DV_ORDINAL.list                    | expected | constraints violated |
|:---------------|:------|--------------------------------------|----------|----------------------|
| local::at0005  | 1     | 1|[local::at0005], 2|[local::at0006] | accepted |                                       |
| local::at0005  | 666   | 1|[local::at0005], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value  |
| local::at0666  | 1     | 1|[local::at0005], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching symbol |



## 3.3. quantity.DV_SCALE

DV_SCALE was introduced to the RM 1.1.0 (https://openehr.atlassian.net/browse/SPECRM-19), it is analogous to DV_ORDINAL with a Real value. So test cases for DV_SCALE and DV_ORDINAL are similar.

NOTE: if this specification is implemented on a system that supports an RM < 1.1.0, then these tests shouldn't run against the system.

### 3.3.1. Test case DV_SCALE open constraint

This case is when the ADL has `DV_SCALE matches {*}`

| symbol         | value | expected | constraints violated |
|:---------------|:------|----------|----------------------|
| NULL           | NULL  | rejected | RM/Schema value and symbol are mandatory |
| NULL           | 1.5   | rejected | RM/Schema symbol is mandatory            |
| local::at0005  | NULL  | rejected | RM/Schema value is mandatory             |
| local::at0005  | 1.5   | accepted |                                          |
| local::at0005  | 666   | accepted |                                          |

### 3.3.2. Test case DV_SCALE with constraints

> NOTE: there is no current C_DV_SCALE constraint in the Archetype Profile, so modeling tools are not yet supporting constraints for this type. This is a [known issue](https://openehr.atlassian.net/browse/SPECPR-381). Though we can assume the constraint type will be analogous to the C_DV_ORDINAL.

| symbol         | value | C_DV_SCALE.list                          | expected | constraints violated                |
|:---------------|:------|------------------------------------------|----------|-------------------------------------|
| local::at0005  | 1.5   | 1.5|[local::at0005], 2.0|[local::at0006] | accepted |                                     |
| local::at0005  | 66.6  | 1.5|[local::at0005], 2.0|[local::at0006] | rejected | C_DV_SCALE.list: no matching value  |
| local::at0666  | 1.5   | 1.5|[local::at0005], 2.0|[local::at0006] | rejected | C_DV_SCALE.list: no matching symbol |


## 3.4. quantity.DV_COUNT

Internally this type is constrained by a C_INTEGER which could contain a range or a list of values.

### 3.4.1. Test case DV_COUNT open constraint

This case represents the DV_COUNT matching {*}, in this case the C_INTEGER is not present in the OPT.

| magnitude      | expected | constraints violated |
|:---------------|----------|----------------------|
| NULL           | rejected | RM/Schema magnitude is mandatory |
| 0              | accepted |                      |
| 1              | accepted |                      |
| 15             | accepted |                      |
| 30             | accepted |                      |

### 3.4.2. Test case DV_COUNT range constraint

| magnitude      | C_INTEGER.range | C_INTEGER.list    | expected | constraints violated |
|:---------------|:----------------|-------------------|----------|----------------------|
| NULL           | 10..20          | NULL              | rejected | RM/Schema magnitude is mandatory |
| 0              | 10..20          | NULL              | rejected | C_INTEGER.range |
| 1              | 10..20          | NULL              | rejected | C_INTEGER.range |
| 15             | 10..20          | NULL              | accepted |  |
| 30             | 10..20          | NULL              | rejected | C_INTEGER.range |

### 3.4.3. Test case DV_COUNT list constraint

> NOTE: some modeling tools might not support the list constraint.

| magnitude      | C_INTEGER.range | C_INTEGER.list    | expected | constraints violated |
|:---------------|:----------------|-------------------|----------|----------------------|
| NULL           | NULL            | [10,15,20]        | rejected | RM/Schema magnitude is mandatory |
| 0              | NULL            | [10,15,20]        | rejected | C_INTEGER.list |
| 1              | NULL            | [10,15,20]        | rejected | C_INTEGER.list |
| 15             | NULL            | [10,15,20]        | accepted |  |
| 30             | NULL            | [10,15,20]        | rejected | C_INTEGER.list |


## 3.5. quantity.DV_QUANTITY

Internally DV_QUANTITY is constrained by a C_DV_QUANTITY, which allows to specify an optional physical property and a list of C_QUANTITY_ITEM, which can contain a mandatory units and optional interval constraints for magnitude and precision.

### 3.5.1. Test case DV_QUANTITY open constraint

This case represents the DV_QUANTITY matching {*}, in this case the C_DV_QUANTITY is not present in the OPT.

| magnitude | units | expected | constraints violated |
|:----------|:------|----------|----------------------|
| NULL      | NULL  | rejected | RM/Schema both magnitude and untis are mandatory |
| NULL      | cm    | rejected | RM/Schema magnitude is mandatory |
| 1.0       | NULL  | rejected | RM/Schema untis is mandatory |
| 0.0       | cm    | accepted |                      |
| 1.0       | cm    | accepted |                      |
| 5.7       | cm    | accepted |                      |
| 10.0      | cm    | accepted |                      |


### 3.5.2. Test case DV_QUANTITY only property is constrained

The C_DV_QUANTITY is present in the OPT and has a value for `property`, but doesn't have a list of C_QUANTITY_ITEM.

> NOTE: in this case all units for the `property` are allowed, so the validation should look into UCUM for all the possible units of measure or that physical property (the possible values are not un the OPT).

| magnitude | units | C_DV_QUANTITY.property  | C_DV_QUANTITY.list    | expected | constraints violated |
|:----------|:------|:------------------------|-------------------|----------|----------------------|
| NULL      | NULL  | openehr::122 (length)   | NULL              | rejected | RM/Schema both magnitude and untis are mandatory |
| NULL      | cm    | openehr::122 (length)   | NULL              | rejected | RM/Schema magnitude is mandatory |
| 1.0       | NULL  | openehr::122 (length)   | NULL              | rejected | RM/Schema untis is mandatory |
| 0.0       | mg    | openehr::122 (length)   | NULL              | rejected | C_DV_QUANTITY.property: `mg` is not a length unit |
| 0.0       | cm    | openehr::122 (length)   | NULL              | accepted |  |
| 1.0       | cm    | openehr::122 (length)   | NULL              | accepted |  |
| 5.7       | cm    | openehr::122 (length)   | NULL              | accepted |  |
| 10.0      | cm    | openehr::122 (length)   | NULL              | accepted |  |


### 3.5.3. Test case DV_QUANTITY property and units are constrained, without magnitude range

| magnitude | units | C_DV_QUANTITY.property  | C_DV_QUANTITY.list    | expected | constraints violated |
|:----------|:------|:------------------------|-------------------|----------|----------------------|
| NULL      | NULL  | openehr::122 (length)   | [cm, m]           | rejected | RM/Schema both magnitude and untis are mandatory |
| NULL      | cm    | openehr::122 (length)   | [cm, m]           | rejected | RM/Schema magnitude is mandatory |
| 1.0       | NULL  | openehr::122 (length)   | [cm, m]           | rejected | RM/Schema untis is mandatory |
| 0.0       | mg    | openehr::122 (length)   | [cm, m]           | rejected | C_DV_QUANTITY.property: `mg` is not a length unit |
| 0.0       | cm    | openehr::122 (length)   | [cm, m]           | accepted |  |
| 0.0       | km    | openehr::122 (length)   | [cm, m]           | rejected | C_DV_QUANTITY.list: `km` is not allowed |
| 1.0       | cm    | openehr::122 (length)   | [cm, m]           | accepted |  |
| 5.7       | cm    | openehr::122 (length)   | [cm, m]           | accepted |  |
| 10.0      | cm    | openehr::122 (length)   | [cm, m]           | accepted |  |


### 3.5.4. Test case DV_QUANTITY property and units are constrained, with magnitude range

| magnitude | units | C_DV_QUANTITY.property  | C_DV_QUANTITY.list    | expected | constraints violated |
|:----------|:------|:------------------------|-----------------------|----------|----------------------|
| NULL      | NULL  | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | RM/Schema both magnitude and untis are mandatory |
| NULL      | cm    | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | RM/Schema magnitude is mandatory |
| 1.0       | NULL  | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | RM/Schema untis is mandatory |
| 0.0       | mg    | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | C_DV_QUANTITY.property: `mg` is not a length unit |
| 0.0       | cm    | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | C_DV_QUANTITY.list: magnitude not in range for unit  |
| 0.0       | km    | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | C_DV_QUANTITY.list: `km` is not allowed |
| 1.0       | cm    | openehr::122 (length)   | [cm 5.0..10.0, m]     | rejected | C_DV_QUANTITY.list: magnitude not in range for unit |
| 5.7       | cm    | openehr::122 (length)   | [cm 5.0..10.0, m]     | accepted |  |
| 10.0      | cm    | openehr::122 (length)   | [cm 5.0..10.0, m]     | accepted |  |


## 3.6. quantity.DV_PROPORTION

The DV_PROPORTION is contrained by a C_COMPLEX_OBJECT, which internally has C_REAL constraints for `numerator` and `denominator`. C_REAL defines two types of constraints: range and list of values. Though current modeling tools only allow range contraints. For the `type` atribute, a C_INTEGER constraint is used, which can hold list and range constraints but modeling tools only use the list.

This type has intrinsic constraints that should be semantically consistent depending on the value of the numerator, denominator, precision and type attributes. For instance, this if type = 2, the denominator value should be 100 and can't be anything else. In te table below we express the valid combinations of attribute values.

| type | meaning (kind)   | numerator | denominator  | precision | comment |
|:----:|------------------|-----------|--------------|-----------|---------|
| 0    | ratio            | any       | any != 0     | any       |         |
| 1    | unitary          | any       | 1            | any       |         |
| 2    | percent          | any       | 100          | any       |         |
| 3    | fraction         | integer   | integer != 0 | 0         | presentation is num/den |
| 4    | integer fraction | integer   | integer != 0 | 0         | presentation is integral(num/den) decimal(num/den), e.g. for num=3 den=2: 1 1/2 |

> NOTE: the difference between fraction and integer fraction is the presentation, the data and constraints are the same.


### 3.6.1. Test case DV_PROPORTION open constraint, validate RM rules

This test case is used to check the internal rules of the DV_PROPORTION are correctly implemented by the SUT.

| type | meaning (kind)   | numerator | denominator | precision | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | accepted |                                  |
| 0    | ratio            | 10        | 0           | 0         | rejected | valid_denominator (invariant)    |
| 1    | unitary          | 10        | 1           | 0         | accepted |                                  |
| 1    | unitary          | 10        | 0           | 0         | rejected | valid_denominator (invariant)    |
| 1    | unitary          | 10        | 500         | 0         | rejected | unitary_validity (invariant)     |
| 2    | percent          | 10        | 0           | 0         | rejected | valid_denominator (invariant)    |
| 2    | percent          | 10        | 100         | 0         | accepted |                                  |
| 2    | percent          | 10        | 500         | 0         | rejected | percent_validity (invariant)     |
| 3    | fraction         | 10        | 0           | 0         | rejected | valid_denominator (invariant)    |
| 3    | fraction         | 10        | 100         | 0         | accepted |                                  |
| 3    | fraction         | 10        | 500         | 1         | rejected | fraction_validity (invariant)    |
| 3    | fraction         | 10.5      | 500         | 1         | rejected | is_integral_validity (invariant) |
| 3    | fraction         | 10        | 500.5       | 1         | rejected | is_integral_validity (invariant) |
| 4    | integer fraction | 10        | 0           | 0         | rejected | valid_denominator (invariant)    |
| 4    | integer fraction | 10        | 100         | 0         | accepted |                                  |
| 4    | integer fraction | 10        | 500         | 1         | rejected | fraction_validity (invariant)    |
| 4    | integer fraction | 10.5      | 500         | 1         | rejected | is_integral_validity (invariant) |
| 4    | integer fraction | 10        | 500.5       | 1         | rejected | is_integral_validity (invariant) |
| 666  |                  | 10        | 500         | 0         | rejected | type_validity (invariant)        |


### 3.6.2. Test case DV_PROPORTION ratio

The C_INTEGER constraint applies to the `type` attribute.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | [0]            | accepted |                                  |
| 1    | unitary          | 10        | 1           | 0         | [0]            | rejected | C_INTEGER.list                   |
| 2    | percent          | 10        | 100         | 0         | [0]            | rejected | C_INTEGER.list                   |
| 3    | fraction         | 10        | 500         | 0         | [0]            | rejected | C_INTEGER.list                   |
| 4    | integer fraction | 10        | 500         | 0         | [0]            | rejected | C_INTEGER.list                   |

> NOTE: all the fail cases related with invariants were already contemplated in 3.6.1.

### 3.6.3. Test case DV_PROPORTION unitary

The C_INTEGER constraint applies to the `type` attribute.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | [1]            | reejcted | C_INTEGER.list                   |
| 1    | unitary          | 10        | 1           | 0         | [1]            | accepted |                                  |
| 2    | percent          | 10        | 100         | 0         | [1]            | rejected | C_INTEGER.list                   |
| 3    | fraction         | 10        | 500         | 0         | [1]            | rejected | C_INTEGER.list                   |
| 4    | integer fraction | 10        | 500         | 0         | [1]            | rejected | C_INTEGER.list                   |

### 3.6.4. Test case DV_PROPORTION percent

The C_INTEGER constraint applies to the `type` attribute.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | [2]            | reejcted | C_INTEGER.list                   |
| 1    | unitary          | 10        | 1           | 0         | [2]            | rejected | C_INTEGER.list                   |
| 2    | percent          | 10        | 100         | 0         | [2]            | accepted |                                  |
| 3    | fraction         | 10        | 500         | 0         | [2]            | rejected | C_INTEGER.list                   |
| 4    | integer fraction | 10        | 500         | 0         | [2]            | rejected | C_INTEGER.list                   |

### 3.6.5. Test case DV_PROPORTION fraction

The C_INTEGER constraint applies to the `type` attribute.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | [3]            | reejcted | C_INTEGER.list                   |
| 1    | unitary          | 10        | 1           | 0         | [3]            | rejected | C_INTEGER.list                   |
| 2    | percent          | 10        | 100         | 0         | [3]            | rejected | C_INTEGER.list                   |
| 3    | fraction         | 10        | 500         | 0         | [3]            | accepted |                                  |
| 4    | integer fraction | 10        | 500         | 0         | [3]            | rejected | C_INTEGER.list                   |

### 3.6.6. Test case DV_PROPORTION integer fraction

The C_INTEGER constraint applies to the `type` attribute.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | [4]            | reejcted | C_INTEGER.list                   |
| 1    | unitary          | 10        | 1           | 0         | [4]            | rejected | C_INTEGER.list                   |
| 2    | percent          | 10        | 100         | 0         | [4]            | rejected | C_INTEGER.list                   |
| 3    | fraction         | 10        | 500         | 0         | [4]            | rejected | C_INTEGER.list                   |
| 4    | integer fraction | 10        | 500         | 0         | [4]            | accepted |                                  |

### 3.6.7. Test case DV_PROPORTION fraction or integer fraction

This case is similar to the previous one, it just tests a combination of possible types for the proportion.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | expected | constraints violated             |
|:----:|------------------|-----------|-------------|-----------|----------------|----------|----------------------------------|
| 0    | ratio            | 10        | 500         | 0         | [3, 4]         | reejcted | C_INTEGER.list                   |
| 1    | unitary          | 10        | 1           | 0         | [3, 4]         | rejected | C_INTEGER.list                   |
| 2    | percent          | 10        | 100         | 0         | [3, 4]         | rejected | C_INTEGER.list                   |
| 3    | fraction         | 10        | 500         | 0         | [3, 4]         | accepted |                                  |
| 4    | integer fraction | 10        | 500         | 0         | [3, 4]         | accepted |                                  |

### 3.6.8. Test case DV_PROPORTION ratio with range limits

The C_INTEGER constraint applies to the `type` attribute. The C_REAL constraints apply to numerator and denominator respectively.

| type | meaning (kind)   | numerator | denominator | precision | C_INTEGER.list | C_REAL.range (num) | C_REAL.range (den) | expected | constraints violated |
|:----:|------------------|-----------|-------------|-----------|----------------|--------------------|--------------------|----------|----------------------|
| 0    | ratio            | 10        | 500         | 0         | [0]            | 5..20              | 200..600           | accepted |                      |
| 0    | ratio            | 10        | 1           | 0         | [0]            | 5..20              | 200..600           | rejected | C_REAL.range (den)   |
| 0    | ratio            | 30        | 500         | 0         | [0]            | 5..20              | 200..600           | rejected | C_REAL.range (num)   |
| 0    | ratio            | 3         | 1000        | 0         | [0]            | 5..20              | 200..600           | rejected | C_REAL.range (num), C_REAL.range (den) |



## 3.7. quantity.DV_INTERVAL<DV_COUNT>

### 3.7.1. Test case DV_INTERVAL<DV_COUNT> open constraint

The DV_INTERVAL<DV_COUNT> constraint is {*}.

> NOTE: the failure instance for this test case are related with violated interval semantics.

| lower | upper | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated |
|-------|-------|-----------------|-----------------|----------------|----------------|----------|----------------------|
| NULL  | NULL  | true            | true            | false          | false          | accepted |                      |
| NULL  | 100   | true            | false           | false          | false          | accepted |                      |
| NULL  | 100   | true            | false           | false          | true           | accepted |                      |
| 0     | NULL  | false           | true            | false          | false          | accepted |                      |
| 0     | NULL  | false           | true            | true           | false          | accepted |                      |
| -20   | -5    | false           | false           | false          | false          | accepted |                      |
| 0     | 100   | false           | false           | true           | true           | accepted |                      |
| 10    | 100   | false           | false           | true           | true           | accepted |                      |
| -50   | 50    | false           | false           | true           | true           | accepted |                      |
| NULL  | NULL  | true            | true            | true           | false          | rejected | lower_included_valid (invariant) |
| 0     | NULL  | false           | true            | false          | true           | rejected | upper_included_valid (invariant) |
| 200   | 100   | false           | false           | true           | true           | rejected | limits_consistent (invariant) |



### 3.7.2. Test case DV_INTERVAL<DV_COUNT> lower and upper range constraint.

Lower and upper are DV_COUNT, which are constrainted internally by C_INTEGER. C_INTEGER has range and list constraints.

> NOTE: the lower and upper limits are not constrained in terms of existence or occurrences, so both are optional. 

| lower | upper | lower_unbounded | upper_unbounded | lower_included | upper_included | C_INTEGER.range (lower) | C_INTEGER.range (upper) | expected | constraints violated |
|-------|-------|-----------------|-----------------|----------------|----------------|-------------------------|-------------------------|----------|----------------------|
| NULL  | NULL  | true            | true            | false          | false          | 0..100                  | 0..100                  | accepted |                      |
| 0     | NULL  | false           | true            | true           | false          | 0..100                  | 0..100                  | accepted |                      |
| NULL  | 100   | true            | false           | false          | true           | 0..100                  | 0..100                  | accepted |                      |
| 0     | 100   | false           | false           | true           | true           | 0..100                  | 0..100                  | accepted |                      |
| -10   | 100   | false           | false           | true           | true           | 0..100                  | 0..100                  | rejected | C_INTEGER.range (lower) |
| 0     | 200   | false           | false           | true           | true           | 0..100                  | 0..100                  | rejected | C_INTEGER.range (upper) |
| -10   | 200   | false           | false           | true           | true           | 0..100                  | 0..100                  | rejected | C_INTEGER.range (lower), C_INTEGER.range (upper) |


### 3.7.3. Test case DV_INTERVAL<DV_COUNT> lower and upper list constraint.

Lower and upper are DV_COUNT, which are constrainted internally by C_INTEGER. C_INTEGER has range and list constraints.

> NOTE: not all modeling tools allow a list constraint for the lower and upper attributes of the DV_INTERVAL.

| lower | upper | lower_unbounded | upper_unbounded | lower_included | upper_included | C_INTEGER.list (lower)  | C_INTEGER.list (upper)  | expected | constraints violated |
|-------|-------|-----------------|-----------------|----------------|----------------|-------------------------|-------------------------|----------|----------------------|
| NULL  | NULL  | true            | true            | false          | false          | [0, 5, 10, 100]         | [0, 5, 10, 100]         | accepted |                      |
| 0     | NULL  | false           | true            | true           | false          | [0, 5, 10, 100]         | [0, 5, 10, 100]         | accepted |                      |
| NULL  | 100   | true            | false           | false          | true           | [0, 5, 10, 100]         | [0, 5, 10, 100]         | accepted |                      |
| 0     | 100   | false           | false           | true           | true           | [0, 5, 10, 100]         | [0, 5, 10, 100]         | accepted |                      |
| -10   | 100   | false           | false           | true           | true           | [0, 5, 10, 100]         | [0, 5, 10, 100]         | rejected | C_INTEGER.list (lower) |
| 0     | 200   | false           | false           | true           | true           | [0, 5, 10, 100]         | [0, 5, 10, 100]         | rejected | C_INTEGER.list (upper) |
| -10   | 200   | false           | false           | true           | true           | [0, 5, 10, 100]         | [0, 5, 10, 100]         | rejected | C_INTEGER.list (lower), C_INTEGER.list (upper) |


## 3.8. quantity.DV_INTERVAL<DV_QUANTITY>

### 3.8.1. Test case DV_INTERVAL<DV_QUANTITY> open constraint

The DV_INTERVAL<DV_QUANTITY> constraint is {*}.

> NOTE: the failure instance for this test case are related with violated interval semantics.

| lower  | upper  | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated |
|--------|--------|-----------------|-----------------|----------------|----------------|----------|----------------------|
| NULL   | NULL   | true            | true            | false          | false          | accepted |                      |
| NULL   | 100 mg | true            | false           | false          | false          | accepted |                      |
| NULL   | 100 mg | true            | false           | false          | true           | accepted |                      |
| 0 mg   | NULL   | false           | true            | false          | false          | accepted |                      |
| 0 mg   | NULL   | false           | true            | true           | false          | accepted |                      |
| 0 mg   | 100 mg | false           | false           | true           | true           | accepted |                      |
| 10 mg  | 100 mg | false           | false           | true           | true           | accepted |                      |
| NULL   | NULL   | true            | true            | true           | false          | rejected | lower_included_valid (invariant) |
| 0 mg   | NULL   | false           | true            | false          | true           | rejected | upper_included_valid (invariant) |
| 200 mg | 100 mg | false           | false           | true           | true           | rejected | limits_consistent (invariant)    |


### 3.8.2. Test case DV_INTERVAL<DV_QUANTITY> lower and upper constraints present

The lower and upper constraints are C_DV_QUANTITY.

> NOTE: in all cases the C_DV_QUANTITY.property referes to `temperature` to keep tests as simple as possible and be able to use negative values (for other physical properties negative values don't make sense). All temperatures will be measured in degree Celsius (`Cel` in UCUM).

| lower     | upper   | lower_unbounded | upper_unbounded | lower_included | upper_included | C_DV_QUANTITY.list (lower) | C_DV_QUANTITY.list (upper) | expected | constraints violated  |
|:---------:|:-------:|-----------------|-----------------|----------------|----------------|----------------------------|----------------------------|----------|-----------------------|
| NULL      | NULL    | true            | true            | false          | false          | [0..100 Cel]               | [0..100 Cel]               | accepted |                       |
| 0 Cel     | NULL    | false           | true            | true           | false          | [0..100 Cel]               | [0..100 Cel]               | accepted |                       |
| NULL      | 100 Cel | true            | false           | false          | true           | [0..100 Cel]               | [0..100 Cel]               | accepted |                       |
| 0 Cel     | 100 Cel | false           | false           | true           | true           | [0..100 Cel]               | [0..100 Cel]               | accepted |                       |
| -10 Cel   | 100 Cel | false           | false           | true           | true           | [0..100 Cel]               | [0..100 Cel]               | rejected | C_DV_QUANTITY (lower) |
| 0 Cel     | 200 Cel | false           | false           | true           | true           | [0..100 Cel]               | [0..100 Cel]               | rejected | C_DV_QUANTITY (upper) |
| -10 Cel   | 200 Cel | false           | false           | true           | true           | [0..100 Cel]               | [0..100 Cel]               | rejected | C_DV_QUANTITY (lower),C_DV_QUANTITY (upper) |


## 3.9. quantity.DV_INTERVAL<DV_DATE_TIME>

### 3.9.1. Test case DV_INTERVAL<DV_DATE_TIME> open constraint

The DV_INTERVAL<DV_DATE_TIME> constraint is {*}.

TBD: this will use the test cases and data sets defined for the DV_DATE_TIME tests.

### 3.9.2. Test case DV_INTERVAL<DV_DATE_TIME> lower and upper constraints are validity kind


### 3.9.3. Test case DV_INTERVAL<DV_DATE_TIME> lower and upper constraints are range

TBD




## 3.10. quantity.DV_INTERVAL<DV_DATE>

TBD: this will use the test cases and data sets defined for the DV_DATE tests.


### 3.10.1. Test case DV_INTERVAL<DV_DATE> open constraint

On this case, the own rules/invariants of the DV_INTERVAL apply to the validation.

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------|-------------------------------|
| NULL       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| NULL       | 2022       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| 2021       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| 2021       | 2022       | false           | false           | true           | true           | accepted |  |
| 2021-01    | 2022-08    | false           | false           | true           | true           | accepted |  |
| 2021-01-20 | 2022-08-11 | false           | false           | true           | true           | accepted |  |
| 2021       | 2021-10    | false           | false           | true           | true           | rejected | IMO two dates with different components and common higher order components (year on this case) shouldn't be strictly comparable, see https://discourse.openehr.org/t/issues-with-date-time-comparison-for-partial-date-time-expressions/2173 |
| NULL       | NULL       | true            | true            | false          | false          | accepted |  |


### 3.10.2. Test case DV_INTERVAL<DV_DATE> validity kind constraint

```
NOTE: this test case doesn't include all the possible combinations of lower/upper data and constraints for the internal since there could be tens of possible combinations. It would be in the scope of a revision to add more combinations of an exhaustive test case.
```

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | month_val. (lower) | day_val. (lower) | month_val. (upper) | day_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|--------------------|------------------|--------------------|------------------|----------|-------------------------------|
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | optional         | mandatory          | optional         | rejected | month_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | optional           | optional         | optional           | optional         | accepted |                               |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | prohibited       | mandatory          | prohibited       | rejected | month_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | prohibited         | prohibited       | prohibited         | prohibited       | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | day_validity (lower), day_validity (upper)     |
| 2021-10    | 2022-10    | false           | false           | true           |true            | mandatory          | optional         | mandatory          | optional         | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | optional           | optional         | optional           | optional         | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | mandatory          | prohibited       | mandatory          | prohibited       | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | prohibited         | prohibited       | prohibited         | prohibited       | rejected | month_validity (lower), month_validity (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | accepted |                               |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | mandatory          | optional         | mandatory          | optional         | accepted |                               |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | optional           | optional         | optional           | optional         | accepted |                               |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | mandatory          | prohibited       | mandatory          | prohibited       | rejected | day_validity (lower), day_validity (upper)     |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | prohibited         | prohibited       | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | optional         | rejected | month_validity (lower), day_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | optional           | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | month_validity (lower), day_validity (lower), day_validity (upper)   |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | optional           | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | prohibited       | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper) |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | optional           | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | prohibited       | rejected | month_validity (lower), day_validity (lower), day_validity (upper)   |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |



### 3.10.3. Test case DV_INTERVAL<DV_DATE> range constraint

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | C_DATE.range (lower) | C_DATE.range (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------------------|----------------------|----------|---------------------------|
| 2021       | 2022       | false           | false           | true           | true           | 1900..2030           | 1900..2030           | accepted |         |
| 2021       | 2022       | false           | false           | true           | true           | 2022..2030           | 1900..2030           | rejected | C_DATE.range (lower)        |
| 2021       | 2022       | false           | false           | true           | true           | 1900..2030           | 2023..2030           | rejected | C_DATE.range (upper)         |
| 2021       | 2022       | false           | false           | true           | true           | 2022..2030           | 2023..2030           | rejected | C_DATE.range (lower), C_DATE.range (upper)         |





## 3.11. quantity.DV_INTERVAL<DV_TIME>

### 3.11.1. Test case DV_INTERVAL<DV_TIME> open constraint

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------|-------------------------------|
| NULL       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| NULL       | T11:00:00  | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| T10:00:00  | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| T10        | T11        | false           | false           | true           | true           | accepted |  |
| T10:00     | T11:00     | false           | false           | true           | true           | accepted |  |
| T10:00:00  | T11:00:00  | false           | false           | true           | true           | accepted |  |
| T10        | T10:45:00  | false           | false           | true           | true           | rejected | IMO two times with different components and common higher order components (hour on this case) shouldn't be strictly comparable, see https://discourse.openehr.org/t/issues-with-date-time-comparison-for-partial-date-time-expressions/2173 |
| NULL       | NULL       | true            | true            | false          | false          | accepted |  |


### 3.11.2. Test case DV_INTERVAL<DV_TIME> validity kind constraint

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | minute_val. (lower) | second_val. (lower) | millisecond_val. (lower) | timezone_val. (lower) | minute_val. (upper) | second_val. (upper) | millisecond_val. (upper) | timezone_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|---------------------|---------------------|-------------------------|-----------------------|---------------------|---------------------|--------------------------|-----------------------|---------|-------------------------------|
| T10        | T11        | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | minute_val. (lower), second_val. (lower), millisecond_val. (lower), timezone_val. (lower), minute_val. (upper), second_val. (upper), millisecond_val. (upper), timezone_val. (upper) |
| T10:00     | T11:00    | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | second_val. (lower), millisecond_val. (lower), timezone_val. (lower), second_val. (upper), millisecond_val. (upper), timezone_val. (upper) |
| T10:00:00  | T11:00:00 | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | millisecond_val. (lower), timezone_val. (lower), millisecond_val. (upper), timezone_val. (upper) |
| T10:00:00.5  | T11:00:00.5 | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | timezone_val. (lower) timezone_val. (upper) |
| T10:00:00.5Z | T11:00:00.5Z | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | accepted |  |

TBD: combinations of other values for validity.


### 3.11.3. Test case DV_INTERVAL<DV_TIME> range constraint

| lower         | upper         | lower_unbounded | upper_unbounded | lower_included | upper_included | C_TIME.range (lower)    | C_TIME.range (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------------------|----------------------|----------|---------------------------|
| T10          | T11          | false           | false           | true           | true           | T09..T11                  | T10..T12          | accepted |    |
| T10:00       | T11:00       | false           | false           | true           | true           | T09:00..T11:00            | T10:00..T12:00       | accepted |    |
| T10:00:00    | T11:00:00    | false           | false           | true           | true           | T09:00:00..T11:00:00      | T10:00:00..T12:00:00    | accepted |    |
| T10:00:00.5  | T11:00:00.5  | false           | false           | true           | true           | T09:00:00.0..T11:00:00.0  | T10:00:00.0..T12:00:00.0  | accepted |    |
| T10:00:00.5Z | T11:00:00.5Z | false           | false           | true           | true           | T09:00:00.0..T11:00:00.0Z | T10:00:00.0Z..T12:00:00.0Z | accepted |    |

TBD: fail cases


## 3.12. quantity.DV_INTERVAL<DV_DURATION>

TBD


## 3.13. quantity.DV_INTERVAL<DV_ORDINAL>

> NOTE: some modeling tools don't support representing DV_INTERVAL<DV_ORDINAL>.


## 3.14. quantity.DV_INTERVAL<DV_SCALE>

> NOTE: some modeling tools don't support representing DV_INTERVAL<DV_SCALE>.


## 3.15. quantity.DV_INTERVAL<DV_PROPORTION>

> NOTE: some modeling tools don't support representing DV_INTERVAL<DV_PROPORTION>.



# 4. quantity.date_time

## 4.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.date_time.svg)


## 4.2. quantity.date_time.DV_DURATION

### 4.2.1. Test case DV_DURATION open constraint

| value           | expected | violated constraints |
|-----------------|----------|----------------------|
| NULL            | rejected | DV_DURATION.value is mandatory in the RM |
| 1Y              | rejected | wrong ISO 8601 duration: missing duration desingator 'P' |         
| P1Y             | accepted | |
| P1Y3M           | accepted | |
| P1W             | accepted | |
| P1Y3M4D         | accepted | |
| P1Y3M4DT2H      | accepted | |
| P1Y3M4DT2H14M   | accepted | |
| P1Y3M4DT2H14M5S | accepted | |


### 4.2.2. Test case DV_DURATION fields allowed constraint

The `allowed` fields are defined in the `C_DURATION` class, which allows to constraint the DV_DURATION.value attribute.

| value              | years_allowed | months_allowed | weeks_allowed | days_allowed | hours_allowed | minutes_allowed | seconds_allowed | fractional_seconds_allowed | expected | violated constraints     |
|--------------------|---------------|----------------|---------------|--------------|---------------|-----------------|-----------------|----------------------------|----------|--------------------------|
| P1Y                | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P1Y                | false         | true           | true          | true         | true          | true            | true            | ???                        | rejected | C_DURATION.years_allowed |
| P1Y3M              | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P1Y3M              | true          | false          | true          | true         | true          | true            | true            | ???                        | rejected | C_DURATION.months_allowed |
| P1Y3M15D           | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P1Y3M15D           | true          | true           | true          | false        | true          | true            | true            | ???                        | rejected | C_DURATION.days_allowed |
| P1W                | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P7W                | true          | true           | false         | true         | true          | true            | true            | ???                        | rejected | C_DURATION.weeks_allowed |
| P1Y3M15DT23H       | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P1Y3M15DT23H       | true          | true           | true          | true         | false         | true            | true            | ???                        | rejected | C_DURATION.hours_allowed |
| P1Y3M15DT23H35M    | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P1Y3M15DT23H35M    | true          | true           | true          | true         | true          | false           | true            | ???                        | rejected | C_DURATION.minutes_allowed |
| P1Y3M15DT23H35M22S | true          | true           | true          | true         | true          | true            | true            | ???                        | accepted |  |
| P1Y3M15DT23H35M22S | true          | true           | true          | true         | true          | true            | false           | ???                        | rejected | C_DURATION.seconds_allowed |

> NOTE: the `fractional_seconds_allowed` field is not so clear since the ISO8601 would allow fractions on the lowest order component, which means if the duration lowest component is `minutes` then it's valid to have `5.23M`. Also consider in programming languages like Java, a duration string with fractions on other fields than seconds can't be parsed (for instance using https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-)


### 4.2.3. Test case DV_DURATION range constraint

| value             | range.lower    | range.upper    | expected | violated constraints   |
|-------------------|----------------|----------------|----------|------------------------|
| P1Y               | P0Y            | P50Y           | accepted |                        |
| P1Y               | P1Y            | P50Y           | accepted |                        |
| P1Y               | P2Y            | P50Y           | rejected | C_DURATION.range.lower |
| P1M               | P0M            | P50M           | accepted |                        |
| P1M               | P1M            | P50M           | accepted |                        |
| P1M               | P2M            | P50M           | rejected | C_DURATION.range.lower |
| P1D               | P0D            | P50D           | accepted |                        |
| P1D               | P1D            | P50D           | accepted |                        |
| P1D               | P2D            | P50D           | rejected | C_DURATION.range.lower |
| P1Y2M             | P0Y            | P50Y           | accepted |                        |
| P1Y2M             | P1Y            | P50Y           | accepted |                        |
| P1Y2M             | P2Y            | P50Y           | rejected | C_DURATION.range.lower |
| P1Y20M            | P0Y            | P50Y           | accepted |                        |
| P1Y20M            | P1Y            | P50Y           | accepted |                        |
| P1Y20M            | P2Y            | P50Y           | ??? | TBD: it is not clear if the 20M are transformed to years to be compared with the range limits that only have years or if years in the value are compared with years in the range limits and if there are no limits for months in the range limits then the months in the value are not constrainted. |
| P2W               | P0W            | P3W            | accepted |                        |
| P2W               | P2W            | P3W            | accepted |                        |
| P2W               | P3W            | P3W            | rejected | C_DURATION.range.lower |


### 4.2.4. Test case DV_DURATION fields allowed and range constraints combined

In the AOM specification it is allowed to combine allowed and range: "Both range and the constraint pattern can be set at the same time, corresponding to the ADL constraint PWD/|P0W..P50W|. (https://specifications.openehr.org/releases/AM/Release-2.2.0/AOM1.4.html#_c_duration_class)"

| value              | years_allowed | months_allowed | weeks_allowed | days_allowed | hours_allowed | minutes_allowed | seconds_allowed | fractional_seconds_allowed | range.lower | range.upper | expected | violated constraints     |
|--------------------|---------------|----------------|---------------|--------------|---------------|-----------------|-----------------|----------------------------|-------------|-------------|----------|--------------------------|
| P1Y                | true          | true           | true          | true         | true          | true            | true            | ???                        | P0Y         | P50Y        | accepted |  |
| P1Y                | true          | true           | true          | true         | true          | true            | true            | ???                        | P2Y         | P50Y        | rejected | C_DURATION.range.lower  |
| P1Y                | false         | true           | true          | true         | true          | true            | true            | ???                        | P0Y         | P50Y        | rejected | C_DURATION.years_allowed |
| P1Y                | false         | true           | true          | true         | true          | true            | true            | ???                        | P2Y         | P50Y        | rejected | C_DURATION.years_allowed, C_DURATION.range.lower |

| P1Y3M              | true          | true           | true          | true         | true          | true            | true            | ???                        | P1Y         | P50Y        | accepted |  |
| P1Y3M              | true          | false          | true          | true         | true          | true            | true            | ???                        | P1Y         | P50Y        | rejected | C_DURATION.months_allowed |
| P1Y3M              | true          | true           | true          | true         | true          | true            | true            | ???                        | P3Y         | P50Y        | rejected | C_DURATION.lower |
| P1Y3M              | true          | false          | true          | true         | true          | true            | true            | ???                        | P3Y         | P50Y        | rejected | C_DURATION.months_allowed. C_DURATION.lower |




## 4.3. quantity.date_time.DV_TIME

DV_TIME constraints are defined by C_TIME, which specifies two types of constraints: validity kind and range. The validity kind constraints are expressed in terms of mandatory/optional/prohibited flags for each part of the time expression: minute, second, millisecond and timezone. The range constraint is an Interval<Time>, which are both [Foundation Types](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html).

> NOTE: the Time class mentioned in the AOM specification is actually the [Iso8601_date_time](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_iso8601_date_time_class) class. This is a known bug in the specs.

Note that time expressions in openEHR are considered an absolute point in time from the start of the current day, that is `T10` represents `10:00:00.000` AM in the local timezone.

### 4.3.1. Test case DV_TIME open constraint

This case is when DV_TIME matches {*}.

> NOTE 1: the decimal mark for the seconds fraction could be `,` (comma) or `.` (period) at in-memory and storage representations of time expressions, but since in most popular exchange formats the `.` is preferred, and considering the implementation of these test cases will surelly use those exchange formats, we only specify test data sets which use the decimal mark `.`. Nevetheless, the `,` is totally valid at in-memory and storage levels.
> In the same line, basic and extended formats are allowed at in-memory and storage representations. Basic format being the time parts without any separators and the extended being the parts with separatos `:` (colon). The extended format is also preferred by the most common exchange fornats, so only test data sets using extended format will be specified.

> NOTE 2: "There is no limit on the number of decimal places for the decimal fraction. However, the number of decimal places needs to be agreed to by the communicating parties." [REF](https://en.wikipedia.org/wiki/ISO_8601#Times)

> NOTE 3: the time marker `T` can be ommitted for the extended format in ISO8601:2019, because there is no risk of ambiguity. Since this is nor mandatory, our test data sets all include the `T` time marker.

> NOTE 4: if no timezone information is included, the time expression is considered `local time`.

> NOTE 5: one clarification about the seconds fraction in ISO8601 is that is not exactly an expression of milliseconds as the AOM specification implies considering the `millisecond_validity` fields. For instance `.5` represents half a second, which is indeed 500 milliseconds but `.5` is not syntactically `500 ms`, or `.333333` represents one third of a second, and syntactilclly `333333` goes beyond the precision of milliseconds which is just 3 digits long. Consider `.33333` is totally valid in ISO8601 for the seconds fraction (see NOTE 2).

| value                  | expected | violated constraints          |
|------------------------|----------|-------------------------------|
| NULL                   | rejected | RM/Schema: value is mandatory |
| ''                     | rejected | ISO8601: at least minutes are required |
| T10                    | accepted |                               |
| T48                    | rejected | ISO8601: hours in 0..23       |
| T10:30                 | accepted |                               |
| T10:95                 | rejected | ISO8601: minutes in 0..59     |
| T10:30:47              | accepted |                               |
| T10:30:78              | rejected | ISO8601: seconds in 0..59     |
| T10:30:47.5            | accepted |                               |
| T10:30:47.333          | accepted |                               |
| T10:30:47.333333       | accepted |                               |
| T10:30:47Z             | accepted |                               |
| T10:30:78Z             | rejected | ISO8601: seconds in 0..59     |
| T10:30:47.5Z           | accepted |                               |
| T10:30:47.333Z         | accepted |                               |
| T10:30:47.333333Z      | accepted |                               |
| T10:30:47-03:00        | accepted |                               |
| T10:30:78-03:00        | rejected | ISO8601: seconds in 0..59     |
| T10:30:47.5-03:00      | accepted |                               |
| T10:30:47.333-03:00    | accepted |                               |
| T10:30:47.333333-03:00 | accepted |                               |


### 4.3.2. Test case DV_TIME validity kind constraint

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10                    | mandatory       | mandatory       | mandatory            | mandatory         | rejected | minute_validity, second_validity, millisecond_validity, timezone_validity |
| T10                    | mandatory       | mandatory       | mandatory            | optional          | rejected | minute_validity, second_validity, millisecond_validity |
| T10                    | mandatory       | mandatory       | optional             | optional          | rejected | minute_validity, second_validity   |
| T10                    | mandatory       | optional        | optional             | optional          | rejected | minute_validity                    |
| T10                    | optional        | optional        | optional             | optional          | accepted |                                    |
| T10                    | mandatory       | mandatory       | mandatory            | prohibited        | rejected | minute_validity, second_validity, millisecond_validity |
| T10                    | mandatory       | mandatory       | prohibited           | prohibited        | rejected | minute_validity, second_validity   |
| T10                    | mandatory       | prohibited      | prohibited           | prohibited        | rejected | minute_validity                    |
| T10                    | prohibited      | prohibited      | prohibited           | prohibited        | accepted |                                    |

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10:30                 | mandatory       | mandatory       | mandatory            | mandatory         | rejected | second_validity, millisecond_validity, timezone_validity |
| T10:30                 | mandatory       | mandatory       | mandatory            | optional          | rejected | second_validity, millisecond_validity |
| T10:30                 | mandatory       | mandatory       | optional             | optional          | rejected | second_validity                    |
| T10:30                 | mandatory       | optional        | optional             | optional          | accepted |                                    |
| T10:30                 | optional        | optional        | optional             | optional          | accepted |                                    |
| T10:30                 | mandatory       | mandatory       | mandatory            | prohibited        | rejected | second_validity, millisecond_validity |
| T10:30                 | mandatory       | mandatory       | prohibited           | prohibited        | rejected | second_validity                    |
| T10:30                 | mandatory       | prohibited      | prohibited           | prohibited        | accepted |                                    |
| T10:30                 | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity                    |

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10:30:47              | mandatory       | mandatory       | mandatory            | mandatory         | rejected | millisecond_validity, timezone_validity |
| T10:30:47              | mandatory       | mandatory       | mandatory            | optional          | rejected | millisecond_validity               |
| T10:30:47              | mandatory       | mandatory       | optional             | optional          | accepted |                                    |
| T10:30:47              | mandatory       | optional        | optional             | optional          | accepted |                                    |
| T10:30:47              | optional        | optional        | optional             | optional          | accepted |                                    |
| T10:30:47              | mandatory       | mandatory       | mandatory            | prohibited        | rejected | millisecond_validity               |
| T10:30:47              | mandatory       | mandatory       | prohibited           | prohibited        | accepted |                                    |
| T10:30:47              | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity                    |
| T10:30:47              | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity   |

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10:30:47.5            | mandatory       | mandatory       | mandatory            | mandatory         | rejected | timezone_validity                  |
| T10:30:47.5            | mandatory       | mandatory       | mandatory            | optional          | accepted |                                    |
| T10:30:47.5            | mandatory       | mandatory       | optional             | optional          | accepted |                                    |
| T10:30:47.5            | mandatory       | optional        | optional             | optional          | accepted |                                    |
| T10:30:47.5            | optional        | optional        | optional             | optional          | accepted |                                    |
| T10:30:47.5            | mandatory       | mandatory       | mandatory            | prohibited        | accepted |                                    |
| T10:30:47.5            | mandatory       | mandatory       | prohibited           | prohibited        | rejected | millisecond_validity               |
| T10:30:47.5            | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, millisecond_validity |
| T10:30:47.5            | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, millisecond_validity |

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10:30:47Z             | mandatory       | mandatory       | mandatory            | mandatory         | rejected | millisecond_validity               |
| T10:30:47Z             | mandatory       | mandatory       | mandatory            | optional          | rejected | millisecond_validity               |
| T10:30:47Z             | mandatory       | mandatory       | optional             | optional          | accepted |                                    |
| T10:30:47Z             | mandatory       | optional        | optional             | optional          | accepted |                                    |
| T10:30:47Z             | optional        | optional        | optional             | optional          | accepted |                                    |
| T10:30:47Z             | mandatory       | mandatory       | mandatory            | prohibited        | rejected | millisecond_validity, timezone_validity |
| T10:30:47Z             | mandatory       | mandatory       | prohibited           | prohibited        | rejected | timezone_validity                  |
| T10:30:47Z             | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, timezone_validity |
| T10:30:47Z             | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, timezone_validity |

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10:30:47.5Z           | mandatory       | mandatory       | mandatory            | mandatory         | accepted |                                    |
| T10:30:47.5Z           | mandatory       | mandatory       | mandatory            | optional          | accepted |                                    |
| T10:30:47.5Z           | mandatory       | mandatory       | optional             | optional          | accepted |                                    |
| T10:30:47.5Z           | mandatory       | optional        | optional             | optional          | accepted |                                    |
| T10:30:47.5Z           | optional        | optional        | optional             | optional          | accepted |                                    |
| T10:30:47.5Z           | mandatory       | mandatory       | mandatory            | prohibited        | rejected | timezone_validity                  |
| T10:30:47.5Z           | mandatory       | mandatory       | prohibited           | prohibited        | rejected | millisecond_validity, timezone_validity                  |
| T10:30:47.5Z           | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, millisecond_validity, timezone_validity |
| T10:30:47.5Z           | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, millisecond_validity, timezone_validity |

| value                  | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| T10:30:47.5-03:00      | mandatory       | mandatory       | mandatory            | mandatory         | accepted |                                    |
| T10:30:47.5-03:00      | mandatory       | mandatory       | mandatory            | optional          | accepted |                                    |
| T10:30:47.5-03:00      | mandatory       | mandatory       | optional             | optional          | accepted |                                    |
| T10:30:47.5-03:00      | mandatory       | optional        | optional             | optional          | accepted |                                    |
| T10:30:47.5-03:00      | optional        | optional        | optional             | optional          | accepted |                                    |
| T10:30:47.5-03:00      | mandatory       | mandatory       | mandatory            | prohibited        | rejected | timezone_validity                  |
| T10:30:47.5-03:00      | mandatory       | mandatory       | prohibited           | prohibited        | rejected | millisecond_validity, timezone_validity                  |
| T10:30:47.5-03:00      | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, millisecond_validity, timezone_validity |
| T10:30:47.5-03:00      | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, millisecond_validity, timezone_validity |



### 4.3.3. Test case DV_TIME range constraint

The C_TIME.range constraint is an Interval<Time>, which are both [Foundation Types](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html).

> NOTE: the Time class mentioned in the AOM specification is actually the [Iso8601_time](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_time_types) class. This is a [known bug](https://openehr.atlassian.net/browse/SPECPR-380) in the specs.

TBD: we need to clarify if T10 is referring to the whole 10th hour or to 10:00:00 omitting the rest of the time components, because it could be a similar case as we have with the date expressions: can only compare expressions that have exactly the same components.

TBD: there is an open question about strictly comparability between time expressions with different components. Is "T10" comparable to "T00:00"?

| value                  | C_TIME.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| T10                    | T00..T23                   | accepted |                               |
| T10                    | T00:00..T23:59             | accepted |                               |
| T10                    | T00:00:00..T23:59:59       | accepted |                               |
| T10                    | T00:00:00.0..T23:59:59.999 | accepted |                               |
| T10                    | T11..T23                   | rejected | C_TIME.range                  |
| T10                    | T11:00..T23:59             | rejected | C_TIME.range                  |
| T10                    | T11:00:00..T23:59:59       | rejected | C_TIME.range                  |
| T10                    | T11:00:00.0..T23:59:59.999 | rejected | C_TIME.range                  |
| T10                    | T00..T09                   | rejected | C_TIME.range                  |
| T10                    | T00:00..T09:59             | rejected | C_TIME.range                  |
| T10                    | T00:00:00..T09:59:59       | rejected | C_TIME.range                  |
| T10                    | T00:00:00.0..T09:59:59.999 | rejected | C_TIME.range                  |
| T10                    | >=T00                      | accepted |                               |
| T10                    | >=T00:00                   | accepted |                               |
| T10                    | >=T00:00:00                | accepted |                               |
| T10                    | >=T00:00:00.0              | accepted |                               |
| T10                    | >=T11                      | rejected | C_TIME.range                  |
| T10                    | >=T11:00                   | rejected | C_TIME.range                  |
| T10                    | >=T11:00:00                | rejected | C_TIME.range                  |
| T10                    | >=T11:00:00.0              | rejected | C_TIME.range                  |
| T10                    | <=T09                      | rejected | C_TIME.range                  |
| T10                    | <=T09:59                   | rejected | C_TIME.range                  |
| T10                    | <=T09:59:59                | rejected | C_TIME.range                  |
| T10                    | <=T09:59:59.999            | rejected | C_TIME.range                  |

> NOTE: the range with the timezone included doesn't make sense when the time value doesn't have a timezone, since will compare a local time (without TZ) with a global time (with TZ). This case should be considered an error at the archetype level.  Analogously, if the DV_TIME value has a timezome, the C_TIME.range constraints should include the timezone.


| value                  | C_TIME.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| T10:30                 | T00..T23                   | accepted |                               |
| T10:30                 | T00:00..T23:59             | accepted |                               |
| T10:30                 | T00:00:00..T23:59:59       | accepted |                               |
| T10:30                 | T00:00:00.0..T23:59:59.999 | accepted |                               |
| T10:30                 | T11..T23                   | rejected | C_TIME.range                  |
| T10:30                 | T11:00..T23:59             | rejected | C_TIME.range                  |
| T10:30                 | T11:00:00..T23:59:59       | rejected | C_TIME.range                  |
| T10:30                 | T11:00:00.0..T23:59:59.999 | rejected | C_TIME.range                  |
| T10:30                 | T00..T09                   | rejected | C_TIME.range                  |
| T10:30                 | T00:00..T09:59             | rejected | C_TIME.range                  |
| T10:30                 | T00:00:00..T09:59:59       | rejected | C_TIME.range                  |
| T10:30                 | T00:00:00.0..T09:59:59.999 | rejected | C_TIME.range                  |
| T10:30                 | >=T00                      | accepted |                               |
| T10:30                 | >=T00:00                   | accepted |                               |
| T10:30                 | >=T00:00:00                | accepted |                               |
| T10:30                 | >=T00:00:00.0              | accepted |                               |
| T10:30                 | >=T11                      | rejected | C_TIME.range                  |
| T10:30                 | >=T11:00                   | rejected | C_TIME.range                  |
| T10:30                 | >=T11:00:00                | rejected | C_TIME.range                  |
| T10:30                 | >=T11:00:00.0              | rejected | C_TIME.range                  |
| T10:30                 | <=T09                      | rejected | C_TIME.range                  |
| T10:30                 | <=T09:59                   | rejected | C_TIME.range                  |
| T10:30                 | <=T09:59:59                | rejected | C_TIME.range                  |
| T10:30                 | <=T09:59:59.999            | rejected | C_TIME.range                  |

| value                  | C_TIME.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| T10:30:47              | T00..T23                   | accepted |                               |
| T10:30:47              | T00:00..T23:59             | accepted |                               |
| T10:30:47              | T00:00:00..T23:59:59       | accepted |                               |
| T10:30:47              | T00:00:00.0..T23:59:59.999 | accepted |                               |
| T10:30:47              | T11..T23                   | rejected | C_TIME.range                  |
| T10:30:47              | T11:00..T23:59             | rejected | C_TIME.range                  |
| T10:30:47              | T11:00:00..T23:59:59       | rejected | C_TIME.range                  |
| T10:30:47              | T11:00:00.0..T23:59:59.999 | rejected | C_TIME.range                  |
| T10:30:47              | T00..T09                   | rejected | C_TIME.range                  |
| T10:30:47              | T00:00..T09:59             | rejected | C_TIME.range                  |
| T10:30:47              | T00:00:00..T09:59:59       | rejected | C_TIME.range                  |
| T10:30:47              | T00:00:00.0..T09:59:59.999 | rejected | C_TIME.range                  |
| T10:30:47              | >=T00                      | accepted |                               |
| T10:30:47              | >=T00:00                   | accepted |                               |
| T10:30:47              | >=T00:00:00                | accepted |                               |
| T10:30:47              | >=T00:00:00.0              | accepted |                               |
| T10:30:47              | >=T11                      | rejected | C_TIME.range                  |
| T10:30:47              | >=T11:00                   | rejected | C_TIME.range                  |
| T10:30:47              | >=T11:00:00                | rejected | C_TIME.range                  |
| T10:30:47              | >=T11:00:00.0              | rejected | C_TIME.range                  |
| T10:30:47              | <=T09                      | rejected | C_TIME.range                  |
| T10:30:47              | <=T09:59                   | rejected | C_TIME.range                  |
| T10:30:47              | <=T09:59:59                | rejected | C_TIME.range                  |
| T10:30:47              | <=T09:59:59.999            | rejected | C_TIME.range                  |

| value                  | C_TIME.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| T10:30:47.5            | T00..T23                   | accepted |                               |
| T10:30:47.5            | T00:00..T23:59             | accepted |                               |
| T10:30:47.5            | T00:00:00..T23:59:59       | accepted |                               |
| T10:30:47.5            | T00:00:00.0..T23:59:59.999 | accepted |                               |
| T10:30:47.5            | T11..T23                   | rejected | C_TIME.range                  |
| T10:30:47.5            | T11:00..T23:59             | rejected | C_TIME.range                  |
| T10:30:47.5            | T11:00:00..T23:59:59       | rejected | C_TIME.range                  |
| T10:30:47.5            | T11:00:00.0..T23:59:59.999 | rejected | C_TIME.range                  |
| T10:30:47.5            | T00..T09                   | rejected | C_TIME.range                  |
| T10:30:47.5            | T00:00..T09:59             | rejected | C_TIME.range                  |
| T10:30:47.5            | T00:00:00..T09:59:59       | rejected | C_TIME.range                  |
| T10:30:47.5            | T00:00:00.0..T09:59:59.999 | rejected | C_TIME.range                  |
| T10:30:47.5            | >=T00                      | accepted |                               |
| T10:30:47.5            | >=T00:00                   | accepted |                               |
| T10:30:47.5            | >=T00:00:00                | accepted |                               |
| T10:30:47.5            | >=T00:00:00.0              | accepted |                               |
| T10:30:47.5            | >=T11                      | rejected | C_TIME.range                  |
| T10:30:47.5            | >=T11:00                   | rejected | C_TIME.range                  |
| T10:30:47.5            | >=T11:00:00                | rejected | C_TIME.range                  |
| T10:30:47.5            | >=T11:00:00.0              | rejected | C_TIME.range                  |
| T10:30:47.5            | <=T09                      | rejected | C_TIME.range                  |
| T10:30:47.5            | <=T09:59                   | rejected | C_TIME.range                  |
| T10:30:47.5            | <=T09:59:59                | rejected | C_TIME.range                  |
| T10:30:47.5            | <=T09:59:59.999            | rejected | C_TIME.range                  |

| value                  | C_TIME.range                 | expected | violated constraints          |
|------------------------|------------------------------|----------|-------------------------------|
| T10:30:47Z             | T00Z..T23Z                   | accepted |                               |
| T10:30:47Z             | T00:00Z..T23:59Z             | accepted |                               |
| T10:30:47Z             | T00:00:00Z..T23:59:59Z       | accepted |                               |
| T10:30:47Z             | T00:00:00.0Z..T23:59:59.999Z | accepted |                               |
| T10:30:47Z             | T11Z..T23Z                   | rejected | C_TIME.range                  |
| T10:30:47Z             | T11:00Z..T23:59Z             | rejected | C_TIME.range                  |
| T10:30:47Z             | T11:00:00Z..T23:59:59Z       | rejected | C_TIME.range                  |
| T10:30:47Z             | T11:00:00.0Z..T23:59:59.999Z | rejected | C_TIME.range                  |
| T10:30:47Z             | T00Z..T09Z                   | rejected | C_TIME.range                  |
| T10:30:47Z             | T00:00Z..T09:59Z             | rejected | C_TIME.range                  |
| T10:30:47Z             | T00:00:00Z..T09:59:59Z       | rejected | C_TIME.range                  |
| T10:30:47Z             | T00:00:00.0Z..T09:59:59.999Z | rejected | C_TIME.range                  |
| T10:30:47Z             | >=T00Z                       | accepted |                               |
| T10:30:47Z             | >=T00:00Z                    | accepted |                               |
| T10:30:47Z             | >=T00:00:00Z                 | accepted |                               |
| T10:30:47Z             | >=T00:00:00.0Z               | accepted |                               |
| T10:30:47Z             | >=T11Z                       | rejected | C_TIME.range                  |
| T10:30:47Z             | >=T11:00Z                    | rejected | C_TIME.range                  |
| T10:30:47Z             | >=T11:00:00Z                 | rejected | C_TIME.range                  |
| T10:30:47Z             | >=T11:00:00.0Z               | rejected | C_TIME.range                  |
| T10:30:47Z             | <=T09Z                       | rejected | C_TIME.range                  |
| T10:30:47Z             | <=T09:59Z                    | rejected | C_TIME.range                  |
| T10:30:47Z             | <=T09:59:59Z                 | rejected | C_TIME.range                  |
| T10:30:47Z             | <=T09:59:59.999Z             | rejected | C_TIME.range                  |

| value                  | C_TIME.range                 | expected | violated constraints          |
|------------------------|------------------------------|----------|-------------------------------|
| T10:30:47.5Z           | T00Z..T23Z                   | accepted |                               |
| T10:30:47.5Z           | T00:00Z..T23:59Z             | accepted |                               |
| T10:30:47.5Z           | T00:00:00Z..T23:59:59Z       | accepted |                               |
| T10:30:47.5Z           | T00:00:00.0Z..T23:59:59.999Z | accepted |                               |
| T10:30:47.5Z           | T11Z..T23Z                   | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T11:00Z..T23:59Z             | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T11:00:00Z..T23:59:59Z       | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T11:00:00.0Z..T23:59:59.999Z | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T00Z..T09Z                   | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T00:00Z..T09:59Z             | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T00:00:00Z..T09:59:59Z       | rejected | C_TIME.range                  |
| T10:30:47.5Z           | T00:00:00.0Z..T09:59:59.999Z | rejected | C_TIME.range                  |
| T10:30:47.5Z           | >=T00Z                       | accepted |                               |
| T10:30:47.5Z           | >=T00:00Z                    | accepted |                               |
| T10:30:47.5Z           | >=T00:00:00Z                 | accepted |                               |
| T10:30:47.5Z           | >=T00:00:00.0Z               | accepted |                               |
| T10:30:47.5Z           | >=T11Z                       | rejected | C_TIME.range                  |
| T10:30:47.5Z           | >=T11:00Z                    | rejected | C_TIME.range                  |
| T10:30:47.5Z           | >=T11:00:00Z                 | rejected | C_TIME.range                  |
| T10:30:47.5Z           | >=T11:00:00.0Z               | rejected | C_TIME.range                  |
| T10:30:47.5Z           | <=T09Z                       | rejected | C_TIME.range                  |
| T10:30:47.5Z           | <=T09:59Z                    | rejected | C_TIME.range                  |
| T10:30:47.5Z           | <=T09:59:59Z                 | rejected | C_TIME.range                  |
| T10:30:47.5Z           | <=T09:59:59.999Z             | rejected | C_TIME.range                  |

| value                  | C_TIME.range                           | expected | violated constraints          |
|------------------------|----------------------------------------|----------|-------------------------------|
| T10:30:47-03:00        | T00-03:00..T23-03:00                   | accepted |                               |
| T10:30:47-03:00        | T00:00-03:00..T23:59-03:00             | accepted |                               |
| T10:30:47-03:00        | T00:00:00-03:00..T23:59:59-03:00       | accepted |                               |
| T10:30:47-03:00        | T00:00:00.0-03:00..T23:59:59.999-03:00 | accepted |                               |
| T10:30:47-03:00        | T11-03:00..T23-03:00                   | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T11:00-03:00..T23:59-03:00             | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T11:00:00-03:00..T23:59:59-03:00       | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T11:00:00.0-03:00..T23:59:59.999-03:00 | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T00-03:00..T09-03:00                   | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T00:00-03:00..T09:59-03:00             | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T00:00:00-03:00..T09:59:59-03:00       | rejected | C_TIME.range                  |
| T10:30:47-03:00        | T00:00:00.0-03:00..T09:59:59.999-03:00 | rejected | C_TIME.range                  |
| T10:30:47-03:00        | >=T00-03:00                            | accepted |                               |
| T10:30:47-03:00        | >=T00:00-03:00                         | accepted |                               |
| T10:30:47-03:00        | >=T00:00:00-03:00                      | accepted |                               |
| T10:30:47-03:00        | >=T00:00:00.0-03:00                    | accepted |                               |
| T10:30:47-03:00        | >=T11-03:00                            | rejected | C_TIME.range                  |
| T10:30:47-03:00        | >=T11:00-03:00                         | rejected | C_TIME.range                  |
| T10:30:47-03:00        | >=T11:00:00-03:00                      | rejected | C_TIME.range                  |
| T10:30:47-03:00        | >=T11:00:00.0-03:00                    | rejected | C_TIME.range                  |
| T10:30:47-03:00        | <=T09-03:00                            | rejected | C_TIME.range                  |
| T10:30:47-03:00        | <=T09:59-03:00                         | rejected | C_TIME.range                  |
| T10:30:47-03:00        | <=T09:59:59-03:00                      | rejected | C_TIME.range                  |
| T10:30:47-03:00        | <=T09:59:59.999-03:00                  | rejected | C_TIME.range                  |

| value                  | C_TIME.range                           | expected | violated constraints          |
|------------------------|----------------------------------------|----------|-------------------------------|
| T10:30:47.5-03:00      | T00-03:00..T23-03:00                   | accepted |                               |
| T10:30:47.5-03:00      | T00:00-03:00..T23:59-03:00             | accepted |                               |
| T10:30:47.5-03:00      | T00:00:00-03:00..T23:59:59-03:00       | accepted |                               |
| T10:30:47.5-03:00      | T00:00:00.0-03:00..T23:59:59.999-03:00 | accepted |                               |
| T10:30:47.5-03:00      | T11-03:00..T23-03:00                   | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T11:00-03:00..T23:59-03:00             | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T11:00:00-03:00..T23:59:59-03:00       | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T11:00:00.0-03:00..T23:59:59.999-03:00 | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T00-03:00..T09-03:00                   | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T00:00-03:00..T09:59-03:00             | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T00:00:00-03:00..T09:59:59-03:00       | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | T00:00:00.0-03:00..T09:59:59.999-03:00 | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | >=T00-03:00                            | accepted |                               |
| T10:30:47.5-03:00      | >=T00:00-03:00                         | accepted |                               |
| T10:30:47.5-03:00      | >=T00:00:00-03:00                      | accepted |                               |
| T10:30:47.5-03:00      | >=T00:00:00.0-03:00                    | accepted |                               |
| T10:30:47.5-03:00      | >=T11-03:00                            | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | >=T11:00-03:00                         | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | >=T11:00:00-03:00                      | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | >=T11:00:00.0-03:00                    | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | <=T09-03:00                            | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | <=T09:59-03:00                         | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | <=T09:59:59-03:00                      | rejected | C_TIME.range                  |
| T10:30:47.5-03:00      | <=T09:59:59.999-03:00                  | rejected | C_TIME.range                  |


## 4.4. quantity.date_time.DV_DATE

DV_DATE constraints are defined by C_DATE, which specifies two types of constraints: validity kind and range. The validity kind constraints are expressed in terms of mandatory/optional/prohibited flags for each part of the date expression: day and month. The range constraint is an Interval<Date>.

> NOTE 1: the basic and extended formats are allowed at in-memory and storage representations. Basic format being the time parts without any separators and the extended being the parts with separatos `-` (hyphen). Since most popular exchange formats use the extended format, and considering the implementation of these test cases will surelly use those exchange formats, we only specify test data sets which use the extended format.

> NOTE 2: by the ISO8601 standard, only years >1582 are valid, since that was the year in which the Gregorian Calendar was put in place. For representing other years, there should be a mutual agreement between information interchange partners.

### 4.4.1. Test case DV_DATE open constraint

| value                  | expected | violated constraints          |
|------------------------|----------|-------------------------------|
| NULL                   | rejected | RM/Schema: value is mandatory |
| ''                     | rejected | ISO8601: at least year is required <sup id="empty_date">[1](#footnote1)</sup> |
| 2021                   | accepted |                               |
| 2021-10                | accepted |                               |
| 2021-00                | rejected | ISO8601: month in 01..12      |
| 2021-13                | rejected | ISO8601: month in 01..12      |
| 2021-10-24             | accepted |                               |
| 2021-10-00             | rejected | ISO8601: day in 01..31        |
| 2021-10-32             | rejected | ISO8601: day in 01..31        |


<b id="footnote1">1</b>: this is the author's interpretation of a minimal valid date in the context of openEHR noting the description of [C_DATE](https://specifications.openehr.org/releases/AM/Release-2.2.0/AOM1.4.html#_c_date_class): "There is no validity flag for ‘year’, since it must always be by definition mandatory in order to have a sensible date at all.". Though the ISO standard seems to allow partial year expressions. [↩](#empty_date)


### 4.4.2. Test Case DV_DATE validity kind constraint

| value                  | month_validity | day_validity   | expected | violated constraints          |
|------------------------|----------------|----------------|----------|-------------------------------|
| 2021                   | mandatory      | mandatory      | rejected | month_validity, day_validity  |
| 2021                   | mandatory      | optional       | rejected | month_validity                |
| 2021                   | optional       | optional       | accepted |                               |
| 2021                   | mandatory      | prohibited     | rejected | month_validity                |
| 2021                   | prohibited     | prohibited     | accepted |                               |
| 2021-10                | mandatory      | mandatory      | rejected | day_validity                  |
| 2021-10                | mandatory      | optional       | accepted |                               |
| 2021-10                | optional       | optional       | accepted |                               |
| 2021-10                | mandatory      | prohibited     | accepted |                               |
| 2021-10                | prohibited     | prohibited     | rejected | month_validity                |
| 2021-10-24             | mandatory      | mandatory      | accepted |                               |
| 2021-10-24             | mandatory      | optional       | accepted |                               |
| 2021-10-24             | optional       | optional       | accepted |                               |
| 2021-10-24             | mandatory      | prohibited     | rejected | day_validity                  |
| 2021-10-24             | prohibited     | prohibited     | rejected | month_validity, day_validity  |



### 4.4.3. Test Case DV_DATE validity range constraint

The C_DATE.range constraint is an Interval<Date>, which are both [Foundation Types](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html).

> NOTE: the Date class mentioned in the AOM specification is actually the [Iso8601_date](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_time_types) class. This is a [known bug](https://openehr.atlassian.net/browse/SPECPR-380) in the specs.

| value                  | C_DATE.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| 2021                   | 1900..2030                 | accepted |                               |
| 2021                   | 2022..2030                 | rejected | C_DATE.range                  |
| 2021                   | 1900..2020                 | rejected | C_DATE.range                  |

| value                  | C_DATE.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| 2021-10                | 1900-03..2030-07           | accepted |                               |
| 2021-10                | 2022-03..2030-07           | rejected | C_DATE.range                  |
| 2021-10                | 1900-03..2020-07           | rejected | C_DATE.range                  |

| value                  | C_DATE.range               | expected | violated constraints          |
|------------------------|----------------------------|----------|-------------------------------|
| 2021-10-24             | 1900-03-13..2030-07-09     | accepted |                               |
| 2021-10-24             | 2022-03-13..2030-07-09     | rejected | C_DATE.range                  |
| 2021-10-24             | 1900-03-13..2020-07-09     | rejected | C_DATE.range                  |

> NOTE: the DV_DATE value and the C_DATE.range limits should be comparable, that means the value and range limits should have the same components, for instance a year-only date 2021 can't be compared to a year+month date like 2021-10, because 2021 refers to a whole year, and 2021-10 refers to a month in that year, but it's not possible to say if 2021 < 2021-10 or 2021 > 2021-10, since both are refering to different things. What we could say is 2020 < 2021, and 2021-10 < 2021-11.


## 4.5. quantity.date_time.DV_DATE_TIME

DV_DATE_TIME constraints are defined by C_DATE_TIME, which specifies two types of constraints: validity kind and range. The validity kind constraints are expressed in terms of mandatory/optional/prohibited flags for each part of the date expression: hour, minute, second, millisecond, timezone, day and month. The range constraint is an Interval<DateTime>.

### 4.5.1. Test case DV_DATE_TIME open constraint

| value                            | expected | violated constraints               |
|----------------------------------|----------|------------------------------------|
| NULL                             | rejected | RM/Schema: value is mandatory      |
| ''                               | rejected | ISO8601: at least year is required |
| 2021                             | accepted |                                    |
| 2021-10                          | accepted |                                    |
| 2021-00                          | rejected | ISO8601: month in 01..12           |
| 2021-13                          | rejected | ISO8601: month in 01..12           |
| 2021-10-24                       | accepted |                                    |
| 2021-10-00                       | rejected | ISO8601: day in 01..31             |
| 2021-10-32                       | rejected | ISO8601: day in 01..31             |
| 2021-10-24T10                    | accepted |                                    |
| 2021-10-24T48                    | rejected | ISO8601: hours in 0..23            |
| 2021-10-24T10:30                 | accepted |                                    |
| 2021-10-24T10:95                 | rejected | ISO8601: minutes in 0..59          |
| 2021-10-24T10:30:47              | accepted |                                    |
| 2021-10-24T10:30:78              | rejected | ISO8601: seconds in 0..59          |
| 2021-10-24T10:30:47.5            | accepted |                                    |
| 2021-10-24T10:30:47.333          | accepted |                                    |
| 2021-10-24T10:30:47.333333       | accepted |                                    |
| 2021-10-24T10:30:47Z             | accepted |                                    |
| 2021-10-24T10:30:78Z             | rejected | ISO8601: seconds in 0..59          |
| 2021-10-24T10:30:47.5Z           | accepted |                                    |
| 2021-10-24T10:30:47.333Z         | accepted |                                    |
| 2021-10-24T10:30:47.333333Z      | accepted |                                    |
| 2021-10-24T10:30:47-03:00        | accepted |                                    |
| 2021-10-24T10:30:78-03:00        | rejected | ISO8601: seconds in 0..59          |
| 2021-10-24T10:30:47.5-03:00      | accepted |                                    |
| 2021-10-24T10:30:47.333-03:00    | accepted |                                    |
| 2021-10-24T10:30:47.333333-03:00 | accepted |                                    |

> NOTE: to verify the date time expressiosn we used this [regex](https://regex101.com/r/pYQJaW/1), you will note the rejected values don't match the regex.


### 4.5.2. Test Case DV_DATE_TIME validity kind constraint

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity |
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, millisecond_validity |
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity   |
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | rejected | month_validity, day_validity, hour_validity, minute_validity                    |
| 2021                   | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | rejected | month_validity, day_validity, hour_validity      |
| 2021                   | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | rejected | month_validity, day_validity                     |
| 2021                   | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | rejected | month_validity                                   |
| 2021                   | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                                  |
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, millisecond_validity |
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity   |
| 2021                   | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity                    |
| 2021                   | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity                    |
| 2021                   | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity                                   |
| 2021                   | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity                                                 |
| 2021                   | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | accepted |                                                                |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | day_validity, hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity |
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | day_validity, hour_validity, minute_validity, second_validity, millisecond_validity |
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | rejected | day_validity, hour_validity, minute_validity, second_validity   |
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | rejected | day_validity, hour_validity, minute_validity                    |
| 2021-10                | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | rejected | day_validity, hour_validity        |
| 2021-10                | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | rejected | day_validity                       |
| 2021-10                | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                    |
| 2021-10                | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                                  |
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity, millisecond_validity |
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity   |
| 2021-10                | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity                    |
| 2021-10                | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity                    |
| 2021-10                | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity                                   |
| 2021-10                | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | accepted |                                                |
| 2021-10                | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity                                 |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | hour_validity, minute_validity, second_validity, millisecond_validity |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | rejected | hour_validity, minute_validity, second_validity |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | rejected | hour_validity, minute_validity                  |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | rejected | hour_validity                      |
| 2021-10-24             | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                    |
| 2021-10-24             | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                    |
| 2021-10-24             | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                    |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | hour_validity, minute_validity, second_validity, millisecond_validity |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity                  |
| 2021-10-24             | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity                                   |
| 2021-10-24             | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | accepted |                                                 |
| 2021-10-24             | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity                                    |
| 2021-10-24             | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity                    |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | minute_validity, second_validity, millisecond_validity, timezone_validity |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | minute_validity, second_validity, millisecond_validity |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | rejected | minute_validity, second_validity            |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | rejected | minute_validity                             |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10          | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10          | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10          | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | minute_validity, second_validity, millisecond_validity |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | minute_validity, second_validity            |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | minute_validity                             |
| 2021-10-24T10          | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | accepted |                                             |
| 2021-10-24T10          | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity                               |
| 2021-10-24T10          | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity                 |
| 2021-10-24T10          | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints               |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|------------------------------------|
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | second_validity, millisecond_validity, timezone_validity |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | second_validity, millisecond_validity |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | rejected | second_validity            |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                            |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30       | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30       | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30       | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | second_validity, millisecond_validity       |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | second_validity                             |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | accepted |                                             |
| 2021-10-24T10:30       | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity                                        |
| 2021-10-24T10:30       | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity                         |
| 2021-10-24T10:30       | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity           |
| 2021-10-24T10:30       | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints                        |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|---------------------------------------------|
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | millisecond_validity, timezone_validity     |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47    | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47    | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | accepted |                                             |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity                             |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity                                              |
| 2021-10-24T10:30:47    | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity                               |
| 2021-10-24T10:30:47    | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity                 |
| 2021-10-24T10:30:47    | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints                        |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|---------------------------------------------|
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | timezone_validity                           |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | accepted |                                             |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, millisecond_validity       |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, millisecond_validity                                              |
| 2021-10-24T10:30:47.5  | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity, millisecond_validity                               |
| 2021-10-24T10:30:47.5  | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity, millisecond_validity                 |
| 2021-10-24T10:30:47.5  | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, millisecond_validity |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints                        |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|---------------------------------------------|
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | timezone_validity                                              |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | millisecond_validity, timezone_validity                        |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, millisecond_validity, timezone_validity       |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, millisecond_validity, timezone_validity                                              |
| 2021-10-24T10:30:47.5Z | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity                               |
| 2021-10-24T10:30:47.5Z | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity                 |
| 2021-10-24T10:30:47.5Z | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity |

| value                  | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints                        |
|------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|---------------------------------------------|
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47Z   | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47Z   | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | millisecond_validity, timezone_validity     |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | timezone_validity                           |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, timezone_validity          |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, timezone_validity                                              |
| 2021-10-24T10:30:47Z   | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity, timezone_validity                               |
| 2021-10-24T10:30:47Z   | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity, timezone_validity                 |
| 2021-10-24T10:30:47Z   | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, timezone_validity |

| value                       | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints                        |
|-----------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|---------------------------------------------|
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | timezone_validity                                              |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | millisecond_validity, timezone_validity                        |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, millisecond_validity, timezone_validity       |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, millisecond_validity, timezone_validity                                              |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity                               |
| 2021-10-24T10:30:47.5-03:00 | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity                 |
| 2021-10-24T10:30:47.5-03:00 | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, millisecond_validity, timezone_validity |

| value                     | month_validity | day_validity | hour_validity | minute_validity | second_validity | millisecond_validity | timezone_validity | expected | violated constraints                        |
|---------------------------|----------------|--------------|---------------|-----------------|-----------------|----------------------|-------------------|----------|---------------------------------------------|
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | mandatory         | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | optional          | rejected | millisecond_validity                        |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47-03:00 | mandatory      | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47-03:00 | optional       | optional     | optional      | optional        | optional        | optional             | optional          | accepted |                                             |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | mandatory            | prohibited        | rejected | millisecond_validity, timezone_validity     |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | mandatory       | prohibited           | prohibited        | rejected | timezone_validity                           |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | mandatory       | prohibited      | prohibited           | prohibited        | rejected | second_validity, timezone_validity          |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | mandatory     | prohibited      | prohibited      | prohibited           | prohibited        | rejected | minute_validity, second_validity, timezone_validity                                              |
| 2021-10-24T10:30:47-03:00 | mandatory      | mandatory    | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | hour_validity, minute_validity, second_validity, timezone_validity                               |
| 2021-10-24T10:30:47-03:00 | mandatory      | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | day_validity, hour_validity, minute_validity, second_validity, timezone_validity                 |
| 2021-10-24T10:30:47-03:00 | prohibited     | prohibited   | prohibited    | prohibited      | prohibited      | prohibited           | prohibited        | rejected | month_validity, day_validity, hour_validity, minute_validity, second_validity, timezone_validity |


### 4.5.3. Test Case DV_DATE_TIME validity range

The C_DATE_TIME.range constraint is an Interval<Date_time>, which are both [Foundation Types](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html).

> NOTE: the Date_time class mentioned in the AOM specification is actually the [Iso8601_date_time](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_time_types) class. This is a [known bug](https://openehr.atlassian.net/browse/SPECPR-380) in the specs.


| value                  | C_DATE_TIME.range               | expected | violated constraints          |
|------------------------|---------------------------------|----------|-------------------------------|
| 2021                   | 1900..2030                      | accepted |                               |
| 2021                   | 2022..2030                      | rejected | C_DATE_TIME.range             |
| 2021                   | 1900..2020                      | rejected | C_DATE_TIME.range             |

| value                  | C_DATE_TIME.range               | expected | violated constraints          |
|------------------------|---------------------------------|----------|-------------------------------|
| 2021-10                | 1900-03..2030-07                | accepted |                               |
| 2021-10                | 2022-03..2030-07                | rejected | C_DATE_TIME.range             |
| 2021-10                | 1900-03..2020-07                | rejected | C_DATE_TIME.range             |

| value                  | C_DATE_TIME.range               | expected | violated constraints          |
|------------------------|---------------------------------|----------|-------------------------------|
| 2021-10-24             | 1900-03-13..2030-07-09          | accepted |                               |
| 2021-10-24             | 2022-03-13..2030-07-09          | rejected | C_DATE_TIME.range             |
| 2021-10-24             | 1900-03-13..2020-07-09          | rejected | C_DATE_TIME.range             |


TBD: there is an open question about strictly comparability between time expressions with different components. Is "T10" comparable to "T00:00"?

| value                  | C_DATE_TIME.range                              | expected | violated constraints          |
|------------------------|------------------------------------------------|----------|-------------------------------|
| 2021-10-24T10          | 1900-03-13T00..1900-03-13T23                   | accepted |                               |
| 2021-10-24T10          | 1900-03-13T00:00..1900-03-13T23:59             | accepted |                               |
| 2021-10-24T10          | 1900-03-13T00:00:00..1900-03-13T23:59:59       | accepted |                               |
| 2021-10-24T10          | 1900-03-13T00:00:00.0..1900-03-13T23:59:59.999 | accepted |                               |
| 2021-10-24T10          | 1900-03-13T11..1900-03-13T23                   | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T11:00..1900-03-13T23:59             | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T11:00:00..1900-03-13T23:59:59       | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T11:00:00.0..1900-03-13T23:59:59.999 | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T00..1900-03-13T09                   | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T00:00..1900-03-13T09:59             | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T00:00:00..1900-03-13T09:59:59       | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | 1900-03-13T00:00:00.0..1900-03-13T09:59:59.999 | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | >=1900-03-13T00                                | accepted |                               |
| 2021-10-24T10          | >=1900-03-13T00:00                             | accepted |                               |
| 2021-10-24T10          | >=1900-03-13T00:00:00                          | accepted |                               |
| 2021-10-24T10          | >=1900-03-13T00:00:00.0                        | accepted |                               |
| 2021-10-24T10          | >=1900-03-13T11                                | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | >=1900-03-13T11:00                             | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | >=1900-03-13T11:00:00                          | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | >=1900-03-13T11:00:00.0                        | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | <=1900-03-13T09                                | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | <=1900-03-13T09:59                             | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | <=1900-03-13T09:59:59                          | rejected | C_DATE_TIME.range                  |
| 2021-10-24T10          | <=1900-03-13T09:59:59.999                      | rejected | C_DATE_TIME.range                  |


# 5. time_specification

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.time_specification.svg)

## DV_GENERAL_TIME_SPECIFICATION

## DV_PERIODIC_TIME_SPECIFICATION



# 6. encapsulated

## 6.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.encapsulated.svg)

## 6.2. encapsulated.DV_PARSABLE

### 6.2.1. Test case DV_PARSABLE open constraint

| value     | formalism   | expected | violated constraints |
|-----------|-------------|----------|----------------------|
| NULL      | NULL        | rejected | RM/schema value and formalism are required |
| abc       | NULL        | rejected | RM/schema formalism is required |
| NULL      | abc         | rejected | RM/schema value is required |
| xxx       | abc         | accepted |  |

### 6.2.2. Test case DV_PARSABLE value and formalism constrained

Each field of the DV_PARSABLE could be constrained by a C_STRING.

| value     | formalism   | C_STRING.pattern (value) | C_STRING.list (value) | C_STRING.pattern (formalism) | C_STRING.list (formalism) | expected | violated constraints |
|-----------|-------------|--------------------------|-----------------------|------------------------------|---------------------------|----------|----------------------|
| xxx       | abc         | x*                       | NULL                  | abc                          | NULL                      | accepted |  |
| xxx       | abc         | a*                       | NULL                  | abc                          | NULL                      | rejected | C_STRING.pattern (value) |
| xxx       | abc         | x*                       | NULL                  | x*                           | NULL                      | rejected | C_STRING.pattern (formalism) |
| xxx       | abc         | NULL                     | [xxx, yyy, zzz]       | abc                          | NULL                      | accepted |  |
| xxx       | abc         | NULL                     | [yyy, zzz]            | abc                          | NULL                      | rejected | C_STRING.list (value) |
| xxx       | abc         | NULL                     | [xxx, yyy, zzz]       | NULL                         | [abc, bbb, aaa]           | accepted |  |
| xxx       | abc         | NULL                     | [xxx, yyy, zzz]       | NULL                         | [bbb, aaa]                | rejected | C_STRING.list (formalism) |


## 6.3. encapsulated.DV_MULTIMEDIA

### 6.3.1. Test ccase DV_MULTIMEDIA open constraint

| media_type        | size        | expected | violated constraints |
|-------------------|-------------|----------|----------------------|
| NULL              | NULL        | rejected | RM/schema media_type and size are required |
| abc               | NULL        | rejected | media_type is not in the media type openEHR term set, RM/schema size is required |
| NULL              | 123         | rejected | RM/schema media_type is required |
| application/dicom | 123         | accepted |  |


### 6.3.2. Test case DV_MULTIMEDIA media type constraint

NOTE: media_type could be constrained by a C_CODE_PHRASE and size could be constrained by C_INTEGER. A NULL C_CODE_PHRASE for the media_type means any code is allowed from the openEHR media type codeset https://github.com/openEHR/terminology/blob/master/openEHR_RM/openehr_external_terminologies.xml#L399

| media_type        | size | C_CODE_PHRASE                              | C_INTEGER.list  | C_INTEGER.range | expected | violated constraints           |
|-------------------|------|--------------------------------------------|-----------------|-----------------|----------|--------------------------------|
| application/dicom | 123  | NULL                                       | [10, 100, 1000] | NULL            | rejected | C_INTEGER.list                 |
| application/dicom | 100  | NULL                                       | [10, 100, 1000] | NULL            | accepted |                                |
| application/dicom | 123  | NULL                                       | NULL            | 0..1000         | accepted |                                |
| application/dicom | 123  | NULL                                       | NULL            | 200..1000       | rejected | C_INTEGER.range                |
| application/dicom | 100  | [application/dicom, text/plain, text/html] | [10, 100, 1000] | NULL            | accepted |                                |
| application/dicom | 100  | [text/plain, text/html]                    | [10, 100, 1000] | NULL            | rejected | C_CODE_PHRASE                  |
| application/dicom | 100  | [application/dicom, text/plain, text/html] | NULL            | 0..1000         | accepted |                                |
| application/dicom | 100  | [text/plain, text/html]                    | NULL            | 200..1000       | rejected | C_CODE_PHRASE, C_INTEGER.range |


# 7. uri

## 7.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.uri.svg)

## 7.2. DV_URI

### 7.2.1. Test case DV_URI open constraint

On this test case, only invalid URIs should be rejected. Any RFC3986-compliant URI should be accepted.

| value                                               | expected | violated constraints         |
|-----------------------------------------------------|----------|------------------------------|
| NULL                                                | rejected | RM/schema: value is required  |
| xyz                                                 | rejected | value doesn't comply with RFC3986 |
| ftp://ftp.is.co.za/rfc/rfc1808.txt                  | accepted |                              |
| http://www.ietf.org/rfc/rfc2396.txt                 | accepted |                              |
| ldap://[2001:db8::7]/c=GB?objectClass?one           | accepted |                              |
| mailto:John.Doe@example.com                         | accepted |                              |
| news:comp.infosystems.www.servers.unix              | accepted |                              |
| tel:+1-816-555-1212                                 | accepted |                              |
| telnet://192.0.2.16:80/                             | accepted |                              |
| urn:oasis:names:specification:docbook:dtd:xml:4.1.2 | accepted |                              |
| http://www.carestreamserver/um/webapp_services/wado?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&contentType=application%2Fdicom | accepted | |


### 7.2.2. Test case DV_URI C_STRING constraint for value

TBD: need to check what happens if the constraint is not compliant with the URI format, for instance when defining a pattern in the C_STRING.



## 7.3. DV_EHR_URI

### 7.3.1. Test case DV_EHR_URI open constraint

| value                                               | expected | violated constraints | notes |
|-----------------------------------------------------|----------|----------------------|-------|
| NULL                                                | rejected | RM/schema: value is required | |
| xyz                                                 | rejected | value doesn't comply with RFC3986 | |
| ftp://ftp.is.co.za/rfc/rfc1808.txt                  | rejected | URI doesn't have schema = 'ehr'   | |
| http://www.ietf.org/rfc/rfc2396.txt                 | rejected | URI doesn't have schema = 'ehr'   | |
| ldap://[2001:db8::7]/c=GB?objectClass?one           | rejected | URI doesn't have schema = 'ehr'   | |
| mailto:John.Doe@example.com                         | rejected | URI doesn't have schema = 'ehr'   | |
| news:comp.infosystems.www.servers.unix              | rejected | URI doesn't have schema = 'ehr'   | |
| tel:+1-816-555-1212                                 | rejected | URI doesn't have schema = 'ehr'   | |
| telnet://192.0.2.16:80/                             | rejected | URI doesn't have schema = 'ehr'   | |
| urn:oasis:names:specification:docbook:dtd:xml:4.1.2 | rejected | URI doesn't have schema = 'ehr'   | |
| http://www.carestreamserver/um/webapp_services/wado?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&contentType=application%2Fdicom | rejected | URI doesn't have schema = 'ehr' |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a           | accepted | | This should be a valid reference to an EHR |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1 | accepted | | This should be a valid reference to a COMPOSITION or FOLDER in an EHR (some top-level VERSIONED_OBJECT) |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1/context/other_context[at0001]/items[archetype_id=openEHR-EHR-CLUSTER.sample_symptom.v1]/items[at0034]/items[at0021]/value | accepted | | This should be a valid reference to a DATA_VALUE node in a COMPOSITION from an EHR |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a           | accepted | | Similar to the examples above, with given system_id as the URI `authority` |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1 | accepted | | Similar to the examples above, with given system_id as the URI `authority` |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1/context/other_context[at0001]/items[archetype_id=openEHR-EHR-CLUSTER.sample_symptom.v1]/items[at0034]/items[at0021]/value | accepted | | Similar to the examples above, with given system_id as the URI `authority` |



### 7.3.2. Test case DV_EHR_URI C_STRING constraint for value

TBD: need to check what happens when the constriant is not compliant with the definition of the DV_EHR_URI which requires the scheme to be `ehr`.




