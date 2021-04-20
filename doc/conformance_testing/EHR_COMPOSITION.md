# openEHR Conformance Testing Specification (EHR/COMPOSITION)

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2019-03-12) | First version covering COMPOSITION and CONTRIBUTION related services.     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2019-04-25) | Added cases for create, update and delete COMPOSITION.     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.3 (2019-05-14) | Fixed typos, improve wording, fully indexed each section inside B.x     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> / Wladislaw Wagner <wladislaw.wagner@vitagroup.ag> | 0.4 (2020-11-18) | Added test cases for CONTRIBUTION with VERSION<EHR_STATUS>     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 1.0 (2021-04-25) | Integration into single document, minor grammar fixes.     |


# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [EHR/COMPOSITION Validation Suite](#ehrcomposition-validation-suite)
  - [A. General Requirements](#a-general-requirements)
  - [B. COMPOSITION Test cases](#b-composition-test-cases)
    - [B.1. Has COMPOSITION](#b1-has-composition)
      - [B.1.a. Main flow: has existing COMPOSITION](#b1a-main-flow-has-existing-composition)
      - [B.1.b. Alternative flow 1: has COMPOSITION, COMPOSITION doesn't exist](#b1b-alternative-flow-1-has-composition-composition-doesnt-exist)
      - [B.1.c. Alternative flow 2: has COMPOSITION on non-existent EHR](#b1c-alternative-flow-2-has-composition-on-non-existent-ehr)
    - [B.2. Get COMPOSITION latest](#b2-get-composition-latest)
      - [B.2.a. Main flow: get existing COMPOSITION latest](#b2a-main-flow-get-existing-composition-latest)
      - [B.2.b. Alternative flow 1: get COMPOSITION latest, COMPOSITION doesn't exist](#b2b-alternative-flow-1-get-composition-latest-composition-doesnt-exist)
      - [B.2.c. Alternative flow 2: get COMPOSITION latest on non-existent EHR](#b2c-alternative-flow-2-get-composition-latest-on-non-existent-ehr)
    - [B.3. Get COMPOSITION at time](#b3-get-composition-at-time)
      - [B.3.a. Main flow: get existing COMPOSITION at time](#b3a-main-flow-get-existing-composition-at-time)
      - [B.3.b. Alternative flow 1: get existing COMPOSITION at time, without a given time](#b3b-alternative-flow-1-get-existing-composition-at-time-without-a-given-time)
      - [B.3.c. Alternative flow 2: get COMPOSITION at time, COMPOSITION doesn't exist](#b3c-alternative-flow-2-get-composition-at-time-composition-doesnt-exist)
      - [B.3.d. Alternative flow 3: get COMPOSITION at time on non-existent EHR](#b3d-alternative-flow-3-get-composition-at-time-on-non-existent-ehr)
      - [B.3.e. Alternative flow 4: get existing COMPOSITION at time, cover different times](#b3e-alternative-flow-4-get-existing-composition-at-time-cover-different-times)
    - [B.4. Get COMPOSITION at version](#b4-get-composition-at-version)
      - [B.4.a. Main flow: get existing COMPOSITION at version](#b4a-main-flow-get-existing-composition-at-version)
      - [B.4.b. Alternative flow 1: get COMPOSITION at version, VERSION<COMPOSITION> doesn't exist](#b4b-alternative-flow-1-get-composition-at-version-versioncomposition-doesnt-exist)
      - [B.4.c. Alternative flow 2: get COMPOSITION at version, EHR doesn't exist](#b4c-alternative-flow-2-get-composition-at-version-ehr-doesnt-exist)
      - [B.4.d. Alternative flow 3: get COMPOSITION at version, cover different versions](#b4d-alternative-flow-3-get-composition-at-version-cover-different-versions)
<!--te-->


# EHR/COMPOSITION Validation Suite

Items under this validation suite conceptually use these abstract interfaces:

- I_EHR_COMPOSITION
- I_EHR_CONTRIBUTION

Reference: https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_openehr_platform_model


This test suite depends on other test suites:

- KNOWLEDGE (we need OPTs)
- EHR (we need EHRs)



## A. General Requirements

1. The server under test should support at least OPTs, 1.4 or 2, but OPT 1.4 if more frequent since modeling tools supporting this were around for a long time. Could also support ADL, 1.4 or 2.

2. The server should support at least one of the XML or JSON representations of COMPOSITIONs for committing data, and integrate the corresponding schemas (XML or JSON) to validate data syntactically (before validating against an OPT).



## B. COMPOSITION Test cases

The services under test in this section are defined by the openEHR Service Model [I_EHR_COMPOSITION interface](https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface).


### B.1. Has COMPOSITION

#### B.1.a. Main flow: has existing COMPOSITION

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has a CONTRIBUTION
3. The CONTRIBUTION contains a VERSION with known uid.

**Postconditions:**

None

**Flow:**

1. Invoke the has COMPOSITION service for the ehr_id and VERSION uid
2. The result must be TRUE


#### B.1.b. Alternative flow 1: has COMPOSITION, COMPOSITION doesn't exist

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has no CONTRIBUTIONS

**Postconditions:**

None

**Flow:**

1. Invoke the has COMPOSITION service for ehr_uid, and a random VERSION uid
2. The result must be FALSE


#### B.1.c. Alternative flow 2: has COMPOSITION on non-existent EHR

**Preconditions:**

1. There are no EHRs on the server

**Postconditions:**

None

**Flow:**

1. Invoke the has COMPOSITION service with a random ehr_id
2. The service should return an error related to the non existence of the EHR



### B.2. Get COMPOSITION latest

**Implementation consideration:**

When a COMPOSITION is retrieved from a service, it will comply with a specific format. There could be a variant for each test to retrieve the COMPOSITION in any of the supported openEHR formats, and the syntactic validation of those retrieved formats should be done by using the corresponding schemas (XML, JSON, etc). That would be the minimal validation for conformance testing. Though it would be ideal to have semantic validation of the retrieved COMPOSITIONs to ensure conformance, which is achieved by validating against the corresponding OPT in the testing layer.



#### B.2.a. Main flow: get existing COMPOSITION latest

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has a VERSIONED_COMPOSITION with known uid
3. The VERSIONED_COMPOSITION has two VERSION<COMPOSITION>

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION latest service for the ehr_id and VERSIONED_COMPOSITION uid
2. The result must return the COMPOSITION contents, and should be the latest version
3. The retrieved format should contain all the exact same data as the format used when committing the COMPOSITION (content check)



#### B.2.b. Alternative flow 1: get COMPOSITION latest, COMPOSITION doesn't exist

**Preconditions:**

1. An EHR with known ehr_id exists and has no CONTRIBUTIONS

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION latest service for ehr_uid, and a random VERSIONED_COMPOSITION uid
2. The result must be empty, with an error "the COMPOSITION uid doesn't exist in the EHR ehd_uid"



#### B.2.c. Alternative flow 2: get COMPOSITION latest on non-existent EHR

**Preconditions:**

1. There are no EHRs on the server

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION latest service with a random ehr_id, and a random VERSIONED_COMPOSITION uid
2. The service should return an error related to the non existence of the EHR



### B.3. Get COMPOSITION at time

#### B.3.a. Main flow: get existing COMPOSITION at time

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has one or more VERSIONED_COMPOSITION with known uid

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at time service for the ehr_id, VERSIONED_COMPOSITION uid and current time
2. The result must return the COMPOSITION contents of the existing COMPOSITION at given time
3. The retrieved format should contain all the exact same data as the format used when committing the COMPOSITION (content check)

**Notes:**

1. When requesting a COMPOSITION at time using the current time, the last version of the matching composition, if it exists, should be retrieved.



#### B.3.b. Alternative flow 1: get existing COMPOSITION at time, without a given time

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has one or more VERSIONED_COMPOSITION with known uid

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at time service for the ehr_id, VERSIONED_COMPOSITION uid and no time
2. The result must return the COMPOSITION contents of the existing COMPOSITION, and should be the latest VERSION of the COMPOSITION
3. The retrieved format should contain all the exact same data as the format used when committing the COMPOSITION (content check)

**Notes:**

1. Test this using COMPOSITIONS with one version and multiple versions, to be sure the retrieved one is the latest
2. The previous tests for "get COMPOSITION latest" could be used to compare results.



#### B.3.c. Alternative flow 2: get COMPOSITION at time, COMPOSITION doesn't exist

**Preconditions:**

1. An EHR with known ehr_id exists and has no CONTRIBUTIONS

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at time service for ehr_uid, and a random VERSIONED_COMPOSITION uid and current time
2. The result must be empty, with an error related to "the COMPOSITION uid doesn't exist in the EHR ehd_uid"



#### B.3.d. Alternative flow 3: get COMPOSITION at time on non-existent EHR

**Preconditions:**

1. There are no EHRs on the server

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at time service with a random ehr_id, random VERSIONED_OBJECT uid and current time
2. The service should return an error related to the non existence of the EHR



#### B.3.e. Alternative flow 4: get existing COMPOSITION at time, cover different times

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR should have one VERSIONED_COMPOSITION with a know uid
3. The VERSIONED_COMPOSITION should have two VERSIONs (the EHR has two CONTRIBUTIONs for the same COMPOSITION)
4. CONTRIBUTIONs were done at times t0 and t1 with t0 < t1

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at time service for the ehr_id, VERSIONED_COMPOSITION uid and a time < t0
2. The result must be negative and return an error related to the COMPOSITION not existing at that time
3. Invoke the get COMPOSITION at time service for the ehr_id, VERSIONED_COMPOSITION uid and a time > t0 and < t1
4. The result must return the COMPOSITION contents of the COMPOSITION committed in t0
5. The retrieved format should contain all the exact same data as the format used when committing the COMPOSITION (content check)
6. Invoke the get COMPOSITION at time service for the ehr_id, VERSIONED_COMPOSITION uid and a time > t1
7. The result must return the COMPOSITION contents of the COMPOSITION committed in t1
8. The retrieved format should contain all the exact same data as the format used when committing the COMPOSITION (content check)



### B.4. Get COMPOSITION at version

#### B.4.a. Main flow: get existing COMPOSITION at version

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has one VERSION<COMPOSITION> with known version id

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at version service for the ehr_id, VERSION<COMPOSITION> version id
2. The result must return the COMPOSITION contents of the existing VERSION<COMPOSITION>
3. The retrieved format should contain all the exact same data as the format used when committing the COMPOSITION (content check)


#### B.4.b. Alternative flow 1: get COMPOSITION at version, VERSION<COMPOSITION> doesn't exist

**Preconditions:**

1. An EHR with known ehr_id exists and doesn't have any commits

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at version service for the ehr_id, and a random version id
2. The result must be negative and return an error related to the non-existent COMPOSITION with the version id


#### B.4.c. Alternative flow 2: get COMPOSITION at version, EHR doesn't exist

**Preconditions:**

1. The system doesn't have any EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at version service a random ehr_id and random version id
2. The result must be negative and return an error related to the non-existent EHR.


#### B.4.d. Alternative flow 3: get COMPOSITION at version, cover different versions

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR should have one VERSIONED_COMPOSITION with a know uid
3. The VERSIONED_COMPOSITION should have two VERSIONs (the EHR has two CONTRIBUTIONs for the same COMPOSITION)
4. Both VERSIONs have ids: v1 and v2

**Postconditions:**

None

**Flow:**

1. Invoke the get COMPOSITION at version service, for the ehr_id and VERSION<COMPOSITION> version id v1
2. The result must be positive and retrieve the COMPOSITION, that should match the COMPOSITION created with version id v1. (content check).
3. Invoke the get COMPOSITION at version service, for the ehr_id and VERSION<COMPOSITION> version id v2
4. The result must be positive and retrieve the COMPOSITION, that should match the COMPOSITION created with version id v2 (content check).

