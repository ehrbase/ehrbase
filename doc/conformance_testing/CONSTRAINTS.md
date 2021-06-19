openEHR Data Validation Conformance
-----------------------------------

# TOC

- [TOC](#toc)
- [1. Introduction](#1-introduction)
- [2. Top-level LOCATABLE class: COMPOSITION](#2-top-level-locatable-class-composition)
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
  - [HISTORY events cardinality 0..*, summary existence 0..1](#history-events-cardinality-0-summary-existence-01)
  - [HISTORY events cardinality 1..*, summary existence 0..1](#history-events-cardinality-1-summary-existence-01)
  - [HISTORY events cardinality 3..*, summary existence 0..1](#history-events-cardinality-3-summary-existence-01)
  - [HISTORY events cardinality 0..1, summary existence 0..1](#history-events-cardinality-01-summary-existence-01)
  - [HISTORY events cardinality 1..1, summary existence 0..1](#history-events-cardinality-11-summary-existence-01)
  - [HISTORY events cardinality 3..5, summary existence 0..1](#history-events-cardinality-35-summary-existence-01)
  - [HISTORY events cardinality 0..*, summary existence 1..1](#history-events-cardinality-0-summary-existence-11)
  - [HISTORY events cardinality 1..*, summary existence 1..1](#history-events-cardinality-1-summary-existence-11)
  - [HISTORY events cardinality 3..*, summary existence 1..1](#history-events-cardinality-3-summary-existence-11)
  - [HISTORY events cardinality 0..1, summary existence 1..1](#history-events-cardinality-01-summary-existence-11)
  - [HISTORY events cardinality 1..1, summary existence 1..1](#history-events-cardinality-11-summary-existence-11)
  - [HISTORY events cardinality 3..5, summary existence 1..1](#history-events-cardinality-35-summary-existence-11)
- [EVENT data sets (WIP)](#event-data-sets-wip)
    - [EVENT data existence 0..1, state existence 0..1](#event-data-existence-01-state-existence-01)
    - [OBSERVATION data.events cardinality = 1..*, state.existence = 0..1, protocol.existence = 0..1](#observation-dataevents-cardinality--1-stateexistence--01-protocolexistence--01)
    - [OBSERVATION events cardinality = 3..*, state.existence = 0..1, protocol.existence = 0..1](#observation-events-cardinality--3-stateexistence--01-protocolexistence--01)
    - [OBSERVATION events cardinality = 0..1, state.existence = 0..1, protocol.existence = 0..1](#observation-events-cardinality--01-stateexistence--01-protocolexistence--01)
    - [OBSERVATION events cardinality = 1..1, state.existence = 0..1, protocol.existence = 0..1](#observation-events-cardinality--11-stateexistence--01-protocolexistence--01)
    - [OBSERVATION events cardinality = 3..5, state.existence = 0..1, protocol.existence = 0..1](#observation-events-cardinality--35-stateexistence--01-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 0..*, state.existence = 1..1, protocol.existence = 0..1](#observation-dataevents-cardinality--0-stateexistence--11-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 1..*, state.existence = 1..1, protocol.existence = 0..1](#observation-dataevents-cardinality--1-stateexistence--11-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 3..*, state.existence = 1..1, protocol.existence = 0..1](#observation-dataevents-cardinality--3-stateexistence--11-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 0..1, state.existence = 1..1, protocol.existence = 0..1](#observation-dataevents-cardinality--01-stateexistence--11-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 1..1, state.existence = 1..1, protocol.existence = 0..1](#observation-dataevents-cardinality--11-stateexistence--11-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 3..5, state.existence = 1..1, protocol.existence = 0..1](#observation-dataevents-cardinality--35-stateexistence--11-protocolexistence--01)
    - [OBSERVATION data.events cardinality = 0..*, state.existence = 0..1, protocol.existence = 1..1](#observation-dataevents-cardinality--0-stateexistence--01-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 1..*, state.existence = 0..1, protocol.existence = 1..1](#observation-dataevents-cardinality--1-stateexistence--01-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 3..*, state.existence = 0..1, protocol.existence = 1..1](#observation-dataevents-cardinality--3-stateexistence--01-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 0..1, state.existence = 0..1, protocol.existence = 1..1](#observation-dataevents-cardinality--01-stateexistence--01-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 1..1, state.existence = 0..1, protocol.existence = 1..1](#observation-dataevents-cardinality--11-stateexistence--01-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 3..5, state.existence = 0..1, protocol.existence = 1..1](#observation-dataevents-cardinality--35-stateexistence--01-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 0..*, state.existence = 1..1, protocol.existence = 1..1](#observation-dataevents-cardinality--0-stateexistence--11-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 1..*, state.existence = 1..1, protocol.existence = 1..1](#observation-dataevents-cardinality--1-stateexistence--11-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 3..*, state.existence = 1..1, protocol.existence = 1..1](#observation-dataevents-cardinality--3-stateexistence--11-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 0..1, state.existence = 1..1, protocol.existence = 1..1](#observation-dataevents-cardinality--01-stateexistence--11-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 1..1, state.existence = 1..1, protocol.existence = 1..1](#observation-dataevents-cardinality--11-stateexistence--11-protocolexistence--11)
    - [OBSERVATION data.events cardinality = 3..5, state.existence = 1..1, protocol.existence = 1..1](#observation-dataevents-cardinality--35-stateexistence--11-protocolexistence--11)

# 1. Introduction

The test cases defined here are for creating archetypes expressing specific constraints over the openEHR RM. Then different data instances should be generated in order to test the constraints. It’s recommended to have at least one success case, one failure case and all border cases covered. That is, for each archetype constraint specified, at least three data instances should be created.

Since there are many combinations of constraints possible in the AOM, we will try to separate them into different classes and focus on each constraint set class independently from the other sets. The sets are defined by:

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


> Definition: any class that extends LOCATABLE is a class with archetypable fields, we also call that an "archetypable" class.


**Implementation notes**

The constraint combinations described in the cases below could be implemented in different archetypes, or in a generic archetype then defining the specific constraints at the template level. Which option to use might depend on the modeling tools used to create archetypes and templates.

We would suggest to automate the archetype/template test cases generation instead of creating each constraint combination manualy.

When there is no constraint defined for an attribute, it means anything is allowed on that attribute. It is recommended to include data not defined by the archetype, but valid in the RM, when generating the data instances.


# 2. Top-level LOCATABLE class: COMPOSITION

These cases are defined to verify the constraints defined over archetypable attributes of the top-level class COMPOSITION.

The constraints combinations describe below could be tested in two ways:

1. Isolation: by not constraining the COMPOSITION.content at all, or adding an open/'any allowed' constraint {*} at the COMPOSITION.content in the archetype/template. This mean anything, even nothing, is accepted at the COMPOSITION.content at runtime.
2. Combination: with constraints set for COMPOSITION.content, for any CONTENT_ITEM (SECTION or ENTRY). Below there is a specification of the constraint combinations for each class accepted at COMPOSITION.content

> Note: we suggest to test with both strategies.

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


The constraints combinations describe below could be tested in two ways:

1. Isolation: by not constraining OBSERVATION.data, OBSERVATION.state and OBSERVATION.protocol, or using the open/'any allowed' constraint {*} for those attributes.
2. Combination: with constraints defined at the HISTORY level (for data and state) and ITEM_STRUCTURE (for protocol). 

> Note: we suggest to test with both strategies.


## 3.1. OBSERVATION state existence = 0..1, protocol existence = 0..1

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


The constraints combinations describe below could be tested in two ways:

1. Isolation: by not constraining HISTORY.events and HISTORY.summary, or using the open/'any allowed' constraint {*} for those attributes.
2. Combination: with constraints defined at the EVENT level (for events) and ITEM_STRUCTURE (for summary). 

> Note: we suggest to test with both strategies.


## HISTORY events cardinality 0..*, summary existence 0..1

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


## HISTORY events cardinality 1..*, summary existence 0..1

<div id="history_2">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower |
| one event        | absent          | accepted |  |
| three events     | absent          | accepted |  |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## HISTORY events cardinality 3..*, summary existence 0..1

<div id="history_3">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower |
| one event        | absent          | rejected | HSITORY.events cardinality.lower |
| three events     | absent          | accepted |  |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | rejected | HSITORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


## HISTORY events cardinality 0..1, summary existence 0..1

<div id="history_4">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | accepted |  |
| one event        | absent          | accepted |  |
| three events     | absent          | rejected | HSITORY.events cardinality.upper |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HSITORY.events cardinality.upper |

</div>


## HISTORY events cardinality 1..1, summary existence 0..1

<div id="history_5">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower |
| one event        | absent          | accepted |  |
| three events     | absent          | rejected | HSITORY.events cardinality.upper |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HSITORY.events cardinality.upper |

</div>


## HISTORY events cardinality 3..5, summary existence 0..1

<div id="history_6">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower |
| one event        | absent          | rejected | HSITORY.events cardinality.lower |
| three events     | absent          | accepted |  |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | rejected | HSITORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


## HISTORY events cardinality 0..*, summary existence 1..1

<div id="history_1">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## HISTORY events cardinality 1..*, summary existence 1..1

<div id="history_2">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | accepted |  |

</div>


## HISTORY events cardinality 3..*, summary existence 1..1

<div id="history_3">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HSITORY.events cardinality.lower, HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | rejected | HSITORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


## HISTORY events cardinality 0..1, summary existence 1..1

<div id="history_4">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HSITORY.events cardinality.upper, HISTORY.summary existence.lower |
| no event         | present         | accepted |  |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HSITORY.events cardinality.upper |

</div>


## HISTORY events cardinality 1..1, summary existence 1..1

<div id="history_5">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HISTORY.summary existence.lower |
| three events     | absent          | rejected | HSITORY.events cardinality.upper, HISTORY.summary existence.lower |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | accepted |  |
| three events     | present         | rejected | HSITORY.events cardinality.upper |

</div>


## HISTORY events cardinality 3..5, summary existence 1..1

<div id="history_6">

| events           | summary         | expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no events        | absent          | rejected | HSITORY.events cardinality.lower, HISTORY.summary existence.lower |
| one event        | absent          | rejected | HSITORY.events cardinality.lower, HISTORY.summary existence.lower |
| three events     | absent          | rejected | HISTORY.summary existence.lower |
| no event         | present         | rejected | HSITORY.events cardinality.lower |
| one event        | present         | rejected | HSITORY.events cardinality.lower |
| three events     | present         | accepted |  |

</div>


# EVENT data sets (WIP)

EVENT data sets:

5. EVENT with no state
6. EVENT with state


EVENT type combinations for the case of HISTORY with events:

1. EVENT is POINT_EVENT
2. EVENT is INTERVAL_EVENT

> Note: the combination of the HISTORY data sets with the EVENT types shouldn't affect the result of the constraints for cardinality and existence, but will affect if the wrong type is provided. So instead of combining the expected results, we could define separate tables for each set of constraints.

EVENT structure combinations

1. EVENT.data is ITEM_TREE
2. EVENT.data is ITEM_LIST
3. EVENT.data is ITEM_TABLE
4. EVENT.data is ITEM_SINGLE

> Note: test with TREE first because that is the most used structure type and the rest are just parcular cases of TREE.

> Note: to simplify modeling, the EVENT.data constraint could be open {*} so any instance will pass constraints at that level and only the constraints at the OBSERVATION level will be tested. 

### EVENT data existence 0..1, state existence 0..1

| event.data | event.state |expected | constraints violated |
|:-----------------|:----------------|:--------:|:---------------------|
| no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | no state    | accepted |  |
| present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present     | accepted |  |
| no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | no state    | accepted |  |
| no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | present     | accepted |  |
| no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | no state    | accepted |  |
| no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | present     | accepted |  |
| no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | no state    | accepted |  |
| no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present    | present     | accepted |  |
| no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |

</div>


----
**FOR REFERENCE ONLY, don't read below this for implementation purposes**

- no state
- data.events cardinality is 0..*
- data.events allow ant EVENT
- EVENT has no state
- EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}


### OBSERVATION data.events cardinality = 1..*, state.existence = 0..1, protocol.existence = 0..1

Content is just OBSERVATION
- no state
- data.events cardinality is 1..*
- data.events allow ant EVENT
- EVENT has no state
- EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

### OBSERVATION events cardinality = 3..*, state.existence = 0..1, protocol.existence = 0..1

Content is just OBSERVATION
- no state
- data.events cardinality is 3..*
- data.events allow ant EVENT
- EVENT has no state
- EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

### OBSERVATION events cardinality = 0..1, state.existence = 0..1, protocol.existence = 0..1

Content is just OBSERVATION
- no state
- data.events cardinality is 0..1
- data.events allow ant EVENT
- EVENT has no state
- EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}


### OBSERVATION events cardinality = 1..1, state.existence = 0..1, protocol.existence = 0..1

Content is just OBSERVATION
- no state
- data.events cardinality is 1..1
- data.events allow ant EVENT
- EVENT has no state
- EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

### OBSERVATION events cardinality = 3..5, state.existence = 0..1, protocol.existence = 0..1

Content is just OBSERVATION
- no state
- data.events cardinality is 3..5
- data.events allow ant EVENT
- EVENT has no state
- EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}


### OBSERVATION data.events cardinality = 0..*, state.existence = 1..1, protocol.existence = 0..1
### OBSERVATION data.events cardinality = 1..*, state.existence = 1..1, protocol.existence = 0..1
### OBSERVATION data.events cardinality = 3..*, state.existence = 1..1, protocol.existence = 0..1
### OBSERVATION data.events cardinality = 0..1, state.existence = 1..1, protocol.existence = 0..1
### OBSERVATION data.events cardinality = 1..1, state.existence = 1..1, protocol.existence = 0..1
### OBSERVATION data.events cardinality = 3..5, state.existence = 1..1, protocol.existence = 0..1


### OBSERVATION data.events cardinality = 0..*, state.existence = 0..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 1..*, state.existence = 0..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 3..*, state.existence = 0..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 0..1, state.existence = 0..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 1..1, state.existence = 0..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 3..5, state.existence = 0..1, protocol.existence = 1..1


### OBSERVATION data.events cardinality = 0..*, state.existence = 1..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 1..*, state.existence = 1..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 3..*, state.existence = 1..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 0..1, state.existence = 1..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 1..1, state.existence = 1..1, protocol.existence = 1..1
### OBSERVATION data.events cardinality = 3..5, state.existence = 1..1, protocol.existence = 1..1

