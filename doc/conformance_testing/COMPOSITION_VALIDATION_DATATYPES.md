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


## 3.4. quantity.DV_COUNT

Internally this type is constrained by a C_INTEGER which could contain a range or a list of values.

### 3.4.1. Test case DV_COUNT open constraint

This case represents the DV_COUNT matching {*}, in this case the C_INTEGER is not present in the OPT.

| magnitude      | C_INTEGER.range | C_INTEGER.list    | expected | constraints violated |
|:---------------|:----------------|-------------------|----------|----------------------|
| NULL           | NULL            | NULL              | rejected | RM/Schema magnitude is mandatory |
| 0              | NULL            | NULL              | accepted |  |
| 1              | NULL            | NULL              | accepted |  |
| 15             | NULL            | NULL              | accepted |  |
| 30             | NULL            | NULL              | accepted |  |

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

| magnitude | units | C_DV_QUANTITY.property | C_DV_QUANTITY.list    | expected | constraints violated |
|:----------|:------|:-----------------------|-------------------|----------|----------------------|
| NULL      | NULL  | NULL                   | NULL              | rejected | RM/Schema both magnitude and untis are mandatory |
| NULL      | cm    | NULL                   | NULL              | rejected | RM/Schema magnitude is mandatory |
| 1.0       | NULL  | NULL                   | NULL              | rejected | RM/Schema untis is mandatory |
| 0.0       | cm    | NULL                   | NULL              | accepted |  |
| 1.0       | cm    | NULL                   | NULL              | accepted |  |
| 5.7       | cm    | NULL                   | NULL              | accepted |  |
| 10.0      | cm    | NULL                   | NULL              | accepted |  |


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


## 3.7. quantity.DV_INTERVAL



# 4. quantity.date_time

## 4.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.date_time.svg)

## 4.2. quantity.date_time.DV_DURATION

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



