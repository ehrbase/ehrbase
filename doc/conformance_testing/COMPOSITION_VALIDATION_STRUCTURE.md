Composition Structural Validation (Conformance Testing)
--------------------------------------------------------

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2021-06-21) | First version with different cardinality constraints for each container attribute in the RM |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2021-11-03) | Updated style for cases accepted/rejected and index |
||||

# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [1. Introduction](#1-introduction)
  - [Glossary](#glossary)
  - [Implementation notes](#implementation-notes)
- [2. Top-level LOCATABLE class: COMPOSITION](#2-top-level-locatable-class-composition)
  - [Reference UML for COMPOSITION (latest RM)](#reference-uml-for-composition-latest-rm)
  - [2.1. COMPOSITION content cardinality 0..*, no constraint over context](#21-composition-content-cardinality-0-no-constraint-over-context)
  - [2.2. COMPOSITION content cardinality 1..*, no constraint over context](#22-composition-content-cardinality-1-no-constraint-over-context)
  - [2.3. COMPOSITION content cardinality 3..*, no constraint over context](#23-composition-content-cardinality-3-no-constraint-over-context)
  - [2.4. COMPOSITION content cardinality 0..1, no constraint over context](#24-composition-content-cardinality-01-no-constraint-over-context)
  - [2.5. COMPOSITION content cardinality 1..1, no constraint over context](#25-composition-content-cardinality-11-no-constraint-over-context)
  - [2.6. COMPOSITION content cardinality 3..5, no constraint over context](#26-composition-content-cardinality-35-no-constraint-over-context)
  - [2.7. COMPOSITION content cardinality 0..*, context occurrences 1..1](#27-composition-content-cardinality-0-context-occurrences-11)
  - [2.8. COMPOSITION content cardinality 1..*, context occurrences 1..1](#28-composition-content-cardinality-1-context-occurrences-11)
  - [2.9. COMPOSITION content cardinality 3..*, context occurrences 1..1](#29-composition-content-cardinality-3-context-occurrences-11)
  - [2.10. COMPOSITION content cardinality 0..1, context occurrences 1..1](#210-composition-content-cardinality-01-context-occurrences-11)
  - [2.11. COMPOSITION content cardinality 1..1, context occurrences 1..1](#211-composition-content-cardinality-11-context-occurrences-11)
  - [2.12. COMPOSITION content cardinality 3..5, context occurrences 1..1](#212-composition-content-cardinality-35-context-occurrences-11)
- [3. Internal LOCATABLE class: OBSERVATION](#3-internal-locatable-class-observation)
  - [3.1. OBSERVATION state existence = 0..1, protocol existence = 0..1](#31-observation-state-existence--01-protocol-existence--01)
  - [3.2. OBSERVATION state existence = 0..1, protocol existence = 1..1](#32-observation-state-existence--01-protocol-existence--11)
  - [3.3. OBSERVATION state existence = 1..1, protocol existence = 0..1](#33-observation-state-existence--11-protocol-existence--01)
  - [3.4. OBSERVATION state existence = 1..1, protocol existence = 1..1](#34-observation-state-existence--11-protocol-existence--11)
- [4. Internal LOCATABLE class: HISTORY](#4-internal-locatable-class-history)
  - [4.1. Reference UML for HISTORY and ITEM_STRUCTURE (latest RM)](#41-reference-uml-for-history-and-item_structure-latest-rm)
  - [4.2. HISTORY events cardinality 0..*, summary existence 0..1](#42-history-events-cardinality-0-summary-existence-01)
  - [4.3. HISTORY events cardinality 1..*, summary existence 0..1](#43-history-events-cardinality-1-summary-existence-01)
  - [4.4. HISTORY events cardinality 3..*, summary existence 0..1](#44-history-events-cardinality-3-summary-existence-01)
  - [4.5. HISTORY events cardinality 0..1, summary existence 0..1](#45-history-events-cardinality-01-summary-existence-01)
  - [4.6. HISTORY events cardinality 1..1, summary existence 0..1](#46-history-events-cardinality-11-summary-existence-01)
  - [4.7. HISTORY events cardinality 3..5, summary existence 0..1](#47-history-events-cardinality-35-summary-existence-01)
  - [4.8. HISTORY events cardinality 0..*, summary existence 1..1](#48-history-events-cardinality-0-summary-existence-11)
  - [4.9. HISTORY events cardinality 1..*, summary existence 1..1](#49-history-events-cardinality-1-summary-existence-11)
  - [4.10. HISTORY events cardinality 3..*, summary existence 1..1](#410-history-events-cardinality-3-summary-existence-11)
  - [4.11. HISTORY events cardinality 0..1, summary existence 1..1](#411-history-events-cardinality-01-summary-existence-11)
  - [4.12. HISTORY events cardinality 1..1, summary existence 1..1](#412-history-events-cardinality-11-summary-existence-11)
  - [4.13. HISTORY events cardinality 3..5, summary existence 1..1](#413-history-events-cardinality-35-summary-existence-11)
- [5. EVENT data sets and test cases](#5-event-data-sets-and-test-cases)
  - [5.1. EVENT state existence 0..1](#51-event-state-existence-01)
  - [5.2. EVENT state existence 1..1](#52-event-state-existence-11)
  - [5.3. EVENT is EVENT](#53-event-is-event)
  - [5.4. EVENT is POINT_EVENT](#54-event-is-point_event)
  - [5.5. EVENT is INTERVAL_EVENT](#55-event-is-interval_event)
- [6. ITEM_STRUCTURE data sets and test cases](#6-item_structure-data-sets-and-test-cases)
  - [6.1. ITEM_STRUCTURE is ITEM_STRUCTURE](#61-item_structure-is-item_structure)
  - [6.2. ITEM_STRUCTURE is ITEM_TREE](#62-item_structure-is-item_tree)
  - [6.3. ITEM_STRUCTURE is ITEM_LIST](#63-item_structure-is-item_list)
  - [6.4. ITEM_STRUCTURE is ITEM_TABLE](#64-item_structure-is-item_table)
  - [6.5. ITEM_STRUCTURE is ITEM_SINGLE](#65-item_structure-is-item_single)
<!--te-->


# 1. Introduction

The test cases defined here are for creating archetypes/templates expressing specific constraints over the openEHR RM. Then different data instances should be generated in order to test the constraints. It’s recommended to have at least one success case, one failure case and all border cases covered. That is, for each archetype constraint specified, at least three data instances should be created.

Since there are many combinations of constraints possible in the AOM/TOM, we will try to separate them into different classes and focus on each constraint set class independently from the other sets. The sets are defined by:

1. A top-level LOCATABLE class: COMPOSITION, EHR_STATUS, FOLDER, PARTY.
2. Constraint sets on top-level attributes for each class.
3. Internal LOCATABLE class: SECTION, ENTRY, HISTORY, ITEM_STRUCRTURE, ITEM, DATA_VALUE.
4. Constraint sets on internal structures and attributes (at any level in the RM hierarchy in the internal LOCATABLE class).

When checking a “multiple attribute” cardinality, this is a good set of combinations to test it:

- 0..*
- 1..*
- 3..*
- 0..1
- 1..1
- 3..5


## Glossary

* Archetypable class: any class that extends LOCATABLE.
* Archetypable field: generic fields on archetypable classes that can be constrained in archetypes.
* SUT: System Under Test
* RM: openEHR Reference Model
* AOM: Archetype Object Model (model of constraints)
* TOM: Template Object Model (model of constraints from the template 1.4 point of view)


## Implementation notes

The constraint combinations described in the cases below could be implemented in different archetypes, or in a generic archetype then defining the specific constraints at the template level. Which option to use might depend on the modeling tools used to create archetypes and templates.

We would suggest to automate the archetype/template test cases generation instead of creating each constraint combination manualy.

When there is no constraint defined for an attribute, it means anything is allowed on that attribute. It is recommended to include data not defined by the archetype, but valid in the RM, when generating the data instances.


# 2. Top-level LOCATABLE class: COMPOSITION

These cases are defined to verify the constraints defined over archetypable attributes of the top-level class COMPOSITION.

The constraints combinations described below could be tested in two ways:

1. Isolation: by not constraining the COMPOSITION.content at all, or adding an open/'any allowed' constraint {*} at the COMPOSITION.content in the archetype/template. This mean anything, even nothing, is accepted at the COMPOSITION.content at runtime.
2. Combination: with constraints set for COMPOSITION.content, for any CONTENT_ITEM (SECTION or ENTRY). Below there is a specification of the constraint combinations for each class accepted at COMPOSITION.content

> Note: we suggest to test with both strategies.

## Reference UML for COMPOSITION (latest RM)

Composition:

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-composition.svg)

Composition content:

![](https://specifications.openehr.org/releases/UML/latest/diagrams/diagram_Diagrams___18_1_83e026d_1433773264942_32842_8268.svg)

## 2.1. COMPOSITION content cardinality 0..*, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, success)
2. COMPOSITION with one entry (success)
3. COMPOSITION with 3 entries (success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_1 tbody > tr:nth-child(1),
#composition_1 tbody > tr:nth-child(2),
#composition_1 tbody > tr:nth-child(3),
#composition_1 tbody > tr:nth-child(4),
#composition_1 tbody > tr:nth-child(5),
#composition_1 tbody > tr:nth-child(6),
#composition_1 tbody > tr:nth-child(7),
#composition_1 tbody > tr:nth-child(8),
#composition_1 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

</style>

<div id="composition_1">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | accepted ||
| one entry        | no context                    | accepted ||
| three entries    | no context                    | accepted ||
| no entries       | context without other_context | accepted ||
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | accepted ||
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | accepted ||

</div>


## 2.2. COMPOSITION content cardinality 1..*, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (border case, success)
3. COMPOSITION with 3 entries (success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_2 tbody > tr:nth-child(2),
#composition_2 tbody > tr:nth-child(3),
#composition_2 tbody > tr:nth-child(5),
#composition_2 tbody > tr:nth-child(6),
#composition_2 tbody > tr:nth-child(8),
#composition_2 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_2 tbody > tr:nth-child(1),
#composition_2 tbody > tr:nth-child(4),
#composition_2 tbody > tr:nth-child(7),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_2">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:----------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | no context                    | accepted ||
| three entries    | no context                    | accepted ||
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | accepted ||

</div>

## 2.3. COMPOSITION content cardinality 3..*, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (fail)
3. COMPOSITION with 3 entries (border case, success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_3 tbody > tr:nth-child(3),
#composition_3 tbody > tr:nth-child(6),
#composition_3 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_3 tbody > tr:nth-child(1),
#composition_3 tbody > tr:nth-child(2),
#composition_3 tbody > tr:nth-child(4),
#composition_3 tbody > tr:nth-child(5),
#composition_3 tbody > tr:nth-child(7),
#composition_3 tbody > tr:nth-child(8),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_3">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | no context                    | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | no context                    | accepted ||
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context with other_context    | accepted ||

</div>

## 2.4. COMPOSITION content cardinality 0..1, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, success)
2. COMPOSITION with one entry (border case, success)
3. COMPOSITION with 3 entries (fail)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_4 tbody > tr:nth-child(1),
#composition_4 tbody > tr:nth-child(2),
#composition_4 tbody > tr:nth-child(4),
#composition_4 tbody > tr:nth-child(5),
#composition_4 tbody > tr:nth-child(7),
#composition_4 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#composition_4 tbody > tr:nth-child(3),
#composition_4 tbody > tr:nth-child(6),
#composition_4 tbody > tr:nth-child(9),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_4">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | accepted ||
| one entry        | no context                    | accepted ||
| three entries    | no context                    | rejected | COMPOSITION.content: cardinality.upper |
| no entries       | context without other_context | accepted ||
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | rejected | COMPOSITION.content: cardinality.upper |
| no entries       | context with other_context    | accepted ||
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | rejected | COMPOSITION.content: cardinality.upper |

</div>

## 2.5. COMPOSITION content cardinality 1..1, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (border case, success)
3. COMPOSITION with 3 entries (fail)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_5 tbody > tr:nth-child(2),
#composition_5 tbody > tr:nth-child(5),
#composition_5 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#composition_5 tbody > tr:nth-child(1),
#composition_5 tbody > tr:nth-child(3),
#composition_5 tbody > tr:nth-child(4),
#composition_5 tbody > tr:nth-child(6),
#composition_5 tbody > tr:nth-child(7),
#composition_5 tbody > tr:nth-child(9),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_5">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | no context                    | accepted ||
| three entries    | no context                    | rejected | COMPOSITION.content: cardinality.upper |
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | rejected | COMPOSITION.content: cardinality.upper |
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | rejected | COMPOSITION.content: cardinality.upper |

</div>


## 2.6. COMPOSITION content cardinality 3..5, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (fail)
2. COMPOSITION with one entry (fail)
3. COMPOSITION with 3 entries (border case, success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_6 tbody > tr:nth-child(3),
#composition_6 tbody > tr:nth-child(6),
#composition_6 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_6 tbody > tr:nth-child(1),
#composition_6 tbody > tr:nth-child(2),
#composition_6 tbody > tr:nth-child(4),
#composition_6 tbody > tr:nth-child(5),
#composition_6 tbody > tr:nth-child(7),
#composition_6 tbody > tr:nth-child(8),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_6">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | no context                    | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | no context                    | accepted ||
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context with other_context    | accepted ||

</div>

## 2.7. COMPOSITION content cardinality 0..*, context occurrences 1..1

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, success)
2. COMPOSITION with one entry (success)
3. COMPOSITION with 3 entries (success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_7 tbody > tr:nth-child(4),
#composition_7 tbody > tr:nth-child(5),
#composition_7 tbody > tr:nth-child(6),
#composition_7 tbody > tr:nth-child(7),
#composition_7 tbody > tr:nth-child(8),
#composition_7 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_7 tbody > tr:nth-child(1),
#composition_7 tbody > tr:nth-child(2),
#composition_7 tbody > tr:nth-child(3),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_7">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.context occurrences.lower |
| one entry        | no context                    | rejected | COMPOSITION.context occurrences.lower |
| three entries    | no context                    | rejected | COMPOSITION.context occurrences.lower |
| no entries       | context without other_context | accepted ||
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | accepted ||
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | accepted ||

</div>


## 2.8. COMPOSITION content cardinality 1..*, context occurrences 1..1

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (border case, success)
3. COMPOSITION with 3 entries (success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_8 tbody > tr:nth-child(5),
#composition_8 tbody > tr:nth-child(6),
#composition_8 tbody > tr:nth-child(8),
#composition_8 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_8 tbody > tr:nth-child(1),
#composition_8 tbody > tr:nth-child(2),
#composition_8 tbody > tr:nth-child(3),
#composition_8 tbody > tr:nth-child(4),
#composition_8 tbody > tr:nth-child(7),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_8">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:----------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower, COMPOSITION.context occurrences.lower |
| one entry        | no context                    | rejected | COMPOSITION.context occurrences.lower |
| three entries    | no context                    | rejected | COMPOSITION.context occurrences.lower |
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | accepted ||

</div>


## 2.9. COMPOSITION content cardinality 3..*, context occurrences 1..1

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (fail)
3. COMPOSITION with 3 entries (border case, success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_9 tbody > tr:nth-child(6),
#composition_9 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_9 tbody > tr:nth-child(1),
#composition_9 tbody > tr:nth-child(2),
#composition_9 tbody > tr:nth-child(3),
#composition_9 tbody > tr:nth-child(4),
#composition_9 tbody > tr:nth-child(5),
#composition_9 tbody > tr:nth-child(7),
#composition_9 tbody > tr:nth-child(8),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_9">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower, COMPOSITION.context occurrences.lower  |
| one entry        | no context                    | rejected | COMPOSITION.content: cardinality.lower, COMPOSITION.context occurrences.lower  |
| three entries    | no context                    | rejected | COMPOSITION.context occurrences.lower |
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context with other_context    | accepted ||

</div>

## 2.10. COMPOSITION content cardinality 0..1, context occurrences 1..1

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, success)
2. COMPOSITION with one entry (border case, success)
3. COMPOSITION with 3 entries (fail)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_10 tbody > tr:nth-child(4),
#composition_10 tbody > tr:nth-child(5),
#composition_10 tbody > tr:nth-child(7),
#composition_10 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#composition_10 tbody > tr:nth-child(1),
#composition_10 tbody > tr:nth-child(2),
#composition_10 tbody > tr:nth-child(3),
#composition_10 tbody > tr:nth-child(6),
#composition_10 tbody > tr:nth-child(9),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_10">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.context occurrences.lower |
| one entry        | no context                    | rejected | COMPOSITION.context occurrences.lower |
| three entries    | no context                    | rejected | COMPOSITION.content: cardinality.upper, COMPOSITION.context occurrences.lower |
| no entries       | context without other_context | accepted ||
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | rejected | COMPOSITION.content: cardinality.upper |
| no entries       | context with other_context    | accepted ||
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | rejected | COMPOSITION.content: cardinality.upper |

</div>

## 2.11. COMPOSITION content cardinality 1..1, context occurrences 1..1

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (border case, success)
3. COMPOSITION with 3 entries (fail)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_11 tbody > tr:nth-child(5),
#composition_11 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#composition_11 tbody > tr:nth-child(1),
#composition_11 tbody > tr:nth-child(2),
#composition_11 tbody > tr:nth-child(3),
#composition_11 tbody > tr:nth-child(4),
#composition_11 tbody > tr:nth-child(6),
#composition_11 tbody > tr:nth-child(7),
#composition_11 tbody > tr:nth-child(9),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_11">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower, COMPOSITION.context occurrences.lower |
| one entry        | no context                    | rejected | COMPOSITION.context occurrences.lower |
| three entries    | no context                    | rejected | COMPOSITION.content: cardinality.upper, COMPOSITION.context occurrences.lower |
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | accepted ||
| three entries    | context without other_context | rejected | COMPOSITION.content: cardinality.upper |
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | accepted ||
| three entries    | context with other_context    | rejected | COMPOSITION.content: cardinality.upper |

</div>

## 2.12. COMPOSITION content cardinality 3..5, context occurrences 1..1

COMPOSITION data sets:

1. COMPOSITION with no entries (fail)
2. COMPOSITION with one entry (fail)
3. COMPOSITION with 3 entries (border case, success)

Combine those cases with:

1. COMPOSITION with no context
2. COMPOSITION with context but no other_context
3. COMPOSITION with context and other_context

All the context structures should be valid.

<style>

#composition_12 tbody > tr:nth-child(6),
#composition_12 tbody > tr:nth-child(9),
.accepted {
  background-color: #ccffcc;
}

#composition_12 tbody > tr:nth-child(1),
#composition_12 tbody > tr:nth-child(2),
#composition_12 tbody > tr:nth-child(3),
#composition_12 tbody > tr:nth-child(4),
#composition_12 tbody > tr:nth-child(5),
#composition_12 tbody > tr:nth-child(7),
#composition_12 tbody > tr:nth-child(8),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="composition_12">

| content          | context                       | expected | constraints violated |
|:-----------------|:------------------------------|:--------:|:---------------------|
| no entries       | no context                    | rejected | COMPOSITION.content: cardinality.lower, COMPOSITION.context occurrences.lower |
| one entry        | no context                    | rejected | COMPOSITION.content: cardinality.lower, COMPOSITION.context occurrences.lower |
| three entries    | no context                    | rejected | COMPOSITION.context occurrences.lower |
| no entries       | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context without other_context | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context without other_context | accepted ||
| no entries       | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| one entry        | context with other_context    | rejected | COMPOSITION.content: cardinality.lower |
| three entries    | context with other_context    | accepted ||

</div>



# 3. Internal LOCATABLE class: OBSERVATION

In this section there are specifications of constraint combinations for OBSERVATION.

Each data set in this section could be combined with the test data sets for COMPOSITION.content defined in section 2.

OBSERVATION data sets:

1. OBSERVATION with no state and no protocol
2. OBSERVATION with no state and protocol
3. OBSERVATION with state and no protocol
4. OBSERVATION with state and protocol

> Note: since OBSERVATION.data is mandatory by the RM we can't have a case for an AOM constraint with "no OBSERVATION.data". Though any OBSERVATION committed to the SUT without data will return a validation error comming from the RM/Schema, and this should be tested.


The constraints combinations described below could be tested in two ways:

1. Isolation: by not constraining OBSERVATION.data, OBSERVATION.state and OBSERVATION.protocol, or using the open/'any allowed' constraint {*} for those attributes.
2. Combination: with constraints defined at the HISTORY level (for data and state) and ITEM_STRUCTURE (for protocol). 

> Note: we suggest to test with both strategies.


## 3.1. OBSERVATION state existence = 0..1, protocol existence = 0..1

<style>

#obs_1 tbody > tr:nth-child(5),
#obs_1 tbody > tr:nth-child(6),
#obs_1 tbody > tr:nth-child(7),
#obs_1 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#obs_1 tbody > tr:nth-child(1),
#obs_1 tbody > tr:nth-child(2),
#obs_1 tbody > tr:nth-child(3),
#obs_1 tbody > tr:nth-child(4),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="obs_1">

| data         | state      | protocol    | expected | constraints violated |
|:-------------|:-----------|:-----------:|----------|----------------------|
| absent       | absent     | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| absent       | absent     | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| absent       | present    | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| absent       | present    | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| present      | absent     | absent      | accepted |  |
| present      | absent     | present     | accepted |  |
| present      | present    | absent      | accepted |  |
| present      | present    | present     | accepted |  |

</div>


## 3.2. OBSERVATION state existence = 0..1, protocol existence = 1..1

<style>

#obs_2 tbody > tr:nth-child(6),
#obs_2 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#obs_2 tbody > tr:nth-child(1),
#obs_2 tbody > tr:nth-child(2),
#obs_2 tbody > tr:nth-child(3),
#obs_2 tbody > tr:nth-child(4),
#obs_2 tbody > tr:nth-child(5),
#obs_2 tbody > tr:nth-child(7),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="obs_2">

| data         | state      | protocol    | expected | constraints violated |
|:-------------|:-----------|:-----------:|----------|----------------------|
| absent       | absent     | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.protocol existence.lower |
| absent       | absent     | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| absent       | present    | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.protocol existence.lower |
| absent       | present    | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| present      | absent     | absent      | rejected | OBSERVATION.protocol existence.lower |
| present      | absent     | present     | accepted |  |
| present      | present    | absent      | rejected | OBSERVATION.protocol existence.lower |
| present      | present    | present     | accepted |  |

</div>


## 3.3. OBSERVATION state existence = 1..1, protocol existence = 0..1

<style>

#obs_3 tbody > tr:nth-child(7),
#obs_3 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#obs_3 tbody > tr:nth-child(1),
#obs_3 tbody > tr:nth-child(2),
#obs_3 tbody > tr:nth-child(3),
#obs_3 tbody > tr:nth-child(4),
#obs_3 tbody > tr:nth-child(5),
#obs_3 tbody > tr:nth-child(6),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="obs_3">

| data         | state      | protocol    | expected | constraints violated |
|:-------------|:-----------|:-----------:|----------|----------------------|
| absent       | absent     | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.state existence.lower |
| absent       | absent     | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.state existence.lower |
| absent       | present    | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| absent       | present    | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| present      | absent     | absent      | rejected | OBSERVATION.state existence.lower |
| present      | absent     | present     | rejected | OBSERVATION.state existence.lower |
| present      | present    | absent      | accepted |  |
| present      | present    | present     | accepted |  |

</div>


## 3.4. OBSERVATION state existence = 1..1, protocol existence = 1..1

<style>

#obs_4 tbody > tr:nth-child(8),
.accepted {
  background-color: #ccffcc;
}

#obs_4 tbody > tr:nth-child(1),
#obs_4 tbody > tr:nth-child(2),
#obs_4 tbody > tr:nth-child(3),
#obs_4 tbody > tr:nth-child(4),
#obs_4 tbody > tr:nth-child(5),
#obs_4 tbody > tr:nth-child(6),
#obs_4 tbody > tr:nth-child(7),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="obs_4">

| data         | state      | protocol    | expected | constraints violated |
|:-------------|:-----------|:-----------:|----------|----------------------|
| absent       | absent     | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.protocol existence.lower, OBSERVATION.state existence.lower |
| absent       | absent     | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.state existence.lower |
| absent       | present    | absent      | rejected | OBSERVATION.data existence.lower (RM/schema constraint), OBSERVATION.protocol existence.lower |
| absent       | present    | present     | rejected | OBSERVATION.data existence.lower (RM/schema constraint) |
| present      | absent     | absent      | rejected | OBSERVATION.protocol existence.lower, OBSERVATION.state existence.lower |
| present      | absent     | present     | rejected | OBSERVATION.state existence.lower |
| present      | present    | absent      | rejected | OBSERVATION.protocol existence.lower |
| present      | present    | present     | accepted |  |

</div>



# 4. Internal LOCATABLE class: HISTORY

In this section there are specifications of constraint combinations for HISTORY.

Each data set in this section could be combined with the test data sets for HISTORY defined in section 3.

HISTORY data sets:

1. HISTORY with no events and no summary
2. HISTORY with events and no summary
3. HISTORY with no events and summary
4. HISTORY with events and summary


The constraints combinations described below could be tested in two ways:

1. Isolation: by not constraining HISTORY.events and HISTORY.summary, or using the open/'any allowed' constraint {*} for those attributes.
2. Combination: with constraints defined at the EVENT level (for events) and ITEM_STRUCTURE (for summary). 

> Note: we suggest to test with both strategies.

## 4.1. Reference UML for HISTORY and ITEM_STRUCTURE (latest RM)

![](https://specifications.openehr.org/releases/UML/latest/diagrams/diagram_Diagrams___18_1_83e026d_1433773264324_187743_6733.svg)


## 4.2. HISTORY events cardinality 0..*, summary existence 0..1

<style>

#history_1 tbody > tr:nth-child(1),
#history_1 tbody > tr:nth-child(2),
#history_1 tbody > tr:nth-child(3),
#history_1 tbody > tr:nth-child(4),
#history_1 tbody > tr:nth-child(5),
#history_1 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

</style>

<div id="history_1">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | accepted |  |
| one event        | absent          | accepted |  |
| three events     | absent          | accepted |  |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## 4.3. HISTORY events cardinality 1..*, summary existence 0..1

<style>

#history_2 tbody > tr:nth-child(2),
#history_2 tbody > tr:nth-child(3),
#history_2 tbody > tr:nth-child(5),
#history_2 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_2 tbody > tr:nth-child(1),
#history_2 tbody > tr:nth-child(4),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_2">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower |
| one event        | absent          | accepted |  |
| three events     | absent          | accepted |  |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## 4.4. HISTORY events cardinality 3..*, summary existence 0..1

<style>

#history_3 tbody > tr:nth-child(3),
#history_3 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_3 tbody > tr:nth-child(1),
#history_3 tbody > tr:nth-child(2),
#history_3 tbody > tr:nth-child(4),
#history_3 tbody > tr:nth-child(5),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_3">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower |
| one event        | absent          | rejected | HISTORY.events cardinality.lower |
| three events     | absent          | accepted |  |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | rejected | HISTORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


## 4.5. HISTORY events cardinality 0..1, summary existence 0..1

<style>

#history_4 tbody > tr:nth-child(1),
#history_4 tbody > tr:nth-child(2),
#history_4 tbody > tr:nth-child(4),
#history_4 tbody > tr:nth-child(5),
.accepted {
  background-color: #ccffcc;
}

#history_4 tbody > tr:nth-child(3),
#history_4 tbody > tr:nth-child(6),
.rejected {
  background-color: #ffcccc;
}

</style>


<div id="history_4">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | accepted |  |
| one event        | absent          | accepted |  |
| three events     | absent          | rejected | HISTORY.events cardinality.upper |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HISTORY.events cardinality.upper |

</div>


## 4.6. HISTORY events cardinality 1..1, summary existence 0..1

<style>

#history_5 tbody > tr:nth-child(2),
#history_5 tbody > tr:nth-child(5),
.accepted {
  background-color: #ccffcc;
}

#history_5 tbody > tr:nth-child(1),
#history_5 tbody > tr:nth-child(3),
#history_5 tbody > tr:nth-child(4),
#history_5 tbody > tr:nth-child(6),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_5">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower |
| one event        | absent          | accepted |  |
| three events     | absent          | rejected | HISTORY.events cardinality.upper |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HISTORY.events cardinality.upper |

</div>


## 4.7. HISTORY events cardinality 3..5, summary existence 0..1

<style>

#history_6 tbody > tr:nth-child(3),
#history_6 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_6 tbody > tr:nth-child(1),
#history_6 tbody > tr:nth-child(2),
#history_6 tbody > tr:nth-child(4),
#history_6 tbody > tr:nth-child(5),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_6">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower |
| one event        | absent          | rejected | HISTORY.events cardinality.lower |
| three events     | absent          | accepted |  |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | rejected | HISTORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


## 4.8. HISTORY events cardinality 0..*, summary existence 1..1

<style>

#history_7 tbody > tr:nth-child(4),
#history_7 tbody > tr:nth-child(5),
#history_7 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_7 tbody > tr:nth-child(1),
#history_7 tbody > tr:nth-child(2),
#history_7 tbody > tr:nth-child(3),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_7">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## 4.9. HISTORY events cardinality 1..*, summary existence 1..1

<style>

#history_8 tbody > tr:nth-child(5),
#history_8 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_8 tbody > tr:nth-child(1),
#history_8 tbody > tr:nth-child(2),
#history_8 tbody > tr:nth-child(3),
#history_8 tbody > tr:nth-child(4),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_8">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## 4.10. HISTORY events cardinality 3..*, summary existence 1..1

<style>

#history_9 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_9 tbody > tr:nth-child(1),
#history_9 tbody > tr:nth-child(2),
#history_9 tbody > tr:nth-child(3),
#history_9 tbody > tr:nth-child(4),
#history_9 tbody > tr:nth-child(5),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_9">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.events cardinality.lower, HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | rejected | HISTORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


## 4.11. HISTORY events cardinality 0..1, summary existence 1..1

<style>

#history_10 tbody > tr:nth-child(4),
#history_10 tbody > tr:nth-child(5),
.accepted {
  background-color: #ccffcc;
}

#history_10 tbody > tr:nth-child(1),
#history_10 tbody > tr:nth-child(2),
#history_10 tbody > tr:nth-child(3),
#history_10 tbody > tr:nth-child(6),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_10">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.events cardinality.upper, HISTORY.summary existence.lower |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HISTORY.events cardinality.upper |

</div>


## 4.12. HISTORY events cardinality 1..1, summary existence 1..1

<style>

#history_11 tbody > tr:nth-child(5),
.accepted {
  background-color: #ccffcc;
}

#history_11 tbody > tr:nth-child(1),
#history_11 tbody > tr:nth-child(2),
#history_11 tbody > tr:nth-child(3),
#history_11 tbody > tr:nth-child(4),
#history_11 tbody > tr:nth-child(6),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_11">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.events cardinality.upper, HISTORY.summary existence.lower |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HISTORY.events cardinality.upper |

</div>


## 4.13. HISTORY events cardinality 3..5, summary existence 1..1

<style>

#history_12 tbody > tr:nth-child(6),
.accepted {
  background-color: #ccffcc;
}

#history_12 tbody > tr:nth-child(1),
#history_12 tbody > tr:nth-child(2),
#history_12 tbody > tr:nth-child(3),
#history_12 tbody > tr:nth-child(4),
#history_12 tbody > tr:nth-child(5),
.rejected {
  background-color: #ffcccc;
}

</style>

<div id="history_12">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.events cardinality.lower, HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | rejected | HISTORY.events cardinality.lower |
| one event        | present         | rejected | HISTORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


# 5. EVENT data sets and test cases

## Reference UML

![](https://specifications.openehr.org/releases/RM/Release-1.1.0/UML/diagrams/RM-data_structures.svg)

EVENT data sets:

1. EVENT with no state
2. EVENT with state

> Note: since EVENT.data is mandatory by the RM we can't have a case for an AOM constraint with "no EVENT.data". Though any EVENT committed to the SUT without data will return a validation error comming from the RM/Schema, and this should be tested.


EVENT type combinations:

1. EVENT is POINT_EVENT
2. EVENT is INTERVAL_EVENT

> Note: testing both EVENT subclasses shouldn't affect the result of testing combinations with the rest of the constraints defined for EVENT or on container classes. It will affect only the type checking test if the wrong type of EVENT is provided. So instead of combining the expected results with the rest of the constraints, we will define separate test cases.

The constraints combinations described below could be tested in two ways:

1. Isolation: by not constraining EVENT.data and EVENT.state, or using the open/'any allowed' constraint {*} for those attributes.
2. Combination: with constraints defined at the ITEM_STRUCTURE level (for data and state).

> Note: we suggest to test with both strategies.

## 5.1. EVENT state existence 0..1

<div id="evt_1">

| data         | state      | expected | constraints violated |
|:-------------|:-----------|----------|----------------------|
| absent       | absent     | rejected | EVENT.data existence.lower (RM/schema constraint) |
| absent       | present    | rejected | EVENT.data existence.lower (RM/schema constraint) |
| present      | absent     | accepted |  |
| present      | present    | accepted |  |

</div>

## 5.2. EVENT state existence 1..1

<div id="evt_2">

| data         | state      | expected | constraints violated |
|:-------------|:-----------|----------|----------------------|
| absent       | absent     | rejected | EVENT.data existence.lower (RM/schema constraint), EVENT.state existence.lower |
| absent       | present    | rejected | EVENT.data existence.lower (RM/schema constraint) |
| present      | absent     | rejected | EVENT.state existence.lower |
| present      | present    | accepted |  |

</div>

## 5.3. EVENT is EVENT (type validation)

The title of this case is correct, it means: in the AOM/TOM the constraint for the EVENT type is using the abstract class EVENT, so it allows any EVENT subclass at runtime.

<div id="evt_3">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| POINT_EVENT    | accepted |  |
| INTERVAL_EVENT | accepted |  |

</div>

## 5.4. EVENT is POINT_EVENT (type validation)

<div id="evt_3">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| POINT_EVENT    | accepted |  |
| INTERVAL_EVENT | rejected | Class not allowed |

</div>

## 5.5. EVENT is INTERVAL_EVENT (type validation)

<div id="evt_3">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| POINT_EVENT    | rejected | Class not allowed |
| INTERVAL_EVENT | accepted |  |

</div>


# 6. ITEM_STRUCTURE data sets and test cases

ITEM_STRUCTURE type combinations:

1. ITEM_STRUCTURE is ITEM_TREE
2. ITEM_STRUCTURE is ITEM_LIST
3. ITEM_STRUCTURE is ITEM_TABLE
4. ITEM_STRUCTURE is ITEM_SINGLE

> Note: testing with any of the ITEM_STRUCTURE subclasses shouldn't affect the result of testing combinations with the rest of the constraints defined on container classes. It will affect only the type checking test if the wrong type of ITEM_STRUCTRE is provided. So instead of combining the expected results with the rest of the constraints, we will define separate test cases.

## 6.1. ITEM_STRUCTURE is any ITEM_STRUCTURE (type validation)

The title of this case is correct, it means, in the AOM/TOM the constraint for the ITEM_STRUCTURE type is using an open constraint, so it allows any ITEM_STRUCTURE subclass at this position at runtime.

<div id="str_1">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| ITEM_TREE      | accepted |  |
| ITEM_LIST      | accepted |  |
| ITEM_TABLE     | accepted |  |
| ITEM_SINGLE    | accepted |  |

</div>

## 6.2. ITEM_STRUCTURE is ITEM_TREE (type validation)

<div id="str_2">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| ITEM_TREE      | accepted |  |
| ITEM_LIST      | rejected | Class not allowed |
| ITEM_TABLE     | rejected | Class not allowed |
| ITEM_SINGLE    | rejected | Class not allowed |

</div>


## 6.3. ITEM_STRUCTURE is ITEM_LIST (type validation)

<div id="str_3">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| ITEM_TREE      | rejected | Class not allowed |
| ITEM_LIST      | accepted |  |
| ITEM_TABLE     | rejected | Class not allowed |
| ITEM_SINGLE    | rejected | Class not allowed |

</div>


## 6.4. ITEM_STRUCTURE is ITEM_TABLE (type validation)

<div id="str_4">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| ITEM_TREE      | rejected | Class not allowed |
| ITEM_LIST      | rejected | Class not allowed |
| ITEM_TABLE     | accepted |  |
| ITEM_SINGLE    | rejected | Class not allowed |

</div>


## 6.5. ITEM_STRUCTURE is ITEM_SINGLE (type validation)

<div id="str_5">

| event          | expected | constraints violated |
|:---------------|----------|----------------------|
| ITEM_TREE      | rejected | Class not allowed |
| ITEM_LIST      | rejected | Class not allowed |
| ITEM_TABLE     | rejected | Class not allowed |
| ITEM_SINGLE    | accepted |  |

</div>

## 6.6. ITEM_TREE.items cardinality 0..*
## 6.7. ITEM_TREE.items cardinality 1..*
## 6.8. ITEM_TREE.items cardinality 3..*
## 6.9. ITEM_TREE.items cardinality 0..1
## 6.10. ITEM_TREE.items cardinality 1..1
## 6.11. ITEM_TREE.items cardinality 3..5

## 6.12. ITEM_LIST.items cardinality 0..*
## 6.13. ITEM_LIST.items cardinality 1..*
## 6.14. ITEM_LIST.items cardinality 3..*
## 6.15. ITEM_LIST.items cardinality 0..1
## 6.16. ITEM_LIST.items cardinality 1..1
## 6.17. ITEM_LIST.items cardinality 3..5

## 6.18. ITEM_TABLE.rows cardinality 0..*
## 6.19. ITEM_TABLE.rows cardinality 1..*
## 6.20. ITEM_TABLE.rows cardinality 3..*
## 6.21. ITEM_TABLE.rows cardinality 0..1
## 6.22. ITEM_TABLE.rows cardinality 1..1
## 6.23. ITEM_TABLE.rows cardinality 3..5


# 7. ITEM data sets and test cases

## 7.1. ITEM is any ITEM (type validation)
## 7.2. ITEM is CLUSTER (type validation)
## 7.3. ITEM is ELEMENT (type validation)

## 7.4. CLUSTER.items cardinality 0..*
## 7.5. CLUSTER.items cardinality 1..*
## 7.6. CLUSTER.items cardinality 3..*
## 7.7. CLUSTER.items cardinality 0..1
## 7.8. CLUSTER.items cardinality 1..1
## 7.9. CLUSTER.items cardinality 3..5

## 7.8. ELEMENT.value existence = 0..1
## 7.9. ELEMENT.value existence = 1..1
## 7.10. ELEMENT.null_flavour existence = 0..1
## 7.11. ELEMENT.null_flavour existence = 1..1

> We need to consider the invariant of value XOR null_flavour

TODO: The cases for the different ELEMENT.value types will be in the data types component

TODO: Add type test case for LOCATABLE.name having it's type defined as DV_CODED_TEXT

TODO: Add occurrences test cases for items contained in container attributes (miultiple attributes)