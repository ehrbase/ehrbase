# openEHR Data Validation Conformance

## TOC

- [openEHR Data Validation Conformance](#openehr-data-validation-conformance)
  - [TOC](#toc)
  - [Introduction](#introduction)
  - [Top-level class: COMPOSITION](#top-level-class-composition)
    - [Content cardinality 0..*, no constraint over context](#content-cardinality-0-no-constraint-over-context)
    - [Content cardinality 1..*, no constraint over context](#content-cardinality-1-no-constraint-over-context)
    - [Content cardinality 3..*, no constraint over context](#content-cardinality-3-no-constraint-over-context)
    - [Content cardinality 0..1, no constraint over context](#content-cardinality-01-no-constraint-over-context)
    - [Content cardinality 1..1, no constraint over context](#content-cardinality-11-no-constraint-over-context)
    - [Content cardinality 3..5, no constraint over context](#content-cardinality-35-no-constraint-over-context)
    - [Content cardinality 0..*, context occurrences 1..1](#content-cardinality-0-context-occurrences-11)
    - [Content cardinality 1..*, context occurrences 1..1](#content-cardinality-1-context-occurrences-11)
    - [Content cardinality 3..*, context occurrences 1..1](#content-cardinality-3-context-occurrences-11)
    - [Content cardinality 0..1, context occurrences 1..1](#content-cardinality-01-context-occurrences-11)
    - [Content cardinality 1..1, context occurrences 1..1](#content-cardinality-11-context-occurrences-11)
    - [Content cardinality 3..5, context occurrences 1..1](#content-cardinality-35-context-occurrences-11)
    - [COMPOSITION.content combinations](#compositioncontent-combinations)
      - [Case C.1.1.](#case-c11)
      - [Case C.1.2.](#case-c12)
      - [Case C.1.3.](#case-c13)
      - [Case C.1.4.](#case-c14)
      - [Case C.1.5.](#case-c15)
      - [Case C.1.6.](#case-c16)

## Introduction

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


> Note: when there is no constraint defined for an attribute, it means anything is allowed on that attribute. It is recommended to include data not defined by the archetype, but valid in the RM, when generating the data instances.

> Note: any class that extends LOCATABLE is a class with archetypable fields.


## Top-level class: COMPOSITION

These cases are defined to verify the constraints defined over archetypable attributes of the top-level class COMPOSITION.

> Note: the constraint combinations described in the cases below could be done in different archetypes, or we could defined very generic archetypes and define the specific constraints at the template level. That possibility might depend on the modeling tools used to create archetypes and templates.

### COMPOSITION content cardinality 0..*, no constraint over context

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


### COMPOSITION content cardinality 1..*, no constraint over context

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

### COMPOSITION content cardinality 3..*, no constraint over context

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

### COMPOSITION content cardinality 0..1, no constraint over context

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

### COMPOSITION content cardinality 1..1, no constraint over context

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


### COMPOSITION content cardinality 3..5, no constraint over context

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

### COMPOSITION content cardinality 0..*, context occurrences 1..1

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


### COMPOSITION content cardinality 1..*, context occurrences 1..1

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


### COMPOSITION content cardinality 3..*, context occurrences 1..1

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

### COMPOSITION content cardinality 0..1, context occurrences 1..1

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

### COMPOSITION content cardinality 1..1, context occurrences 1..1

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

### COMPOSITION content cardinality 3..5, context occurrences 1..1

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


## Internal LOCATABLE classes

Each data set that included entries at the COMPOSITION.content in the previously defined data sets, requires some specification of the internal entry structure that will be used. Since each entry type could also have it's own archetypable fields where data validation should also be tested, the entry data sets described in this section should be combined with the COMPOSITION data sets.

Also for each entry type in this section we specify a combination of constraints that should be tested against the entry data sets.

OBSERVATION data sets:

1. OBSERVATION with no state and no protocol
2. OBSERVATION with state and no protocol
3. OBSERVATION with no state and protocol
4. OBSERVATION with state and protocol

Combine those cases with:

1. EVENT with no state
2. EVENT with state

EVENT type combinations:

1. EVENT is POINT_EVENT
2. EVENT is INTERVAL_EVENT

EVENT structure combinations

1. EVENT.data is ITEM_TREE
2. EVENT.data is ITEM_LIST
3. EVENT.data is ITEM_TABLE
4. EVENT.data is ITEM_SINGLE

> Note: test with TREE first because that is the most used structure type and the rest are just parcular cases of TREE.

> Note: to simplify modeling, the EVENT.data constraint could be open {*} so any instance will pass constraints at that level and only the constraints at the OBSERVATION level will be tested. 

### OBSERVATION data.events cardinality = 0..*, state.existence = 0..1, protocol.existence = 0..1

The table below shows combinations of different OBSERVATION options with it's HISTORY and EVENT options.

TODO: since the expected repeats based on EVENT constraints it might be better to define the OBSERVATION combinations and HISTORY/EVENT combinatiosn separatelly, then state those need to be combined to create the data sets, so the expected results are also combined.

<div id="obs_1">

| data          | state      | protocol    | event          | event.data | event.state | expected | constraints violated |
|:--------------|:-----------|:-----------:|:--------------:|------------|-------------|----------|----------------------|
| no data       | no state   | no protocol | -              | -          | -           | rejected | OBSERVATION.data occurrences.lower (RM/schema constraint) |
| present       | no state   | no protocol | POINT_EVENT    | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | no protocol | POINT_EVENT    | present    | no state    | accepted |  |
| present       | no state   | no protocol | POINT_EVENT    | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | no protocol | POINT_EVENT    | present    | present     | accepted |  |
| present       | present    | no protocol | POINT_EVENT    | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | no protocol | POINT_EVENT    | present    | no state    | accepted |  |
| present       | present    | no protocol | POINT_EVENT    | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | no protocol | POINT_EVENT    | present    | present     | accepted |  |
| present       | no state   | present     | POINT_EVENT    | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | present     | POINT_EVENT    | present    | no state    | accepted |  |
| present       | no state   | present     | POINT_EVENT    | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | present     | POINT_EVENT    | present    | present     | accepted |  |
| present       | present    | present     | POINT_EVENT    | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | present     | POINT_EVENT    | present    | no state    | accepted |  |
| present       | present    | present     | POINT_EVENT    | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | present     | POINT_EVENT    | present    | present     | accepted |  |
| present       | no state   | no protocol | INTERVAL_EVENT | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | no protocol | INTERVAL_EVENT | present    | no state    | accepted |  |
| present       | no state   | no protocol | INTERVAL_EVENT | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | no protocol | INTERVAL_EVENT | present    | present     | accepted |  |
| present       | present    | no protocol | INTERVAL_EVENT | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | no protocol | INTERVAL_EVENT | present    | no state    | accepted |  |
| present       | present    | no protocol | INTERVAL_EVENT | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | no protocol | INTERVAL_EVENT | present    | present     | accepted |  |
| present       | no state   | present     | INTERVAL_EVENT | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | present     | INTERVAL_EVENT | present    | no state    | accepted |  |
| present       | no state   | present     | INTERVAL_EVENT | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | no state   | present     | INTERVAL_EVENT | present    | present     | accepted |  |
| present       | present    | present     | INTERVAL_EVENT | no data    | no state    | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | present     | INTERVAL_EVENT | present    | no state    | accepted |  |
| present       | present    | present     | INTERVAL_EVENT | no data    | present     | rejected | EVENT.data occurrences.lower (RM/schema constraint) |
| present       | present    | present     | INTERVAL_EVENT | present    | present     | accepted |  |

</div>

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

