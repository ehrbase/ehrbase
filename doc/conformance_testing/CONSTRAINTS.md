# openEHR Data Validation Conformance

The test cases defined here are for creating archetypes expressing specific constraints over the openEHR RM. Then different data instances should be generated in order to test the constraints. It’s recommended to have at least one success case, one failure case and all border cases covered. That is, for each archetype constraint specified, at least three data instances should be created.

Since there are many combinations of constraints possible in the AOM, we will try to separate them into different classes and focus on each constraint set class independently from the other sets. The sets are defined by:

1. A top-level LOCATABLE class: COMPOSITION, EHR_STATUS, FOLDER, PARTY
2. Constraint sets on top-level attributes for each class
3. Constraint sets on internal attributes (at any level in the RM hierarchy in the top-level class)


For easy reference, we could use the first letter of each top-level class to define the ID of each data set, and numbers for the other two levels of the classification.

When checking a “multiple attribute” cardinality, this is a good set of combinations to test is:

- 0..*
- 1..*
- 3..*
- 0..1
- 1..1
- 3..5


> Note: when there is no constraint defined for an attribute, it means anything is allowed on that attribute. It is recommended to include data not defined by the archetype, but valid in the RM, when generating the data instances.


## Top-level class: COMPOSITION

### Content cardinality 0..*, no constraint over context

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


### Content cardinality 1..*, no constraint over context

COMPOSITION data sets:

1. COMPOSITION with no entries (border case, fail)
2. COMPOSITION with one entry (success)
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

### Content cardinality 3..*, no constraint over context

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

### Content cardinality 0..1, no constraint over context

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
| three entries    | context with other_context    | reejcted | COMPOSITION.content: cardinality.upper |

</div>

### Content cardinality 1..1, no constraint over context



### Content cardinality 3..5, no constraint over context



### Content cardinality 0..*, context occurrence 1..1


### Content cardinality 1..*, context occurrence 1..1


### Content cardinality 3..*, context occurrence 1..1


### Content cardinality 0..1, context occurrence 1..1


### Content cardinality 1..1, context occurrence 1..1


### Content cardinality 3..5, context occurrence 1..1


### COMPOSITION.content combinations

These data sets should be combined with the COMPOSITION archetype constraints mentioned above to generate the final OPTs.

#### Case C.1.1.

Content is just OBSERVATION
- no state
- data.events cardinality is 0..*
- data.events are POINT_EVENT
- POINT_EVENT has no state
- POINT_EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

#### Case C.1.2.

Content is just OBSERVATION
- no state
- data.events cardinality is 1..*
- data.events are POINT_EVENT
- POINT_EVENT has no state
- POINT_EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

#### Case C.1.3.

Content is just OBSERVATION
- no state
- data.events cardinality is 1..1
- data.events are POINT_EVENT
- POINT_EVENT has no state
- POINT_EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

#### Case C.1.4.

Content is just OBSERVATION
- no state
- data.events cardinality is 3..*
- data.events are POINT_EVENT
- POINT_EVENT has no state
- POINT_EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

#### Case C.1.5.

Content is just OBSERVATION
- no state
- data.events cardinality is 0..1
- data.events are POINT_EVENT
- POINT_EVENT has no state
- POINT_EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}

#### Case C.1.6.

Content is just OBSERVATION
- no state
- data.events cardinality is 3..5
- data.events are POINT_EVENT
- POINT_EVENT has no state
- POINT_EVENT.data is ITEM_TREE
- ITEM_TREE has open constraint {*}
