Composition Datatyps Validation (Conformance Testing)
--------------------------------------------------------

| Author                                           | Version          | Comments                                                       |
|:------------------------------------------------:|:----------------:|:--------------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2021-11-03) | First version                                                  |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2022-05-21) | Improved test cases and data sets for date/time/duration types |
||||

# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [Index](#index)
- [General notes](#general-notes)
- [1. basic](#1-basic)
  - [1.1. Reference UML](#11-reference-uml)
  - [1.2. basic.DV_BOOLEAN](#12-basicdv_boolean)
    - [1.2.1. Test case anything allowed](#121-test-case-anything-allowed)
    - [1.2.2. Test case only true allowed](#122-test-case-only-true-allowed)
    - [1.2.3. Test case only false allowed](#123-test-case-only-false-allowed)
  - [1.3. basic.DV_IDENTIFIER](#13-basicdv_identifier)
    - [1.3.1. Test case validating all attributes using the pattern constraint](#131-test-case-validating-all-attributes-using-the-pattern-constraint)
    - [1.3.2. Test case validating all attributes using the list constraint](#132-test-case-validating-all-attributes-using-the-list-constraint)
  - [1.3. basic.DV_STATE](#13-basicdv_state)
- [2. text](#2-text)
  - [2.1. Reference UML](#21-reference-uml)
  - [2.2. text.DV_TEXT](#22-textdv_text)
    - [2.2.1. Test case DV_TEXT with open constraint](#221-test-case-dv_text-with-open-constraint)
    - [2.2.2. Test case DV_TEXT with pattern constraint](#222-test-case-dv_text-with-pattern-constraint)
    - [2.2.3. Test case DV_TEXT with list constraint](#223-test-case-dv_text-with-list-constraint)
  - [2.3. text.DV_CODED_TEXT](#23-textdv_coded_text)
    - [2.3.1. Test case DV_CODED_TEXT with open constraint](#231-test-case-dv_coded_text-with-open-constraint)
    - [2.3.2. Test case DV_CODED_TEXT with local codes](#232-test-case-dv_coded_text-with-local-codes)
    - [2.3.3. Test case DV_CODED_TEXT with external terminology (constraint reference)](#233-test-case-dv_coded_text-with-external-terminology-constraint-reference)
  - [2.4. text.DV_PARAGRAPH](#24-textdv_paragraph)
- [3. quantity](#3-quantity)
  - [3.1. Reference UML](#31-reference-uml)
  - [3.2. quantity.DV_ORDINAL](#32-quantitydv_ordinal)
    - [3.2.1. Test case DV_ORDINAL open constraint](#321-test-case-dv_ordinal-open-constraint)
    - [3.2.2. Test case DV_ORDINAL with constraints](#322-test-case-dv_ordinal-with-constraints)
  - [3.3. quantity.DV_SCALE](#33-quantitydv_scale)
    - [3.3.1. Test case DV_SCALE open constraint](#331-test-case-dv_scale-open-constraint)
    - [3.3.2. Test case DV_SCALE with constraints](#332-test-case-dv_scale-with-constraints)
  - [3.4. quantity.DV_COUNT](#34-quantitydv_count)
    - [3.4.1. Test case DV_COUNT open constraint](#341-test-case-dv_count-open-constraint)
    - [3.4.2. Test case DV_COUNT range constraint](#342-test-case-dv_count-range-constraint)
    - [3.4.3. Test case DV_COUNT list constraint](#343-test-case-dv_count-list-constraint)
  - [3.5. quantity.DV_QUANTITY](#35-quantitydv_quantity)
    - [3.5.1. Test case DV_QUANTITY open constraint](#351-test-case-dv_quantity-open-constraint)
    - [3.5.2. Test case DV_QUANTITY only property is constrained](#352-test-case-dv_quantity-only-property-is-constrained)
    - [3.5.3. Test case DV_QUANTITY property and units are constrained, without magnitude range](#353-test-case-dv_quantity-property-and-units-are-constrained-without-magnitude-range)
    - [3.5.4. Test case DV_QUANTITY property and units are constrained, with magnitude range](#354-test-case-dv_quantity-property-and-units-are-constrained-with-magnitude-range)
  - [3.6. quantity.DV_PROPORTION](#36-quantitydv_proportion)
    - [3.6.1. Test case DV_PROPORTION open constraint, validate RM rules](#361-test-case-dv_proportion-open-constraint-validate-rm-rules)
    - [3.6.2. Test case DV_PROPORTION ratio](#362-test-case-dv_proportion-ratio)
    - [3.6.3. Test case DV_PROPORTION unitary](#363-test-case-dv_proportion-unitary)
    - [3.6.4. Test case DV_PROPORTION percent](#364-test-case-dv_proportion-percent)
    - [3.6.5. Test case DV_PROPORTION fraction](#365-test-case-dv_proportion-fraction)
    - [3.6.6. Test case DV_PROPORTION integer fraction](#366-test-case-dv_proportion-integer-fraction)
    - [3.6.7. Test case DV_PROPORTION fraction or integer fraction](#367-test-case-dv_proportion-fraction-or-integer-fraction)
    - [3.6.8. Test case DV_PROPORTION ratio with range limits](#368-test-case-dv_proportion-ratio-with-range-limits)
  - [3.7. quantity.DV_INTERVAL<DV_COUNT>](#37-quantitydv_intervaldv_count)
    - [3.7.1. Test case DV_INTERVAL<DV_COUNT> open constraint](#371-test-case-dv_intervaldv_count-open-constraint)
    - [3.7.2. Test case DV_INTERVAL<DV_COUNT> lower and upper range constraint.](#372-test-case-dv_intervaldv_count-lower-and-upper-range-constraint)
    - [3.7.3. Test case DV_INTERVAL<DV_COUNT> lower and upper list constraint.](#373-test-case-dv_intervaldv_count-lower-and-upper-list-constraint)
  - [3.8. quantity.DV_INTERVAL<DV_QUANTITY>](#38-quantitydv_intervaldv_quantity)
    - [3.8.1. Test case DV_INTERVAL<DV_QUANTITY> open constraint](#381-test-case-dv_intervaldv_quantity-open-constraint)
    - [3.8.2. Test case DV_INTERVAL<DV_QUANTITY> lower and upper constraints present](#382-test-case-dv_intervaldv_quantity-lower-and-upper-constraints-present)
  - [3.9. quantity.DV_INTERVAL<DV_DATE_TIME>](#39-quantitydv_intervaldv_date_time)
    - [3.9.1. Test case DV_INTERVAL<DV_DATE_TIME> open constraint](#391-test-case-dv_intervaldv_date_time-open-constraint)
    - [3.9.2. Test case DV_INTERVAL<DV_DATE_TIME> lower and upper constraints are validity kind](#392-test-case-dv_intervaldv_date_time-lower-and-upper-constraints-are-validity-kind)
    - [3.9.3. Test case DV_INTERVAL<DV_DATE_TIME> lower and upper constraints are range](#393-test-case-dv_intervaldv_date_time-lower-and-upper-constraints-are-range)
  - [3.10. quantity.DV_INTERVAL<DV_DATE>](#310-quantitydv_intervaldv_date)
    - [3.10.1. Test case DV_INTERVAL<DV_DATE> open constraint](#3101-test-case-dv_intervaldv_date-open-constraint)
    - [3.10.2. Test case DV_INTERVAL<DV_DATE> validity kind constraint](#3102-test-case-dv_intervaldv_date-validity-kind-constraint)
    - [3.10.3. Test case DV_INTERVAL<DV_DATE> range constraint](#3103-test-case-dv_intervaldv_date-range-constraint)
  - [3.11. quantity.DV_INTERVAL<DV_TIME>](#311-quantitydv_intervaldv_time)
    - [3.11.1. Test case DV_INTERVAL<DV_TIME> open constraint](#3111-test-case-dv_intervaldv_time-open-constraint)
    - [3.11.2. Test case DV_INTERVAL<DV_TIME> validity kind constraint](#3112-test-case-dv_intervaldv_time-validity-kind-constraint)
    - [3.11.3. Test case DV_INTERVAL<DV_TIME> range constraint](#3113-test-case-dv_intervaldv_time-range-constraint)
  - [3.12. quantity.DV_INTERVAL<DV_DURATION>](#312-quantitydv_intervaldv_duration)
    - [3.12.1. Test case DV_INTERVAL<DV_DURATION> open constraint](#3121-test-case-dv_intervaldv_duration-open-constraint)
    - [3.12.2. Test case DV_INTERVAL<DV_DURATION> xxx_allowed constraints](#3122-test-case-dv_intervaldv_duration-xxx_allowed-constraints)
    - [3.12.3. Test case DV_INTERVAL<DV_DURATION> range constraints](#3123-test-case-dv_intervaldv_duration-range-constraints)
  - [3.13. quantity.DV_INTERVAL<DV_ORDINAL>](#313-quantitydv_intervaldv_ordinal)
    - [3.13.1. Test case DV_INTERVAL<DV_ORDINAL> open constraint](#3131-test-case-dv_intervaldv_ordinal-open-constraint)
    - [3.13.2. Test case DV_INTERVAL<DV_ORDINAL> with constraints](#3132-test-case-dv_intervaldv_ordinal-with-constraints)
  - [3.14. quantity.DV_INTERVAL<DV_SCALE>](#314-quantitydv_intervaldv_scale)
    - [3.14.1. Test case DV_SCALE open constraint](#3141-test-case-dv_scale-open-constraint)
    - [3.14.2. Test case DV_SCALE with constraints](#3142-test-case-dv_scale-with-constraints)
  - [3.15. quantity.DV_INTERVAL<DV_PROPORTION>](#315-quantitydv_intervaldv_proportion)
    - [3.15.1. Test case DV_INTERVAL<DV_PROPORTION> open constraint](#3151-test-case-dv_intervaldv_proportion-open-constraint)
      - [3.15.1.a. Data set both valid ratios](#3151a-data-set-both-valid-ratios)
      - [3.15.1.b. Data set different limit types](#3151b-data-set-different-limit-types)
      - [3.15.1.c. Data set greater lower](#3151c-data-set-greater-lower)
    - [3.15.2. Test case DV_INTERVAL<DV_PROPORTION> ratios](#3152-test-case-dv_intervaldv_proportion-ratios)
      - [3.15.2.a. Data set valid ratios](#3152a-data-set-valid-ratios)
      - [3.15.2.b. Data set no ratios](#3152b-data-set-no-ratios)
    - [3.15.3. Test case DV_INTERVAL<DV_PROPORTION> unitaries](#3153-test-case-dv_intervaldv_proportion-unitaries)
      - [3.15.3.a. Data set valid unitaries](#3153a-data-set-valid-unitaries)
      - [3.15.3.b. Data set no unitaries](#3153b-data-set-no-unitaries)
    - [3.15.4. Test case DV_INTERVAL<DV_PROPORTION> percentages](#3154-test-case-dv_intervaldv_proportion-percentages)
      - [3.15.4.a. Data set valid percentages](#3154a-data-set-valid-percentages)
      - [3.15.4.b. Data set no percentages](#3154b-data-set-no-percentages)
    - [3.15.5. Test case DV_INTERVAL<DV_PROPORTION> fractions](#3155-test-case-dv_intervaldv_proportion-fractions)
      - [3.15.5.a. Data set valid fractions](#3155a-data-set-valid-fractions)
      - [3.15.5.b. Data set no fractions](#3155b-data-set-no-fractions)
    - [3.15.6. Test case DV_INTERVAL<DV_PROPORTION> integer fractions](#3156-test-case-dv_intervaldv_proportion-integer-fractions)
      - [3.15.6.a. Data set valid integer fractions](#3156a-data-set-valid-integer-fractions)
      - [3.15.6.b. Data set no integer fractions](#3156b-data-set-no-integer-fractions)
    - [3.15.7. Test case DV_INTERVAL<DV_PROPORTION> ratios with range limits](#3157-test-case-dv_intervaldv_proportion-ratios-with-range-limits)
      - [3.15.7.a. Data set valid ratios](#3157a-data-set-valid-ratios)
      - [3.15.7.b. Data set ratios, invalid lower](#3157b-data-set-ratios-invalid-lower)
      - [3.15.7.c. Data set ratios, invalid upper](#3157c-data-set-ratios-invalid-upper)
- [4. quantity.date_time](#4-quantitydate_time)
  - [4.1. Reference UML](#41-reference-uml)
  - [4.2. quantity.date_time.DV_DURATION](#42-quantitydate_timedv_duration)
    - [4.2.1. Test case DV_DURATION open constraint](#421-test-case-dv_duration-open-constraint)
    - [4.2.2. Test case DV_DURATION xxx_allowed field constraints](#422-test-case-dv_duration-xxx_allowed-field-constraints)
    - [4.2.3. Test case DV_DURATION range constraint](#423-test-case-dv_duration-range-constraint)
    - [4.2.4. Test case DV_DURATION fields allowed and range constraints combined](#424-test-case-dv_duration-fields-allowed-and-range-constraints-combined)
  - [4.3. quantity.date_time.DV_TIME](#43-quantitydate_timedv_time)
    - [4.3.1. Test case DV_TIME open constraint](#431-test-case-dv_time-open-constraint)
    - [4.3.2. Test case DV_TIME validity kind constraint](#432-test-case-dv_time-validity-kind-constraint)
    - [4.3.3. Test case DV_TIME range constraint](#433-test-case-dv_time-range-constraint)
  - [4.4. quantity.date_time.DV_DATE](#44-quantitydate_timedv_date)
    - [4.4.1. Test case DV_DATE open constraint](#441-test-case-dv_date-open-constraint)
    - [4.4.2. Test Case DV_DATE validity kind constraint](#442-test-case-dv_date-validity-kind-constraint)
    - [4.4.3. Test Case DV_DATE validity range constraint](#443-test-case-dv_date-validity-range-constraint)
  - [4.5. quantity.date_time.DV_DATE_TIME](#45-quantitydate_timedv_date_time)
    - [4.5.1. Test case DV_DATE_TIME open constraint](#451-test-case-dv_date_time-open-constraint)
    - [4.5.2. Test Case DV_DATE_TIME validity kind constraint](#452-test-case-dv_date_time-validity-kind-constraint)
    - [4.5.3. Test Case DV_DATE_TIME validity range](#453-test-case-dv_date_time-validity-range)
- [5. time_specification](#5-time_specification)
  - [Reference UML](#reference-uml)
  - [5.1. DV_GENERAL_TIME_SPECIFICATION](#51-dv_general_time_specification)
  - [5.2. DV_PERIODIC_TIME_SPECIFICATION](#52-dv_periodic_time_specification)
- [6. encapsulated](#6-encapsulated)
  - [6.1. Reference UML](#61-reference-uml)
  - [6.2. encapsulated.DV_PARSABLE](#62-encapsulateddv_parsable)
    - [6.2.1. Test case DV_PARSABLE open constraint](#621-test-case-dv_parsable-open-constraint)
    - [6.2.2. Test case DV_PARSABLE value and formalism constrained](#622-test-case-dv_parsable-value-and-formalism-constrained)
  - [6.3. encapsulated.DV_MULTIMEDIA](#63-encapsulateddv_multimedia)
    - [6.3.1. Test ccase DV_MULTIMEDIA open constraint](#631-test-ccase-dv_multimedia-open-constraint)
    - [6.3.2. Test case DV_MULTIMEDIA media type constraint](#632-test-case-dv_multimedia-media-type-constraint)
- [7. uri](#7-uri)
  - [7.1. Reference UML](#71-reference-uml)
  - [7.2. DV_URI](#72-dv_uri)
    - [7.2.1. Test case DV_URI open constraint](#721-test-case-dv_uri-open-constraint)
    - [7.2.2. Test case DV_URI C_STRING pattern constraint for value](#722-test-case-dv_uri-c_string-pattern-constraint-for-value)
    - [7.2.3. Test case DV_URI C_STRING list constraint for value](#723-test-case-dv_uri-c_string-list-constraint-for-value)
  - [7.3. DV_EHR_URI](#73-dv_ehr_uri)
    - [7.3.1. Test case DV_EHR_URI open constraint](#731-test-case-dv_ehr_uri-open-constraint)
    - [7.3.2. Test case DV_EHR_URI C_STRING pattern constraint for value](#732-test-case-dv_ehr_uri-c_string-pattern-constraint-for-value)
    - [7.3.3. Test case DV_EHR_URI C_STRING list constraint for value](#733-test-case-dv_ehr_uri-c_string-list-constraint-for-value)

# General notes

1. All test data sets for date/time/datetime expressions are represented in the ISO 8601 extended format. An openEHR CDR could choose to use the extended (with field delimiter characters) or basic format (without field delimiters) of ISO 8601, or support any of the two formats. In the test implementations it is probable that the data sets are represented as JSON or XML documents, in which the date and time expressions are always representede in the ISO 8601 extended format, but internally the SUT could store any of the two formats. If the test implementation doesn't use JSON or XML, the date and time expression formats could use the ISO 8601 basic format.

2. The combination of test case + test data set is what will generate a result when running the test implementation againts a SUT.

3. The test data sets described inside each test case are not exhaustive. We can create more data sets here, including border cases and more failure cases and data set combinations.
   
4. To have a full view of the Conformance Verification components, please check the document published here https://www.cabolabs.com/blog/article/openehr_conformance_framework-61ef4f513f7c5.html

5. "TBD" means "To be defined".

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

Since this DB is not used or supported by modeling tools, this conformance test suite doesn't define test cases for valdiating this data type. For more info, see https://discourse.openehr.org/t/is-dv-paragraph-used/2187


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

NOTE: if this specification is implemented on a system that supports a RM < 1.1.0, then these tests shouldn't run against the system.

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

The DV_PROPORTION is constrained by a C_COMPLEX_OBJECT, which internally has C_REAL constraints for `numerator` and `denominator`. C_REAL defines two types of constraints: range and list of values. Though current modeling tools only allow range constraints. For the `type` atribute, a C_INTEGER constraint is used, which can hold list and range constraints but modeling tools only use the list.

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
| 0    | ratio            | 10        | 500         | 0         | [3]            | rejected | C_INTEGER.list                   |
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

| lower                        | upper                        | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated          |
|:----------------------------:|:----------------------------:|-----------------|-----------------|----------------|----------------|----------|-------------------------------|
| NULL                         | NULL                         | false           | false           | true           | true           | rejected | RM/Schema: value is mandatory for lower and upper |
| NULL                         | ""                           | false           | false           | true           | true           | rejected | RM/Schema: value is mandatory for lower. ISO8601: at least year is required for upper. |
| ""                           | NULL                         | false           | false           | true           | true           | rejected | ISO8601: at least year is required for lower. RM/Schema: value is mandatory for upper.
| 2021                         | NULL                         | false           | false           | true           | true           | rejected | RM/Schema: value is mandatory for upper. |
| NULL                         | 2022                         | false           | false           | true           | true           | rejected | RM/Schema: value is mandatory for lower. |
| 2021                         | 2022                         | false           | false           | true           | true           | accepted | |
| 2021-00                      | 2022-01                      | false           | false           | true           | true           | rejected | ISO8601: month in 01..12 for lower. |
| 2021-01                      | 2022-01                      | false           | false           | true           | true           | accepted | |
| 2021-01-00                   | 2022-01-01                   | false           | false           | true           | true           | rejected | ISO8601: day in 01..31 for lower. |
| 2021-01-32                   | 2022-01-01                   | false           | false           | true           | true           | rejected | ISO8601: day in 01..31 for lower. |
| 2021-01-01                   | 2022-01-00                   | false           | false           | true           | true           | rejected | ISO8601: day in 01..31 for upper. |
| 2021-01-30                   | 2022-01-00                   | false           | false           | true           | true           | rejected | ISO8601: day in 01..31 for upper. |
| 2021-01-30                   | 2022-01-15                   | false           | false           | true           | true           | accepted | |
| 2021-10-24T48                | 2022-01-15T10                | false           | false           | true           | true           | rejected | ISO8601: hours in 00..23 for lower. |
| 2021-10-24T21                | 2022-01-15T73                | false           | false           | true           | true           | rejected | ISO8601: hours in 00..23 for upper. |
| 2021-10-24T05                | 2022-01-15T10                | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:95             | 2022-01-15T10:45             | false           | false           | true           | true           | rejected | ISO8601: minutes in 00..59 for lower. |
| 2021-10-24T05:30             | 2022-01-15T10:61             | false           | false           | true           | true           | rejected | ISO8601: minutes in 00..59 for upper. |
| 2021-10-24T05:30             | 2022-01-15T10:45             | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:30:78          | 2022-01-15T10:45:13          | false           | false           | true           | true           | rejected | ISO8601: seconds in 00..59 for lower. |
| 2021-10-24T05:30:47          | 2022-01-15T10:45:69          | false           | false           | true           | true           | rejected | ISO8601: seconds in 00..59 for upper. |
| 2021-10-24T05:30:47          | 2022-01-15T10:45:13          | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:30:47.5        | 2022-01-15T10:45:13.6        | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:30:47.333      | 2022-01-15T10:45:13.555      | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:30:47.333333   | 2022-01-15T10:45:13.555555   | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:30:47Z         | 2022-01-15T10:45:13Z         | false           | false           | true           | true           | accepted | |
| 2021-10-24T05:30:47-03:00    | 2022-01-15T10:45:13-03:00    | false           | false           | true           | true           | accepted | |


### 3.9.2. Test case DV_INTERVAL<DV_DATE_TIME> lower and upper constraints are validity kind

> NOTE: the C_DATE_TIME has invariants that define if a higher precision component is optional or prohibited, lower precision components should be optional or prohibited. In other words, if `month` is optional, `day`, `hours`, `minutes`, etc. are optional or prohibited. These invariants should be checked in an archetype editor and template editor, we consider the following tests to follow those rules without checking them, since that is related to archetype/template validation, not with data validation.

> NOTE: if different components of each lower/upper date time expression fail the validity constraint for `mandatory`, the only required constraint violated to be reported is the higher precision one, since it implies the lower precision components will also fail. For instance if the hour, second and millisecond are `mandatory`, and the corresponding date time expression doesn't have hour, it is accepted if the reported constraints violated is only the hour_validity, and optionally the SUT can report the minute_validity, second_validity and millisecond_validity constraints as violated too. In the data sets below we show all the constraints violated.

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | month_val. (lower) | day_val. (lower) | month_val. (upper) | day_val. (upper) | hour_val. (lower) | minute_val. (lower) | second_val. (lower) | millisecond_val. (lower) | timezone_val. (lower) | hour_val. (upper) | minute_val. (upper) | second_val. (upper) | millisecond_val. (upper) | timezone_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|--------------------|------------------|--------------------|------------------|-------------------|---------------------|---------------------|-------------------------|-----------------------|-------------------|---------------------|---------------------|--------------------------|-----------------------|----------|-------------------------------|
| 2021       | 2022       | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | month_val. (lower), day_val. (lower), month_val. (upper), day_val. (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), second_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper), timezone_val. (lower), timezone__val. (upper) |
| 2021       | 2022       | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                 | mandatory             | rejected | month_validity (lower), month_validity (upper), timezone_val. (lower), timezone__val. (upper) |
| 2021       | 2022       | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                 | optional              | rejected | month_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                 | mandatory             | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021       | 2022       | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                 | optional              | accepted |  |
| 2021       | 2022       | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited        | prohibited          | prohibited          | prohibited               | mandatory             | prohibited        | prohibited          | prohibited          | prohibited               | mandatory             | rejected | month_validity (lower), month_validity (upper), timezone_val. (lower), timezone__val. (upper) |
| 2021       | 2022       | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited        | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited               | prohibited            | rejected | month_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited        | prohibited          | prohibited          | prohibited               | mandatory             | prohibited        | prohibited          | prohibited          | prohibited               | mandatory             | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021       | 2022       | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited        | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited               | prohibited            | accepted | |
| 2021-10    | 2022-10    | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | day_validity (lower), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), second_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper), timezone_val. (lower), timezone__val. (upper)|
| 2021-10    | 2022-10    | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone_val. (upper) |
| 2021-10    | 2022-10    | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                | optional               | accepted | |
| 2021-10    | 2022-10    | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited      |  prohibited        | prohibited          | prohibited          | prohibited               | mandatory             | prohibited        | prohibited          | prohibited          | mandatory               | mandatory              | rejected | timezone_val. (lower), timezone_val. (upper) |
| 2021-10    | 2022-10    | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited      | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | accepted | |
| 2021-10    | 2022-10    | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | mandatory            | prohibited         | prohibited          | prohibited          | prohibited              | mandatory              | rejected | month_validity (lower), month_validity (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10    | 2022-10    | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | month_validity (lower), month_validity (upper) |


| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | month_val. (lower) | day_val. (lower) | month_val. (upper) | day_val. (upper) | hour_val. (lower) | minute_val. (lower) | second_val. (lower) | millisecond_val. (lower) | timezone_val. (lower) | hour_val. (upper) | minute_val. (upper) | second_val. (upper) | millisecond_val. (upper) | timezone_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|--------------------|------------------|--------------------|------------------|-------------------|---------------------|---------------------|-------------------------|-----------------------|-------------------|---------------------|---------------------|--------------------------|-----------------------|----------|-------------------------------|
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), second_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | mandatory             | mandatory         | optional            | optional            | optional                | mandatory              | rejected | hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | optional              | mandatory         | optional            | optional            | optional                | optional               | rejected | hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                | optional               | accepted | |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | day_validity (lower), day_validity (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | minute_val. (lower), minute_val. (upper), second_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | mandatory             | mandatory         | optional            | optional            | optional                | mandatory              | rejected | minute_val. (lower), minute_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | optional              | mandatory         | optional            | optional            | optional                | optional               | rejected | minute_val. (lower), minute_val. (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                | optional               | accepted | |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | day_validity (lower), day_validity (upper), hour_val. (lower), hour_val. (upper) |
| 2021-10-24T22 | 2022-10-24T07 | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper), hour_val. (lower), hour_val. (upper) |


| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | month_val. (lower) | day_val. (lower) | month_val. (upper) | day_val. (upper) | hour_val. (lower) | minute_val. (lower) | second_val. (lower) | millisecond_val. (lower) | timezone_val. (lower) | hour_val. (upper) | minute_val. (upper) | second_val. (upper) | millisecond_val. (upper) | timezone_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|--------------------|------------------|--------------------|------------------|-------------------|---------------------|---------------------|-------------------------|-----------------------|-------------------|---------------------|---------------------|--------------------------|-----------------------|----------|-------------------------------|
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | second_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | mandatory             | mandatory         | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | optional              | mandatory         | optional            | optional            | optional                | optional               | accepted |  |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                | optional               | accepted | |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | day_validity (lower), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower),  minute_val. (upper) |
| 2021-10-24T22:10 | 2022-10-24T07:47 | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper) |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory               | mandatory              | rejected | millisecond_val. (lower), millisecond_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | mandatory             | mandatory         | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | optional              | mandatory         | optional            | optional            | optional                | optional               | accepted |  |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                | optional               | accepted | |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | day_validity (lower), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower),  minute_val. (upper), second_val. (lower), second_val. (upper) |
| 2021-10-24T22:10:45 | 2022-10-24T07:47:13 | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), second_val. (lower), second_val. (upper) |


| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | month_val. (lower) | day_val. (lower) | month_val. (upper) | day_val. (upper) | hour_val. (lower) | minute_val. (lower) | second_val. (lower) | millisecond_val. (lower) | timezone_val. (lower) | hour_val. (upper) | minute_val. (upper) | second_val. (upper) | millisecond_val. (upper) | timezone_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|--------------------|------------------|--------------------|------------------|-------------------|---------------------|---------------------|-------------------------|-----------------------|-------------------|---------------------|---------------------|--------------------------|-----------------------|----------|-------------------------------|
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | mandatory             | mandatory         | optional            | optional            | optional                 | mandatory              | rejected | timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | optional              | mandatory         | optional            | optional            | optional                 | optional               | accepted |  |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                 | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                 | optional               | accepted | |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                 | mandatory              | rejected | timezone_val. (lower), timezone__val. (upper) |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited               | prohibited             | rejected | day_validity (lower), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower),  minute_val. (upper), seoncd_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper) |
| 2021-10-24T22:10:45.5 | 2022-10-24T07:47:13.666666 | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited               | prohibited             | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), seoncd_val. (lower), second_val. (upper), millisecond_val. (lower), millisecond_val. (upper) |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | mandatory         | mandatory           | mandatory           | mandatory                | mandatory             | rejected | millisecond_val. (lower), millisecond_val. (upper) |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | mandatory             | mandatory         | optional            | optional            | optional                | mandatory              | accepted | |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | mandatory          | mandatory        | mandatory          | mandatory        | mandatory         | mandatory           | optional            | optional                 | optional              | mandatory         | optional            | optional            | optional                | optional               | accepted |  |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | accepted | |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | mandatory          | optional         | mandatory          | optional         | optional          | optional            | optional            | optional                 | optional              | optional          | optional            | optional            | optional                | optional               | accepted | |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | optional           | optional         | optional           | optional         | optional          | optional            | optional            | optional                 | mandatory             | optional          | optional            | optional            | optional                | mandatory              | accepted | |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | mandatory          | prohibited       | mandatory          | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | day_validity (lower), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower),  minute_val. (upper), second_val. (lower), second_val. (upper), timezone_val. (lower), timezone_val. (upper) |
| 2021-10-24T22:10:45Z | 2022-10-24T07:47:13Z | false           | false           | true           | true           | prohibited         | prohibited       | prohibited         | prohibited       | prohibited         | prohibited          | prohibited          | prohibited               | prohibited            | prohibited        | prohibited          | prohibited          | prohibited              | prohibited             | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper), hour_val. (lower), hour_val. (upper), minute_val. (lower), minute_val. (upper), second_val. (lower), second_val. (upper), timezone_val. (lower), timezone_val. (upper) |



### 3.9.3. Test case DV_INTERVAL<DV_DATE_TIME> lower and upper constraints are range

| lower              | upper              | lower_unbounded | upper_unbounded | lower_included | upper_included | C_DATE_TIME.range (lower)       | C_DATE_TIME.range (upper)       | expected | constraints violated                       |
|:------------------:|:------------------:|-----------------|-----------------|----------------|----------------|---------------------------------|---------------------------------|----------|-------------------------------------------|
| 2021               | 2022               | false           | false           | true           | true           | 2020..2030                      | 2020..2030                      | accepted |                                            |
| 2021               | 2022               | false           | false           | true           | true           | 2000..2010                      | 2020..2030                      | rejected | C_DATE_TIME.range (lower)                  |
| 2021               | 2022               | false           | false           | true           | true           | 2020..2030                      | 2020..2021                      | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10            | 2022-11            | false           | false           | true           | true           | 2020-01..2030-12                | 2020-01..2030-12                | accepted |                                            |
| 2021-10            | 2022-11            | false           | false           | true           | true           | 2000-01..2010-12                | 2020-01..2030-12                | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10            | 2022-11            | false           | false           | true           | true           | 2020-01..2030-12                | 2020-01..2021-12                | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10-24         | 2022-11-02         | false           | false           | true           | true           | 2020-01-01..2030-12-31          | 2020-01-01..2030-12-31          | accepted |                                            |
| 2021-10-24         | 2022-11-02         | false           | false           | true           | true           | 2000-01-01..2010-12-31          | 2020-01-01..2030-12-31          | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10-24         | 2022-11-02         | false           | false           | true           | true           | 2020-01-01..2030-12-31          | 2020-01-01..2021-12-31          | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10-24T10      | 2022-11-02T19      | false           | false           | true           | true           | 2020-01-01T00..2030-12-31T23    | 2020-01-01T00..2030-12-31T23    | accepted |                                            |
| 2021-10-24T10      | 2022-11-02T19      | false           | false           | true           | true           | 2000-01-01T00..2010-12-31T23    | 2020-01-01T00..2030-12-31T23    | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10-24T10      | 2022-11-02T19      | false           | false           | true           | true           | 2020-01-01T00..2030-12-31T23    | 2020-01-01T00..2021-12-31T23    | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10-24T10:00          | 2022-11-02T19:32          | false           | false           | true           | true           | 2020-01-01T00:00..2030-12-31T23:59    | 2020-01-01T00:00..2030-12-31T23:59    | accepted |                                            |
| 2021-10-24T10:00          | 2022-11-02T19:32          | false           | false           | true           | true           | 2000-01-01T00:00..2010-12-31T23:59    | 2020-01-01T00:00..2030-12-31T23:59    | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10-24T10:00          | 2022-11-02T19:32          | false           | false           | true           | true           | 2020-01-01T00:00..2030-12-31T23:59    | 2020-01-01T00:00..2021-12-31T23:59    | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10-24T10:00:10       | 2022-11-02T19:32:40       | false           | false           | true           | true           | 2020-01-01T00:00:00..2030-12-31T23:59:59    | 2020-01-01T00:00..2030-12-31T23:59    | accepted |                                            |
| 2021-10-24T10:00:10       | 2022-11-02T19:32:40       | false           | false           | true           | true           | 2000-01-01T00:00:00..2010-12-31T23:59:59    | 2020-01-01T00:00..2030-12-31T23:59    | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10-24T10:00:10       | 2022-11-02T19:32:40       | false           | false           | true           | true           | 2020-01-01T00:00:00..2030-12-31T23:59:59    | 2020-01-01T00:00..2021-12-31T23:59    | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10-24T10:00:10.5     | 2022-11-02T19:32:40.333   | false           | false           | true           | true           | 2020-01-01T00:00:00.0..2030-12-31T23:59:59.999999 | 2020-01-01T00:00..2030-12-31T23:59    | accepted |                                            |
| 2021-10-24T10:00:10.5     | 2022-11-02T19:32:40.333   | false           | false           | true           | true           | 2000-01-01T00:00:00.0..2010-12-31T23:59:59.999999 | 2020-01-01T00:00..2030-12-31T23:59    | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10-24T10:00:10.5     | 2022-11-02T19:32:40.333   | false           | false           | true           | true           | 2020-01-01T00:00:00.0..2030-12-31T23:59:59.999999 | 2020-01-01T00:00..2021-12-31T23:59    | rejected | C_DATE_TIME.range (upper)                  |
| 2021-10-24T10:00:10Z      | 2022-11-02T19:32:40Z      | false           | false           | true           | true           | 2020-01-01T00:00:00Z..2030-12-31T23:59:59Z   | 2020-01-01T00:00..2030-12-31T23:59    | accepted |                                            |
| 2021-10-24T10:00:10Z      | 2022-11-02T19:32:40Z      | false           | false           | true           | true           | 2000-01-01T00:00:00Z..2010-12-31T23:59:59Z   | 2020-01-01T00:00..2030-12-31T23:59    | rejected | C_DATE_TIME.range (lower)                  |
| 2021-10-24T10:00:10Z      | 2022-11-02T19:32:40Z      | false           | false           | true           | true           | 2020-01-01T00:00:00Z..2030-12-31T23:59:59Z   | 2020-01-01T00:00..2021-12-31T23:59    | rejected | C_DATE_TIME.range (upper)                  |


## 3.10. quantity.DV_INTERVAL<DV_DATE>

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

> NOTE: the C_DATE has invariants that define if a higher precision component is optional or prohibited, lower precision components should be optional or prohibited. In other words, if `month` is optional, `day` should be optional or prohibited. These invariants should be checked in an archetype editor and template editor, we consider the following tests to follow those rules without checking them, since that is related to archetype/template validation, not with data validation.


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

| lower        | upper        | lower_unbounded | upper_unbounded | lower_included | upper_included | minute_val. (lower) | second_val. (lower) | millisecond_val. (lower) | timezone_val. (lower) | minute_val. (upper) | second_val. (upper) | millisecond_val. (upper) | timezone_val. (upper) | expected | constraints violated          |
|:------------:|:------------:|-----------------|-----------------|----------------|----------------|---------------------|---------------------|-------------------------|-----------------------|---------------------|---------------------|--------------------------|-----------------------|---------|-------------------------------|
| T10          | T11          | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | minute_val. (lower), second_val. (lower), millisecond_val. (lower), timezone_val. (lower), minute_val. (upper), second_val. (upper), millisecond_val. (upper), timezone_val. (upper) |
| T10:00       | T11:00       | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | second_val. (lower), millisecond_val. (lower), timezone_val. (lower), second_val. (upper), millisecond_val. (upper), timezone_val. (upper) |
| T10:00:00    | T11:00:00    | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | millisecond_val. (lower), timezone_val. (lower), millisecond_val. (upper), timezone_val. (upper) |
| T10:00:00.5  | T11:00:00.5  | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | rejected | timezone_val. (lower) timezone_val. (upper) |
| T10:00:00.5Z | T11:00:00.5Z | false           | false           | true           | true           | mandatory           | mandatory           | mandatory                | mandatory             | mandatory           | mandatory           | mandatory                | mandatory             | accepted |  |

TBD: combinations of other values for validity.


### 3.11.3. Test case DV_INTERVAL<DV_TIME> range constraint

| lower         | upper         | lower_unbounded | upper_unbounded | lower_included | upper_included | C_TIME.range (lower)      | C_TIME.range (upper)       | expected | constraints violated      |
|:-------------:|:-------------:|-----------------|-----------------|----------------|----------------|---------------------------|----------------------------|----------|---------------------------|
| T10           | T11           | false           | false           | true           | true           | T09..T11                  | T10..T12                   | accepted |                           |
| T10:00        | T11:00        | false           | false           | true           | true           | T09:00..T11:00            | T10:00..T12:00             | accepted |                           |
| T10:00:00     | T11:00:00     | false           | false           | true           | true           | T09:00:00..T11:00:00      | T10:00:00..T12:00:00       | accepted |                           |
| T10:00:00.5   | T11:00:00.5   | false           | false           | true           | true           | T09:00:00.0..T11:00:00.0  | T10:00:00.0..T12:00:00.0   | accepted |                           |
| T10:00:00.5Z  | T11:00:00.5Z  | false           | false           | true           | true           | T09:00:00.0..T11:00:00.0Z | T10:00:00.0Z..T12:00:00.0Z | accepted |                           |
| T10           | T11           | false           | false           | true           | true           | T11..T12                  | T11..T12                   | rejected | C_TIME.range (lower)      |
| T10           | T12           | false           | false           | true           | true           | T10..T11                  | T10..T11                   | rejected | C_TIME.range (upper)      |
| null          | T11           | true            | false           | false          | true           | T09..T11                  | T10..T12                   | rejected | C_TIME.range (lower)      |
| T10           | null          | false           | true            | true           | false          | T09..T11                  | T10..T12                   | accepted | C_TIME.range (upper)      |



## 3.12. quantity.DV_INTERVAL<DV_DURATION>

### 3.12.1. Test case DV_INTERVAL<DV_DURATION> open constraint

> NOTE: this considers the `lower` value of the interval should have all it's components lower or equals to the corresponding component in the `upper` value. This is to avoid normalization problems. For instance we could have an interval `P1Y6M..P2Y` which is semantically correct. But if we have values outside the normal boundaries of each component, like `P1Y37M..P2Y` there is a need of normalization to know if `P1Y37M` is really lower or equals to `P2Y`, which is the check ofr a valid internal. In some cases this normalization is doable, but in other cases it is not. For instance, some implementations might not know how many days in a month are, since months have a variable number of days. In the previous case, we know each year has 12 months so `P1Y37M` can actually be normalized to `P4Y1M`, but `P61D` can't be strictly compared with, let's say, `P3M`, since months could have 28, 29, 30 or 31 days, so without other information `P61D` could be lower or greater than `P3M`. To simplify this, some implementations might consider the measure of a `month`, in a duration expression, to be exactly 30 days. These considerations should be stated in the SUT Conformance Statement Document. To simplify writing the test cases for any implementation, we consider if `lower` is `P1Y37M`, the valid `upper` values have Y >= 1 and M >= 37, so `P2Y` wouldn't be valid in this context, but `P1Y37M..P1Y38M` or `P1Y37M..P2Y37M` would be valid intervals for the test cases. One extra simplification would be to consider values are inside their normal boundaries (hours < 24, days < 31, etc.) but this won't be encouraged but these test cases. If each component is inside it's constrainsts it is possible to compare expressions that differ in the components like `P1D3H` and `P10D`, since comparison doesn't require normalization and both values form a semantically valid interval.

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated          | comment |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------|-------------------------------|---------|
| NULL       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 | |
| NULL       | PT2H       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 | |
| PT1H       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 | |
| PT1H       | PT2H       | false           | false           | true           | true           | accepted |            | |
| PT1H       | PT2H       | false           | false           | true           | true           | accepted |            | |
| P1Y7M3D    | P1Y8M3D    | false           | false           | true           | true           | accepted |            | |
| P1M5DT3H   | P10M       | false           | false           | true           | true           | accepted |            | Note this case has different components in the lower and upper values, this is possible because the values don't exceed their normal boundaries, e.g. `days` > 31. Without this condition a normalization of the values would be needed, and in some cases the normalization is not possible without some extra constraints, for instance considering `P1M` is equivalent to `P30D`. |
| P2M        | P1M        | false           | false           | true           | true           | rejected | limits_consistent (invariant) | |
| P10M       | P1M5DT3H   | false           | false           | true           | true           | rejected | limits_consistent (invariant) | |

### 3.12.2. Test case DV_INTERVAL<DV_DURATION> xxx_allowed constraints

> NOTE: in the openEHR specifications only the seconds can have a fraction, but in the ISO8601 standard, the component at the lowest precision can have a fraction, for instance `P0.5Y` is a valid ISO 8601 duration.

| lower            | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | years_allowed (lower) | months_allowed (lower) | weeks_allowed (lower) | days_allowed (lower) | hours_allowed (lower) | minutes_allowed (lower) | seconds_allowed (lower) | fractional_seconds_allowed (lower) | years_allowed (upper) | months_allowed (upper) | weeks_allowed (upper) | days_allowed (upper) | hours_allowed (upper) | minutes_allowed (upper) | seconds_allowed (upper) | fractional_seconds_allowed (upper) | expected | constraints violated               | comment |
|:----------------:|:----------:|-----------------|-----------------|----------------|----------------|-----------------------|------------------------|-----------------------|----------------------|-----------------------|-------------------------|-------------------------|------------------------------------|-----------------------|------------------------|-----------------------|----------------------|-----------------------|-------------------------|-------------------------|------------------------------------|----------|------------------------------------|---------|
| P1Y              | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | accepted |                                    |         |
| P3W              | P5W        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | accepted |                                    |         |
| P1Y              | P2Y        | false           | false           | true           | true           | false                 | true                   | true                  | true                 | true                  | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | years_allowed (lower)              |         |
| P1Y              | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | false                 | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | years_allowed (upper)              |         |
| P1Y1M1DT1H1M1.5S | P2Y        | false           | false           | true           | true           | true                  | false                  | true                  | true                 | true                  | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | months_allowed (lower)             |         |
| P2W              | P2Y        | false           | false           | true           | true           | true                  | true                   | false                 | true                 | true                  | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | weeks_allowed (lower)              |         |
| P1Y1M1DT1H1M1.5S | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | false                | true                  | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | days_allowed (lower)               |         |
| P1Y1M1DT1H1M1.5S | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | false                 | true                    | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | hours_allowed (lower)              |         |
| P1Y1M1DT1H1M1.5S | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | true                  | false                   | true                    | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | minutes_allowed (lower)            |         |
| P1Y1M1DT1H1M1.5S | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | true                  | true                    | false                   | true                               | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | seconds_allowed (lower)            |         |
| P1Y1M1DT1H1M1.5S | P2Y        | false           | false           | true           | true           | true                  | true                   | true                  | true                 | true                  | true                    | true                    | false                              | true                  | true                   | true                  | true                 | true                  | true                    | true                    | true                               | rejected | fractional_seconds_allowed (lower) |         |


### 3.12.3. Test case DV_INTERVAL<DV_DURATION> range constraints

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | range.lower (lower) | range.upper (lower) | range.lower (upper) | range.upper (upper) | expected | constraints violated          | comment |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|---------------------|---------------------|---------------------|---------------------|----------|-------------------------------|---------|
| P1Y        | P2Y        | false           | false           | true           | true           | P1Y                 | P3Y                 | P1Y                 | P3Y                 | accepted |                               |         |
| P1Y        | P2Y        | false           | false           | true           | true           | P2Y                 | P3Y                 | P1Y                 | P3Y                 | rejected | range.lower (lower)           |         |
| P1Y        | P2Y        | false           | false           | true           | true           | P1Y                 | P3Y                 | P3Y                 | P4Y                 | rejected | range.lower (upper)           |         |
| P5Y        | P10Y       | false           | false           | true           | true           | P2Y                 | P3Y                 | P5Y                 | P15Y                | rejected | range.upper (lower)           |         |
| P5Y        | P10Y       | false           | false           | true           | true           | P1Y                 | P9Y                 | P3Y                 | P9Y                 | rejected | range.upper (upper)           |         |
| P5Y4M      | P10Y       | false           | false           | true           | true           | P1Y                 | P9Y                 | P3Y                 | P15Y                | accepted |                               |         |
| P5Y4M      | P10Y       | false           | false           | true           | true           | P6Y                 | P9Y                 | P3Y                 | P15Y                | rejected | range.lower (lower)           |         |
| P5Y4M      | P10Y       | false           | false           | true           | true           | P5Y4M2D             | P9Y                 | P3Y                 | P15Y                | rejected | range.lower (lower)           |         |
| P5Y4M20D   | P10Y       | false           | false           | true           | true           | P1Y                 | P9Y                 | P3Y                 | P15Y                | accepted |                               |         |
| P5Y4M20D   | P10Y       | false           | false           | true           | true           | P5Y6M               | P9Y                 | P3Y                 | P15Y                | rejected | range.lower (lower)           |         |




## 3.13. quantity.DV_INTERVAL<DV_ORDINAL>

> NOTE: some modeling tools don't support representing DV_INTERVAL<DV_ORDINAL>.

### 3.13.1. Test case DV_INTERVAL<DV_ORDINAL> open constraint

This case is when the ADL has `DV_ORDINAL matches {*}`

| lower.symbol   | lower.value | upper.symbol   | upper.value | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated |
|:---------------|------------:|:---------------|------------:|-----------------|-----------------|----------------|----------------|----------|----------------------|
| NULL           | NULL        | NULL           | NULL        | false           | false           | true           | true           | rejected | RM/Schema value and symbol are mandatory for lower and upper |
| NULL           | 1           | NULL           | 5           | false           | false           | true           | true           | rejected | RM/Schema symbol is mandatory for lower and upper            |
| local::at0005  | NULL        | local::at0003  | NULL        | false           | false           | true           | true           | rejected | RM/Schema value is mandatory for lower and upper             |
| local::at0005  | 1           | local::at0002  | 5           | false           | false           | true           | true           | accepted |                                                              |
| local::at0004  | 666         | local::at0003  | 777         | false           | false           | true           | true           | accepted |                                                              |
| local::at0003  | 777         | local::at0004  | 666         | false           | false           | true           | true           | rejected | RM invariante Interval.Limits_comparable                     |


### 3.13.2. Test case DV_INTERVAL<DV_ORDINAL> with constraints

| lower.symbol   | lower.value | upper.symbol   | upper.value | lower_unbounded | upper_unbounded | lower_included | upper_included | lower.C_DV_ORDINAL.list                | upper.C_DV_ORDINAL.list                | expected | constraints violated |
|:---------------|------------:|:---------------|------------:|-----------------|-----------------|----------------|----------------|----------------------------------------|----------------------------------------|----------|----------------------|
| local::at0005  | 1           | local::at0002  | 5           | false           | false           | true           | true           |   1|[local::at0005], 2|[local::at0006] |   5|[local::at0002], 2|[local::at0006] | accepted |                                                           |
| local::at0004  | 666         | local::at0003  | 777         | false           | false           | true           | true           |   8|[local::at0004], 2|[local::at0006] |   9|[local::at0003], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value for lower and upper  |
| local::at0666  | 1           | local::at0777  | 2           | false           | false           | true           | true           |   1|[local::at0005], 2|[local::at0006] |   1|[local::at0005], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching symbol for lower and upper |
| local::at0004  | 666         | local::at0003  | 777         | false           | false           | true           | true           |   8|[local::at0004], 2|[local::at0006] | 777|[local::at0003], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value for lower            |
| local::at0666  | 1           | local::at0777  | 2           | false           | false           | true           | true           |   1|[local::at0005], 2|[local::at0006] |   1|[local::at0005], 2|[local::at0777] | rejected | C_DV_ORDINAL.list: no matching symbol for lower           |
| local::at0004  | 666         | local::at0003  | 777         | false           | false           | true           | true           | 666|[local::at0004], 2|[local::at0006] |   9|[local::at0003], 2|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value for upper            |
| local::at0005  | 1           | local::at0777  | 5           | false           | false           | true           | true           |   1|[local::at0005], 2|[local::at0006] |   1|[local::at0005], 5|[local::at0999] | rejected | C_DV_ORDINAL.list: no matching symbol for upper           |



## 3.14. quantity.DV_INTERVAL<DV_SCALE>

DV_SCALE was introduced to the RM 1.1.0 (https://openehr.atlassian.net/browse/SPECRM-19), it is analogous to DV_ORDINAL with a Real value. So test cases for DV_SCALE and DV_ORDINAL are similar.

NOTE: if this specification is implemented on a system that supports a RM < 1.1.0, then these tests shouldn't run against the system.

> NOTE: some modeling tools don't support representing DV_INTERVAL<DV_SCALE>

### 3.14.1. Test case DV_SCALE open constraint

This case is when the ADL has `DV_ORDINAL matches {*}`

| lower.symbol   | lower.value | upper.symbol   | upper.value | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated |
|:---------------|------------:|:---------------|------------:|-----------------|-----------------|----------------|----------------|----------|----------------------|
| NULL           | NULL        | NULL           | NULL        | false           | false           | true           | true           | rejected | RM/Schema value and symbol are mandatory for lower and upper |
| NULL           | 1.5         | NULL           | 5.3         | false           | false           | true           | true           | rejected | RM/Schema symbol is mandatory for lower and upper            |
| local::at0005  | NULL        | local::at0003  | NULL        | false           | false           | true           | true           | rejected | RM/Schema value is mandatory for lower and upper             |
| local::at0005  | 1.5         | local::at0002  | 5.3         | false           | false           | true           | true           | accepted |                                                              |
| local::at0004  | 666.1       | local::at0003  | 777.1       | false           | false           | true           | true           | accepted |                                                              |
| local::at0003  | 777.1       | local::at0004  | 666.1       | false           | false           | true           | true           | rejected | RM invariante Interval.Limits_comparable                     |


### 3.14.2. Test case DV_SCALE with constraints

| lower.symbol   | lower.value | upper.symbol   | upper.value | lower_unbounded | upper_unbounded | lower_included | upper_included | lower.C_DV_ORDINAL.list                    | upper.C_DV_ORDINAL.list                    | expected | constraints violated |
|:---------------|------------:|:---------------|------------:|-----------------|-----------------|----------------|----------------|--------------------------------------------|--------------------------------------------|----------|----------------------|
| local::at0005  | 1.5         | local::at0002  | 5.3         | false           | false           | true           | true           |   1.5|[local::at0005], 2.4|[local::at0006] |   5.3|[local::at0002], 2.4|[local::at0006] | accepted |                                                           |
| local::at0004  | 666.1       | local::at0003  | 777.1       | false           | false           | true           | true           |   8.9|[local::at0004], 2.4|[local::at0006] |   9.7|[local::at0003], 2.4|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value for lower and upper  |
| local::at0666  | 1.5         | local::at0777  | 2.4         | false           | false           | true           | true           |   1.5|[local::at0005], 2.4|[local::at0006] |   1.5|[local::at0005], 2.4|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching symbol for lower and upper |
| local::at0004  | 666.1       | local::at0003  | 777.1       | false           | false           | true           | true           |   8.9|[local::at0004], 2.4|[local::at0006] | 777.1|[local::at0003], 2.4|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value for lower            |
| local::at0666  | 1.5         | local::at0777  | 2.4         | false           | false           | true           | true           |   1.5|[local::at0005], 2.4|[local::at0006] |   1.5|[local::at0005], 2.4|[local::at0777] | rejected | C_DV_ORDINAL.list: no matching symbol for lower           |
| local::at0004  | 666.1       | local::at0003  | 777.1       | false           | false           | true           | true           | 666.1|[local::at0004], 2.4|[local::at0006] |   9.7|[local::at0003], 2.4|[local::at0006] | rejected | C_DV_ORDINAL.list: no matching value for upper            |
| local::at0005  | 1.5         | local::at0777  | 5.3         | false           | false           | true           | true           |   1.5|[local::at0005], 2.4|[local::at0006] |   1.5|[local::at0005], 5.3|[local::at0999] | rejected | C_DV_ORDINAL.list: no matching symbol for upper           |




## 3.15. quantity.DV_INTERVAL<DV_PROPORTION>

> NOTE: some modeling tools don't support representing DV_INTERVAL<DV_PROPORTION>.

### 3.15.1. Test case DV_INTERVAL<DV_PROPORTION> open constraint

The test data sets for lower and upper are divided into multiple tables because there are many attributes in the DV_PROPORTION.

#### 3.15.1.a. Data set both valid ratios

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 20        | 500         | 0         |

| expected | constraints violated             |
|----------|----------------------------------|
| accepted |                                  |

#### 3.15.1.b. Data set different limit types

This data set fails beacause DV_INTERVAL.Limits_consistent need both lower and upper to have the same `type`.

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | unitary          | 10        | 1           | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

| expected | constraints violated                      |
|----------|-------------------------------------------|
| rejected | DV_INTERVAL.Limits_consistent (invariant) |

#### 3.15.1.c. Data set greater lower

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 5         | 500         | 0         |

| expected | constraints violated                      |
|----------|-------------------------------------------|
| rejected | DV_INTERVAL.Limits_consistent (invariant) |



### 3.15.2. Test case DV_INTERVAL<DV_PROPORTION> ratios

The constraint is on the `type` of each limit of the interval as a C_INTEGER.list = [0], constraining the type as a ratio.

#### 3.15.2.a. Data set valid ratios

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 20        | 500         | 0         |

| expected | constraints violated             |
|----------|----------------------------------|
| accepted |                                  |

#### 3.15.2.b. Data set no ratios

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 1    | unitary          | 10        | 1           | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 1    | unitary          | 20        | 1           | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| rejected | C_INTEGER.list for lower and upper |



### 3.15.3. Test case DV_INTERVAL<DV_PROPORTION> unitaries

The constraint is on the `type` of each limit of the interval as a C_INTEGER.list = [1], constraining the type as unitary.

#### 3.15.3.a. Data set valid unitaries

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 1    | unitary          | 10        | 1           | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 1    | unitary          | 20        | 1           | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| accepted |                                    |

#### 3.15.3.b. Data set no unitaries

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 20        | 500         | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| rejected | C_INTEGER.list for lower and upper |



### 3.15.4. Test case DV_INTERVAL<DV_PROPORTION> percentages

The constraint is on the `type` of each limit of the interval as a C_INTEGER.list = [2], constraining the type as percentage.

#### 3.15.4.a. Data set valid percentages

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 2    | percent          | 10        | 100         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 2    | percent          | 20        | 100         | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| accepted |                                    |

#### 3.15.4.b. Data set no percentages

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 20        | 500         | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| rejected | C_INTEGER.list for lower and upper |



### 3.15.5. Test case DV_INTERVAL<DV_PROPORTION> fractions

The constraint is on the `type` of each limit of the interval as a C_INTEGER.list = [3], constraining the type as fraction.

#### 3.15.5.a. Data set valid fractions

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 3    | fraction         | 3         | 4           | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 3    | fraction         | 5         | 4           | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| accepted |                                    |

#### 3.15.5.b. Data set no fractions

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 20        | 500         | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| rejected | C_INTEGER.list for lower and upper |



### 3.15.6. Test case DV_INTERVAL<DV_PROPORTION> integer fractions

The constraint is on the `type` of each limit of the interval as a C_INTEGER.list = [3], constraining the type as fraction.

#### 3.15.6.a. Data set valid integer fractions

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 4    | integer fraction | 3         | 4           | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 4    | integer fraction | 5         | 4           | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| accepted |                                    |

#### 3.15.6.b. Data set no integer fractions

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 10        | 500         | 0         |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision |
|:----:|------------------|-----------|-------------|-----------|
| 0    | ratio            | 20        | 500         | 0         |

| expected | constraints violated               |
|----------|------------------------------------|
| rejected | C_INTEGER.list for lower and upper |



### 3.15.7. Test case DV_INTERVAL<DV_PROPORTION> ratios with range limits

The constraint is on the `type` of each limit of the interval as a C_INTEGER.list = [0], constraining the type as a ratio. For the limits, the constraints are C_REAL using the range attribute.

#### 3.15.7.a. Data set valid ratios

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision | C_REAL.range (num) | C_REAL.range (den) | 
|:----:|------------------|-----------|-------------|-----------|--------------------|--------------------|
| 0    | ratio            | 10        | 500         | 0         | 0..15              | 100..1000          |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision | C_REAL.range (num) | C_REAL.range (den) | 
|:----:|------------------|-----------|-------------|-----------|--------------------|--------------------|
| 0    | ratio            | 20        | 500         | 0         | 0..50              | 100..1000          |

| expected | constraints violated             |
|----------|----------------------------------|
| accepted |                                  |


#### 3.15.7.b. Data set ratios, invalid lower

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision | C_REAL.range (num) | C_REAL.range (den) | 
|:----:|------------------|-----------|-------------|-----------|--------------------|--------------------|
| 0    | ratio            | 10        | 500         | 0         | 0..5               | 100..1000          |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision | C_REAL.range (num) | C_REAL.range (den) | 
|:----:|------------------|-----------|-------------|-----------|--------------------|--------------------|
| 0    | ratio            | 20        | 500         | 0         | 0..50              | 100..1000          |

| expected | constraints violated             |
|----------|----------------------------------|
| rejected | C_REAL.range (num) for lower     |


#### 3.15.7.c. Data set ratios, invalid upper

DV_INTERVAL.lower

| type | meaning (kind)   | numerator | denominator | precision | C_REAL.range (num) | C_REAL.range (den) | 
|:----:|------------------|-----------|-------------|-----------|--------------------|--------------------|
| 0    | ratio            | 10        | 500         | 0         | 0..15              | 100..1000          |

DV_INTERVAL.upper

| type | meaning (kind)   | numerator | denominator | precision | C_REAL.range (num) | C_REAL.range (den) | 
|:----:|------------------|-----------|-------------|-----------|--------------------|--------------------|
| 0    | ratio            | 20        | 500         | 0         | 0..10              | 100..1000          |

| expected | constraints violated             |
|----------|----------------------------------|
| rejected | C_REAL.range (num) for upper     |


# 4. quantity.date_time

## 4.1. Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_types.quantity.date_time.svg)


## 4.2. quantity.date_time.DV_DURATION

> NOTE: different duration implementations might affect the DV_DURATION related test cases. For instance, some implementations might not support `days` in the same duration
> expression that contains `months`, since there is no exact correspondence between the number of `days` and `months` (months could have 28, 29, 30 or 31 days). Then other
> implementations might simplify the `month` measurement to be 30 days. This also happens with some implementations that consider a `day` is exactly `24 hours` as a simplification.
> It is worth mentioning that openEHR provides means for calculating this based on averages, in DV_DURATION.magnitude(), which is implemented in terms of Iso8601_duration.to_seconds(),
> it uses `Time_Definitions.Average_days_in_year` as an approximation to the numbers of days in a year, and `Time_Definitions.Average_days_in_month` as an approximation to the numbers of
> days in a month. So to normalize an expression that is P1Y3M5D to `days` we would have `1 * Average_days_in_year + 3 * Average_days_in_month + 5`.
> In case the SUT has an implementation decision to be considered, the developers should mention it in the Conformance Statement Document.

The openEHR specifications have two exceptions to the ISO 8601-1 rules:

1. a negative sign may be used before a Duration expression, for example `-P10D`, meaning '10 days before [origin]', where the 'origin' is a timepoint understood as the origin for the duration;
2. the `W` designator may be mixed with other designators in the duration expression.

Note those exceptions are invalid in terms of ISO 8601-1_2019, but, those are valid in terms for ISO 8601-2_2019, which defines some extensions to the ISO 8601-1 standard. From ISO 8601-2:

> Expressions in the following four examples below are not valid in ISO 8601-1, but are valid as specified in this clause.
> 
> EXAMPLE 3 'P3W2D', duration of three weeks and two days, which is 23 days (equivalent to the expression 'P23D'). In ISO 8601-1, ["W"] is not permitted to occur along with any other component.
> 
> EXAMPLE 4 'P5Y10W', duration of five years and ten weeks.
> ...
> EXAMPLE 7 '-P2M1D' is equivalent to 'P-2M-1D'.


### 4.2.1. Test case DV_DURATION open constraint

| value              | expected | violated constraints                                         |
|--------------------|----------|--------------------------------------------------------------|
| NULL               | rejected | DV_DURATION.value is mandatory in the RM                     |
| 1Y                 | rejected | invalid ISO 8601-1 duration: missing duration desingator 'P' |
| P1Y                | accepted |                                                              |
| P1Y3M              | accepted |                                                              |
| P1W                | accepted |                                                              |
| P1Y3M4D            | accepted |                                                              |
| P1Y3M4DT2H         | accepted |                                                              |
| P1Y3M4DT2H14M      | accepted |                                                              |
| P1Y3M4DT2H14M5S    | accepted |                                                              |
| P1Y3M4DT2H14M15.5S | accepted |                                                              |
| P1Y3M4DT2H14.5M    | rejected | openEHR: fractions for minutes are not allowed               |
| P1Y3M4DT2.5H       | rejected | openEHR: fractions for hours are not allowed                 |
| P3M1W              | accepted |                                                              |
| -P2M               | accepted |                                                              |


### 4.2.2. Test case DV_DURATION xxx_allowed field constraints

The `xxx_allowed` fields are defined in the `C_DURATION` class, which allows to constraint the `DV_DURATION.value` attribute.

| value                | years_allowed | months_allowed | weeks_allowed | days_allowed | hours_allowed | minutes_allowed | seconds_allowed | fractional_seconds_allowed | expected | violated constraints     |
|----------------------|---------------|----------------|---------------|--------------|---------------|-----------------|-----------------|----------------------------|----------|--------------------------|
| P1Y                  | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y                  | false         | true           | true          | true         | true          | true            | true            | true                       | rejected | C_DURATION.years_allowed |
| P1Y3M                | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y3M                | true          | false          | true          | true         | true          | true            | true            | true                       | rejected | C_DURATION.months_allowed |
| P1Y3M15D             | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y3M15D             | true          | true           | true          | false        | true          | true            | true            | true                       | rejected | C_DURATION.days_allowed |
| P1W                  | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P7W                  | true          | true           | false         | true         | true          | true            | true            | true                       | rejected | C_DURATION.weeks_allowed |
| P1Y3M15DT23H         | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y3M15DT23H         | true          | true           | true          | true         | false         | true            | true            | true                       | rejected | C_DURATION.hours_allowed |
| P1Y3M15DT23H35M      | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y3M15DT23H35M      | true          | true           | true          | true         | true          | false           | true            | true                       | rejected | C_DURATION.minutes_allowed |
| P1Y3M15DT23H35M22S   | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y3M15DT23H35M22S   | true          | true           | true          | true         | true          | true            | false           | true                       | rejected | C_DURATION.seconds_allowed |
| P1Y3M15DT23H35M22.5S | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1Y3M15DT23H35M22.5S | true          | true           | true          | true         | true          | true            | true            | false                      | rejected | C_DURATION.fractional_seconds_allowed |
| P1W3D                | true          | true           | true          | true         | true          | true            | true            | true                       | accepted |  |
| P1W3D                | true          | true           | false         | true         | true          | true            | true            | true                       | rejected | C_DURATION.weeks_allowed |


### 4.2.3. Test case DV_DURATION range constraint

In order to compare durations, the DV_DURATION.magnitude() should be used, which will calculate the seconds in the duration based on the avg. days in year and days in month. If the SUT does calculate the `magnitude()` in a different way, it should be stated in the Conformance Statement Document.

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
| P1Y20M            | P2Y            | P50Y           | accepted |                        |
| P2W               | P0W            | P3W            | accepted |                        |
| P2W               | P2W            | P3W            | accepted |                        |
| P2W               | P3W            | P3W            | rejected | C_DURATION.range.lower |
| P2W3D             | P3W            | P4W            | rejected | C_DURATION.range.lower |
| P2W8D             | P3W            | P4W            | accepted |                        |
| P2W15D            | P3W            | P4W            | rejected | C_DURATION.range.upper |


### 4.2.4. Test case DV_DURATION fields allowed and range constraints combined

In the AOM specification it is allowed to combine allowed and range: "Both range and the constraint pattern can be set at the same time, corresponding to the ADL constraint PWD/|P0W..P50W|. (https://specifications.openehr.org/releases/AM/Release-2.2.0/AOM1.4.html#_c_duration_class)"

| value             | years_allowed | months_allowed | weeks_allowed | days_allowed | hours_allowed | minutes_allowed | seconds_allowed | fractional_seconds_allowed | range.lower | range.upper | expected | violated constraints                             |
|-------------------|---------------|----------------|---------------|--------------|---------------|-----------------|-----------------|----------------------------|-------------|-------------|----------|--------------------------------------------------|
| P1Y               | true          | true           | true          | true         | true          | true            | true            | true                       | P0Y         | P50Y        | accepted |                                                  |
| P1Y               | true          | true           | true          | true         | true          | true            | true            | true                       | P2Y         | P50Y        | rejected | C_DURATION.range.lower                           |
| P1Y               | false         | true           | true          | true         | true          | true            | true            | true                       | P0Y         | P50Y        | rejected | C_DURATION.years_allowed                         |
| P1Y               | false         | true           | true          | true         | true          | true            | true            | true                       | P2Y         | P50Y        | rejected | C_DURATION.years_allowed, C_DURATION.range.lower |
| P1Y3M             | true          | true           | true          | true         | true          | true            | true            | true                       | P1Y         | P50Y        | accepted |                                                  |
| P1Y3M             | true          | false          | true          | true         | true          | true            | true            | true                       | P1Y         | P50Y        | rejected | C_DURATION.months_allowed                        |
| P1Y3M             | true          | true           | true          | true         | true          | true            | true            | true                       | P3Y         | P50Y        | rejected | C_DURATION.lower                                 |
| P1Y3M             | true          | false          | true          | true         | true          | true            | true            | true                       | P3Y         | P50Y        | rejected | C_DURATION.months_allowed. C_DURATION.lower      |
| PT2M43.5S         | true          | true           | true          | true         | true          | true            | true            | false                      | PT1M        | PT60M       | rejected | C_DURATION.fractional_seconds_allowed            |



## 4.3. quantity.date_time.DV_TIME

DV_TIME constraints are defined by C_TIME, which specifies two types of constraints: validity kind and range. The validity kind constraints are expressed in terms of mandatory/optional/prohibited flags for each part of the time expression: minute, second, millisecond and timezone. The range constraint is an Interval<Time>, which are both [Foundation Types](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html).

> NOTE: the Time class mentioned in the AOM 1.4 specification is actually the [Iso8601_time](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_iso8601_time_class) class. This is a [known bug](https://openehr.atlassian.net/browse/SPECPR-380) in the specs. So in C_TIME, range being an Interval<Time> should be an Interval<Iso8601_time>.

Time expressions in openEHR are considered an absolute point in time from the start of the current day, that is `T10` represents `10:00:00.000` AM in the local timezone.

### 4.3.1. Test case DV_TIME open constraint

This case is when DV_TIME matches {*}.

> NOTE 1: the decimal mark for the seconds fraction could be `,` (comma) or `.` (period) at in-memory and storage representations of time expressions, but since in most popular exchange formats
> the `.` is preferred, and considering the implementation of these test cases will surelly use those exchange formats, we only specify test data sets which use the decimal mark `.`.
> Nevetheless, the `,` is totally valid at in-memory and storage levels.
> In the same line, basic and extended formats are allowed at in-memory and storage representations. Basic format being the time parts without any separators and the extended being the parts
> with separatos `:` (colon). The extended format is also preferred by the most common exchange fornats, so only test data sets using extended format will be specified.

> NOTE 2: "There is no limit on the number of decimal places for the decimal fraction. However, the number of decimal places needs to be agreed to by the communicating parties." [REF](https://en.wikipedia.org/wiki/ISO_8601#Times)

> NOTE 3: the time marker `T` can be ommitted for the extended format in ISO8601:2019, because there is no risk of ambiguity. Since this is nor mandatory, our test data sets all include the `T` time marker.

> NOTE 4: if no timezone information is included, the time expression is considered `local time`.

> NOTE 5: one clarification about the seconds fraction in ISO8601 is that is not exactly an expression of milliseconds as the AOM specification implies considering the `millisecond_validity`
> field. For instance `.5` represents half a second, which is indeed 500 milliseconds but `.5` is not syntactically `500 ms`, or `.333333` represents one third of a second, and syntactically
> `333333` goes beyond the precision of milliseconds which is just 3 digits long. Consider `.33333` is totally valid in ISO8601 for the seconds fraction (see NOTE 2).

| value                  | expected | violated constraints                 |
|------------------------|----------|--------------------------------------|
| NULL                   | rejected | RM/Schema: value is mandatory        |
| ''                     | rejected | ISO8601: at least hours are required |
| T10                    | accepted |                                      |
| T48                    | rejected | ISO8601: hours in 0..23              |
| T10:30                 | accepted |                                      |
| T10:95                 | rejected | ISO8601: minutes in 0..59            |
| T10:30:47              | accepted |                                      |
| T10:30:78              | rejected | ISO8601: seconds in 0..59            |
| T10:30:47.5            | accepted |                                      |
| T10:30:47.333          | accepted |                                      |
| T10:30:47.333333       | accepted |                                      |
| T10:30:47Z             | accepted |                                      |
| T10:30:78Z             | rejected | ISO8601: seconds in 0..59            |
| T10:30:47.5Z           | accepted |                                      |
| T10:30:47.333Z         | accepted |                                      |
| T10:30:47.333333Z      | accepted |                                      |
| T10:30:47-03:00        | accepted |                                      |
| T10:30:78-03:00        | rejected | ISO8601: seconds in 0..59            |
| T10:30:47.5-03:00      | accepted |                                      |
| T10:30:47.333-03:00    | accepted |                                      |
| T10:30:47.333333-03:00 | accepted |                                      |
| T10.5                  | rejected | [openEHR doesn't allow fractional hours in partial time expressions](https://specifications.openehr.org/releases/BASE/latest/foundation_types.html#_primitive_time_types), an openEHR exception over the ISO 8601 spec |
| T10:05.5               | rejected | [openEHR doesn't allow fractional minutes in partial time expressions](https://specifications.openehr.org/releases/BASE/latest/foundation_types.html#_primitive_time_types), an openEHR exception over the ISO 8601 spec |


### 4.3.2. Test case DV_TIME validity kind constraint

> NOTE: the C_TIME has invariants that define if a lower precision component is optional or prohibited (e.g. minutes) then the higher precision components (e.g. seconds) should be optional or prohibited. In other words, if `minutes` is optional then `seconds` should be optional or prohibited. These invariants should be checked in an archetype/template editor, since that is part of archetype/template validation. Here we consider the archetypes and templates used are valid.

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

> NOTE: the Time class mentioned in the AOM 1.4 specification is actually the [Iso8601_time](https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_iso8601_time_class) class. This is a [known bug](https://openehr.atlassian.net/browse/SPECPR-380) in the specs. So in C_TIME, range being an Interval<Time> should be an Interval<Iso8601_time>.

> NOTE: there is an open issue in the comparability of two date/time/datetime expressions with different precisions but shared values for the components they contain. For instance, in ISO 8601-1_2019, the expression `T23:20` is referring to a specific hour and minute, and `T23` is referring to a specific hour. Then, numerically, it's not possible to say if `T23 < T23:20` or if `T23 > T23:20`. That is because the expressions represent different time components, which are really intervals of time, and one interval contains the oher (the 23rd hour of the day contains the minute 23:30). Though when the precisions are not the same but there are no shared components, then the expressions are comparable, for instance we can say `T22 < T2320`, because all the minutes and seconds in the 22nd hour of the day come before the minute 23:20. Similarly we can say `T22:45 < T23`, since the whole minute 22:45 comes before all minutes and seconds in the 23rd hour of the day. This issue is currently being discussed in the openEHR SEC because it changes the definition of the method is_strictly_comparable_to() for DV_DATE, DV_TIME and DV_DATE_TIME. This is commented here because this test case needs to compare time expressions to be able to check the range constraint.
>
> Besides noting that reduced precision time expressions represent an interval or range when those reduced precision time expressions are used as limits for an openEHR Interval, then it seems reasonable to interpret the whole range as the interval defined by the beginning of the lower limit and the end of the upper limit. For instance `T11` represents the whole 11th hour of the day, from start to end, and `T23` represents the whole 23rd hour of the day from start to end, then `T11..T23` represents all hours, minutes and seconds from the start of hour 11 to the end of hour 23 (yes the end not the start!). So something that might be counterintuitive by using this interpretation is: in this notation is `T23:30` would be contained in the `T11..T23` interval, though it is not strictly comparable to `T23`.
> 
> More about these considerations in the [openEHR discourse](https://discourse.openehr.org/t/conformance-data-validation-question-counterintuitive-c-time-range-constraints/2637).


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

Some exceptions to ISO 8601 specs is that in openEHR date/time/duration types, which dates back to RM 1.0.2 (and maybe before that):

> ISO 8601 semantics not used in openEHR include:
> • “expanded” dates, which have year numbers of greater than 4 digits, and may be negative; in openEHR, only 4-digit year numbers are assumed;
> • the YYYY-WW-DD method of expressing dates (since this is imprecise and difficult to compute with due to variable week starting dates, and not required in health);
> • partial date/times with fractional minutes or hours, e.g. hh,hhh or mm,mm; in openEHR, only fractional seconds are supported;
> • the interval syntax. Intervals of date/times are supported in openEHR, but their syntax form is defined by ADL, and is standardised across all comparable types, not just dates and times.

Following those rules, will include test data sets that break the openEHR rules, even if those are ISO 8601 valid, the SUT should mark them as invalid.

### 4.4.1. Test case DV_DATE open constraint

| value                  | expected | violated constraints          |
|------------------------|----------|-------------------------------|
| NULL                   | rejected | RM/Schema: value is mandatory |
| ''                     | rejected | openEHR RM/AOM: at least year is required <sup id="empty_date">[1](#footnote1)</sup> |
| 2021                   | accepted |                               |
| 2021-10                | accepted |                               |
| 2021-00                | rejected | ISO8601: month in 01..12      |
| 2021-13                | rejected | ISO8601: month in 01..12      |
| 2021-10-24             | accepted |                               |
| 2021-10-00             | rejected | ISO8601: day in 01..31        |
| 2021-10-32             | rejected | ISO8601: day in 01..31        |
| +001985-04             | rejected | expanded ISO 8601-1 dates are not supported in openEHR (see exceptions above) |
| 1985-W15-5             | rejected | week dates are allowed in ISO 8601-1 but are not supported in openEHR (see exceptions above) |


<b id="footnote1">1</b>: this is the author's interpretation of a minimal valid date in the context of openEHR noting the description of [C_DATE](https://specifications.openehr.org/releases/AM/Release-2.2.0/AOM1.4.html#_c_date_class): "There is no validity flag for ‘year’, since it must always be by definition mandatory in order to have a sensible date at all.". Though the ISO 8601 standard allows partial year expressions like `198` to denoted the `80's` and `19` to denote de `1900's`. [↩](#empty_date)


### 4.4.2. Test Case DV_DATE validity kind constraint

> NOTE: the C_DATE has invariants that define if a higher precision component is optional or prohibited, lower precision components should be optional or prohibited. In other words, if `month` is optional, `day` should be optional or prohibited. These invariants should be checked in an archetype editor and template editor, we consider the following tests to follow those rules without checking them, since that is related to archetype/template validation, not with data validation.

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
| ''                               | rejected | openEHR RM/AOM: at least year is required |
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

> NOTE: to verify the date time expressions we used this [regex](https://regex101.com/r/pYQJaW/1), you will note the rejected values don't match the regex.


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


| value                  | C_DATE_TIME.range               | expected | violated constraints          | note         |
|------------------------|---------------------------------|----------|-------------------------------|--------------|
| 2021                   | 1900..2030                      | accepted |                               |              |
| 2021                   | 2022..2030                      | rejected | C_DATE_TIME.range             |              |
| 2021                   | 1900..2020                      | rejected | C_DATE_TIME.range             |              |

| value                  | C_DATE_TIME.range               | expected | violated constraints          |              |
|------------------------|---------------------------------|----------|-------------------------------|--------------|
| 2021-10                | 1900-03..2030-07                | accepted |                               |              |
| 2021-10                | 2022-03..2030-07                | rejected | C_DATE_TIME.range             |              |
| 2021-10                | 1900-03..2020-07                | rejected | C_DATE_TIME.range             |              |

| value                  | C_DATE_TIME.range               | expected | violated constraints          |              |
|------------------------|---------------------------------|----------|-------------------------------|--------------|
| 2021-10-24             | 1900-03-13..2030-07-09          | accepted |                               |              |
| 2021-10-24             | 2022-03-13..2030-07-09          | rejected | C_DATE_TIME.range             |              |
| 2021-10-24             | 1900-03-13..2020-07-09          | rejected | C_DATE_TIME.range             |              |

| value                  | C_DATE_TIME.range               | expected | violated constraints          |              |
|------------------------|---------------------------------|----------|-------------------------------|--------------|
| 2021-05                | 1900..2030                      | accepted |                               |              |
| 2021-05                | 2022..2030                      | rejected | C_DATE_TIME.range             |              |
| 2021-05                | 1900..2021                      | accepted |                               | 1900..2021 is interpreted as 1900-01-01..2021-12-31, so 2021-05 is contained in that range |
| 2021                   | 2020-07..2022-03                | accepted |                               | 2020-07..2022-03 is interpreted as 2020-07-01..2022-03-31, and 2021 is 2021-01-01..2021-12-31, which is fully contained in the range constraint |

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
| 2021-10-24T10          | >=1900-03-13T00                                | accepted |                                    |
| 2021-10-24T10          | >=1900-03-13T00:00                             | accepted |                                    |
| 2021-10-24T10          | >=1900-03-13T00:00:00                          | accepted |                                    |
| 2021-10-24T10          | >=1900-03-13T00:00:00.0                        | accepted |                                    |
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

## 5.1. DV_GENERAL_TIME_SPECIFICATION

TBD: this data type might not be used or supported by modeling tools

## 5.2. DV_PERIODIC_TIME_SPECIFICATION

TBD: this data type might not be used or supported by modeling tools


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
| NULL                                                | rejected | RM/schema: value is required |
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


### 7.2.2. Test case DV_URI C_STRING pattern constraint for value

NOTE: to use the pattern constraint, the pattern should comply with the URI format from RFC3986, so the pattern defines a subset of valid URIs. If the pattern doesn't comply with the URI format, modeling tools should be responsible to notify the modeler and shouldn't allow to export archetypes or templates in that case. Testing this is not part of the data validation compliance scope, because it is validation of AOM objects not RM.

| value                                               | C_STRING.pattern | expected | violated constraints |
|-----------------------------------------------------|------------------|----------|----------------------|
| xyz                                                 | https://.*       | rejected | C_STRING.pattern     |
| https://cabolabs.com                                | https://.*       | accepted |                      |


### 7.2.3. Test case DV_URI C_STRING list constraint for value

NOTE: the values in the C_STRING.list should be valid URIs compliant with RFC3986. If this doesn't happen, the archetype/template is invalid. Testing this case is not in the scope of the data validation.

| value                | C_STRING.list                                      | expected | violated constraints |
|----------------------|----------------------------------------------------|----------|----------------------|
| xyz                  | [https://cabolabs.com, https://cloudehrserver.com] | rejected | C_STRING.list        |
| https://cabolabs.com | [https://cabolabs.com, https://cloudehrserver.com] | accepted |                      |




## 7.3. DV_EHR_URI

### 7.3.1. Test case DV_EHR_URI open constraint

| value                                               | expected | violated constraints              | notes |
|-----------------------------------------------------|----------|-----------------------------------|-------|
| NULL                                                | rejected | RM/schema: value is required      |       |
| xyz                                                 | rejected | value doesn't comply with RFC3986 |       |
| ftp://ftp.is.co.za/rfc/rfc1808.txt                  | rejected | URI doesn't have schema = 'ehr'   |       |
| http://www.ietf.org/rfc/rfc2396.txt                 | rejected | URI doesn't have schema = 'ehr'   |       |
| ldap://[2001:db8::7]/c=GB?objectClass?one           | rejected | URI doesn't have schema = 'ehr'   |       |
| mailto:John.Doe@example.com                         | rejected | URI doesn't have schema = 'ehr'   |       |
| news:comp.infosystems.www.servers.unix              | rejected | URI doesn't have schema = 'ehr'   |       |
| tel:+1-816-555-1212                                 | rejected | URI doesn't have schema = 'ehr'   |       |
| telnet://192.0.2.16:80/                             | rejected | URI doesn't have schema = 'ehr'   |       |
| urn:oasis:names:specification:docbook:dtd:xml:4.1.2 | rejected | URI doesn't have schema = 'ehr'   |       |
| http://www.carestreamserver/um/webapp_services/wado?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&contentType=application%2Fdicom | rejected | URI doesn't have schema = 'ehr' |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a           | accepted | | This should be a valid reference to an EHR |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1 | accepted | | This should be a valid reference to a COMPOSITION or FOLDER in an EHR (some top-level VERSIONED_OBJECT) |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1/context/other_context[at0001]/items[archetype_id=openEHR-EHR-CLUSTER.sample_symptom.v1]/items[at0034]/items[at0021]/value | accepted | | This should be a valid reference to a DATA_VALUE node in a COMPOSITION from an EHR |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a           | accepted | | Similar to the examples above, with given system_id as the URI `authority` |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1 | accepted | | Similar to the examples above, with given system_id as the URI `authority` |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a/031f2513-b9ef-47b2-bbef-8db24ae68c2f::EHRSERVER::1/context/other_context[at0001]/items[archetype_id=openEHR-EHR-CLUSTER.sample_symptom.v1]/items[at0034]/items[at0021]/value | accepted | | Similar to the examples above, with given system_id as the URI `authority` |


### 7.3.2. Test case DV_EHR_URI C_STRING pattern constraint for value

NOTE: to use the pattern constraint, the pattern should comply with the URI format from RFC3986, so the pattern defines a subset of valid URIs. If the pattern doesn't comply with the URI format, modeling tools should be responsible to notify the modeler and shouldn't allow to export archetypes or templates in that case. Testing this is not part of the data validation compliance scope, because it is validation of AOM objects not RM.

| value                                                      | C_STRING.pattern | expected | violated constraints |
|------------------------------------------------------------|------------------|----------|----------------------|
| xyz                                                        | ehr://.*         | rejected | C_STRING.pattern     |
| https://cabolabs.com                                       | ehr://.*         | rejected | C_STRING.pattern     |
| ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a | ehr://.*         | accepted |                      |


### 7.3.3. Test case DV_EHR_URI C_STRING list constraint for value

NOTE: the values in the C_STRING.list should be valid URIs compliant with RFC3986. If this doesn't happen, the archetype/template is invalid. Testing this case is not in the scope of the data validation.

| value                | C_STRING.list                                                                                                                | expected | violated constraints |
|----------------------|------------------------------------------------------------------------------------------------------------------------------|----------|----------------------|
| xyz                  | [ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a, ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a]                      | rejected | C_STRING.list        |
| https://cabolabs.com | [ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a, ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a]                      | rejected | C_STRING.list        |
| ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a | [ehr:/89c0752e-0815-47d7-8b3c-b3aaea2cea7a, ehr://CLOUD_EHRSERVER/89c0752e-0815-47d7-8b3c-b3aaea2cea7a] | accepted |                      |

