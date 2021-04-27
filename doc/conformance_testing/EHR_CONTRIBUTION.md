# openEHR Conformance Testing Specification (EHR/CONTRIBUTION)

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 1.0 (2021-04-26) | Split documentation of CONTRIBUTION from the EHR/COMPOSITION suite.     |


# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [EHR/CONTRIBUTION Validation Suite](#ehrcontribution-validation-suite)
  - [A. General Requirements](#a-general-requirements)
  - [B. CONTRIBUTION Test Cases](#b-contribution-test-cases)
    - [CONTRIBUTION data Sets:](#contribution-data-sets)
    - [VERSION<COMPOSITION> Data Sets (CONTRIBUTION contents):](#versioncomposition-data-sets-contribution-contents)
    - [B.1. Commit CONTRIBUTION](#b1-commit-contribution)
      - [B.1.1. Commit CONTRIBUTION Test Data Sets for COMPOSITIONs](#b11-commit-contribution-test-data-sets-for-compositions)
        - [B.1.1.1. Combinations for data sets](#c111-combinations-for-data-sets)
<!--te-->


# EHR/CONTRIBUTION Validation Suite

Items under this validation suite conceptually use these abstract interfaces:

- I_EHR_CONTRIBUTION

Reference: https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_openehr_platform_model


This test suite depends on other test suites:

- KNOWLEDGE (we need OPTs)
- EHR (we need EHRs)


## A. General Requirements

1. The server under test should support at least OPTs, 1.4 or 2, but OPT 1.4 if more frequent since modeling tools supporting this were around for a long time. Could also support ADL, 1.4 or 2.

2. The server should support at least one of the XML or JSON representations of CONTRIBUTIONs for committing data, and integrate the corresponding schemas (XML or JSON) to validate data syntactically (before validating against an OPT).


## B. CONTRIBUTION Test Cases

### CONTRIBUTION data Sets:

1. CONTRIBUTIONS with single valid VERSION<COMPOSITION> (minimal, one for each entry type)
2. CONTRIBUTIONS with multiple valid VERSION<COMPOSITION> (reuse the minimal ^)
3. CONTRIBUTION with single valid VERSION<COMPOSITION> with maximal data sets
4. Empty CONTRIBUTION (no VERSIONS)
5. CONTRIBUTIONS with invalid VERSION<COMPOSITION>
   1. Invalid data
   2. Wrong change type
   3. Wrong lifecycle
6. CONTRIBUTIONS with multiple VERSION<COMPOSITION>, with mixed valid and invalid ones


### VERSION<COMPOSITION> Data Sets (CONTRIBUTION contents):

1. Valid
   1. minimal COMPOSITIONs with one type of ENTRY (one ENTRY each, all ENTRIES covered)
   2. maximal COMPOSITION (all data types, all ENTRY types, and SECTIONs)
   3. a persistent COMPOSITION (e.g. problem list, medication list, immunization list, ...)
   4. time series COMPOSITION (observation with many events, e.g. CPR compressions intervals)
   5. COMPOSITION with alternative types (e.g. lab result DV_COUNT, DV_QUANTITY and DV_CODED_TEXT)
   6. COMPOSITION with DV_CODED_TEXT instance on nodes declared as DV_TEXT in the OPT
   7. COMPOSITION with empty ELEMENT.value and not empty ELEMENT.null_flavour
2. Invalid
   1.  Invalid COMPOSITIONs (e.g. mandatory items not present, wrong types, extra items not declared in OPT, invalid values)
   2.  Referenced OPT not loaded (this has to do more with the state of the system than to invalid data)
3.  Change type combinations (these are the minimal required, [all supported change types can be found here](https://github.com/openEHR/terminology/blob/master/openEHR_RM/en/openehr_terminology.xml#L26-L34))
    1.  VERSION.commit_audit.change_type = creation
    2.  VERSION.commit_audit.change_type = modification
    3.  VERSION.commit_audit.change_type = delete

> Note: there could be many combinations of flows to use the different Change Types mentioned above. The minimal required by this specification it that the server is capable of this flow:
> 1. creation
> 2. modification (one or many times)
> 3. deleted


### B.1. Commit CONTRIBUTION

#### B.1.1. Commit CONTRIBUTION Test Data Sets for COMPOSITIONs

**change_type**

Eech VERSION in a CONTRIBUTION has an AUDIT_DETAILS which contains a change_type attribute. The value on that attribute determines the internal behavior for processing each VERSION, and each VERSION in the same CONTRIBUTION could have a different change_type. The most used change types are:

1. **creation**: the VERSION represents the first version of a COMPOSITION.
2. **amendment**: the VERSION represents a new version of an existing COMPOSITION, with the purpose of adding data.
3. **modification**: the VERSION represents a new version of an existing COMPOSITION, with the purpose of changing data, maybe to fix an error.
4. **deleted**:the VERSION represents a new version of an existing COMPOSITION, with the purpose of deleting it.

Internally, amendment and modification might be processed in the exact same way, because the difference is semantic not functional.

Here is a full list: https://github.com/openEHR/terminology/blob/master/openEHR_RM/en/openehr_terminology.xml#L26


**lifecycle_state**

Each VERSION in a CONTRIBUTION contains an lifecycle_state attribute, which value gives semantics about the contents of the VERSION. The values could be:

1. **incomplete**: the COMPOSITION was committed incomplete and should be completed (reviewed, validated, amended) later.
2. **complete**: the COMPOSITION was complete at the moment it was committed.
3. **deleted**: the COMPOSITION was committed for deletion.

These codes are defined here: https://github.com/openEHR/terminology/blob/master/openEHR_RM/en/openehr_terminology.xml#L170

<div align="center">

![lifecycle state machine](img/lifecycle_state_machine.jpg "lifecycle state machine")
</div>


##### B.1.1.1. Combinations for data sets

These combinations can be tested by doing a single commit. The same combinations with flows of multiple commits could lead to different results.

**One commit (no previous commits were done), single version cases:**

> Note: All change types but creation should fail on the first commit, since other change types need a previous commit. Last one could fail because the first commit can't be change_type deleted or because the lifecycle_state complete can't be with change_type deleted.

<style>

#one_commit tr:nth-child(1) > td:last-child,
#one_commit tr:nth-child(5) > td:last-child,
.accepted {
  background-color: #ccffcc;
}

#one_commit tr:nth-child(2) > td:last-child,
#one_commit tr:nth-child(3) > td:last-child,
#one_commit tr:nth-child(4) > td:last-child,
#one_commit tr:nth-child(6) > td:last-child,
#one_commit tr:nth-child(7) > td:last-child,
#one_commit tr:nth-child(8) > td:last-child,
#one_commit tr:nth-child(9) > td:last-child,
#one_commit tr:nth-child(10) > td:last-child,
#one_commit tr:nth-child(11) > td:last-child,
#one_commit tr:nth-child(12) > td:last-child,
.rejected {
  background-color: #ffcccc;
}
</style>

<div id ="one_commit">

| change_type  | lifecycle_state* | composition category | composition validity** | expected |
|:------------:|:----------------:|:--------------------:|:----------------------:|:--------:|
| creation     | complete         | event                | valid                  | accepted |
| amendment    | complete         | event                | valid                  | rejected |
| modification | complete         | event                | valid                  | rejected |
| deleted      | complete         | event                | valid                  | rejected |
| creation     | complete         | persistent           | valid                  | accepted |
| amendment    | complete         | persistent           | valid                  | rejected |
| modification | complete         | persistent           | valid                  | rejected |
| deleted      | complete         | persistent           | valid                  | rejected |
| creation     | deleted          | event                | valid                  | rejected |
| amendment    | deleted          | event                | valid                  | rejected |
| modification | deleted          | event                | valid                  | rejected |
| deleted      | deleted          | event                | valid                  | rejected |

</div>

> \* Note: the incomplete cases should be equal to the complete, because the flag is just adding semantics about the content, not setting how the content should be processed.

> \** Note: the invalid cases will make the accepted cases on the previous table to be rejected because the content in the COMPOSITION is not valid.


**One commit (no previous commits were done), multiple versions cases:**

> Note: the tables below represent one VERSIONs in the committed CONTRIBUTION.

A. Creating two valid, complete and event COMPOSITIONS in one commit should be accepted.

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | event                | valid                |
| creation      | complete          | event                | valid                |

This CONTRIBUTION should be <span class="accepted">ACCEPTED</span>.


B. Creating two valid, complete and persistent COMPOSITIONS in one commit should be accepted.

> Note: depending on the server implementation, some servers might not accept the second COMPOSITION if both COMPOSITIONs reference the same persistent OPT. So this test case considers both COMPOSITIONs reference different persistent OPTs.

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | persistent           | valid                |
| creation      | complete          | persistent           | valid                |

This CONTRIBUTION should be <span class="accepted">ACCEPTED</span>.


C. Creating two valid, complete and mixed category COMPOSITIONS in one commit should be accepted.

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | event                | valid                |
| creation      | complete          | persistent           | valid                |

This CONTRIBUTION should be <span class="accepted">ACCEPTED</span>.


D. If any COMPOSITION is invalid, the whole commit should fail. It doesn't matter if it is complete or incomplete, event or persistent (just showing some of the combinations below).

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | event                | valid                |
| creation      | complete          | event                | invalid              |

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | persistent           | valid                |
| creation      | complete          | persistent           | valid                |

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | event                | valid                |
| creation      | complete          | persistent           | invalid              |

| change_type+  | lifecycle_state++ | composition category | composition validity |
|:-------------:|:-----------------:|:--------------------:|:--------------------:|
| creation      | complete          | event                | invalid              |
| creation      | complete          | persistent           | valid                |

These CONTRIBUTIONs should be <span class="rejected">REJECTED</span>.

> \+ Note: for other change types than creation, the first commit will be rejected, so not included in the table those cases but should be tested.

> \++ Note: the incomplete cases should be equal to the complete, because the flag is just adding semantics about the content, not setting how the content should be processed.


WIP....