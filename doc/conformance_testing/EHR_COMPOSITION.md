# openEHR Conformance Testing Specification (EHR/COMPOSITION)

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2019-03-12) | First version covering COMPOSITION and CONTRIBUTION related services.     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2019-04-25) | Added cases for create, update and delete COMPOSITION.     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.3 (2019-05-14) | Fixed typos, improve wording, fully indexed each section inside B.x     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> / Wladislaw Wagner <wladislaw.wagner@vitagroup.ag> | 0.4 (2020-11-18) | Added test cases for CONTRIBUTION with VERSION<EHR_STATUS>     |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 1.0 (2021-04-25) | Integration into single document, minor grammar fixes, improved description of flows.     |


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
    - [B.5. Get VERSIONED COMPOSITION](#b5-get-versioned-composition)
      - [B.5.a. Main flow: get existing VERSIONED COMPOSITION](#b5a-main-flow-get-existing-versioned-composition)
      - [B.5.b. Alternative flow 1: get non-existent VERSIONED COMPOSITION](#b5b-alternative-flow-1-get-non-existent-versioned-composition)
      - [B.5.c. Alternative flow 2: get VERSIONED COMPOSITION, EHR doesn't exist](#b5c-alternative-flow-2-get-versioned-composition-ehr-doesnt-exist)
    - [B.6. Create COMPOSITION](#b6-create-composition)
      - [B.6.a. Main flow: Create new event COMPOSITION](#b6a-main-flow-create-new-event-composition)
      - [B.6.b. Alternative flow 1: Create new persistent COMPOSITION](#b6b-alternative-flow-1-create-new-persistent-composition)
      - [B.6.c. Alternative flow 2: Create persistent COMPOSITION for the same OPT twice](#b6c-alternative-flow-2-create-persistent-composition-for-the-same-opt-twice)
      - [B.6.d. Alternative flow 3: Create new invalid event COMPOSITION](#b6d-alternative-flow-3-create-new-invalid-event-composition)
      - [B.6.e. Alternative flow 4: Create new invalid persistent COMPOSITION](#b6e-alternative-flow-4-create-new-invalid-persistent-composition)
      - [B.6.f. Alternative flow 5: Create new event COMPOSITION, referenced OPT doesn't exist](#b6f-alternative-flow-5-create-new-event-composition-referenced-opt-doesnt-exist)
      - [B.6.g. Alternative flow 6: Create new event COMPOSITION, EHR doesn't exist](#b6g-alternative-flow-6-create-new-event-composition-ehr-doesnt-exist)
    - [B.7. Update COMPOSITION](#b7-update-composition)
      - [B.7.a. Main flow: update an existing event COMPOSITION](#b7a-main-flow-update-an-existing-event-composition)
      - [B.7.b. Alternative flow 1: update an existing persistent COMPOSITION](#b7b-alternative-flow-1-update-an-existing-persistent-composition)
      - [B.7.c. Alternative flow 2: update a non-existent COMPOSITION](#b7c-alternative-flow-2-update-a-non-existent-composition)
      - [B.7.d. Alternative flow 3: update an existing COMPOSITION, referencing a different template](#b7d-alternative-flow-3-update-an-existing-composition-referencing-a-different-template)
    - [B.8. Delete COMPOSITION](#b8-delete-composition)
      - [B.8.a. Main flow: delete event COMPOSITION](#b8a-main-flow-delete-event-composition)
      - [B.8.b. Alternative flow 1: delete persistent COMPOSITION](#b8b-alternative-flow-1-delete-persistent-composition)
      - [B.8.c. Alternative flow 2: delete non-existent COMPOSITION](#b8c-alternative-flow-2-delete-non-existent-composition)
<!--te-->


# EHR/COMPOSITION Validation Suite

Items under this validation suite conceptually use these abstract interfaces:

- I_EHR_COMPOSITION

Reference: https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_openehr_platform_model


This test suite depends on other test suites:

- KNOWLEDGE (we need OPTs)
- EHR (we need EHRs)



## A. General Requirements

1. The server under test should support at least OPTs, 1.4 or 2, but OPT 1.4 if more frequent since modeling tools supporting this were around for a long time. Could also support ADL, 1.4 or 2.

2. The server should support at least one of the XML or JSON representations of COMPOSITIONs for committing data, and integrate the corresponding schemas (XML or JSON) to validate data syntactically (before validating against an OPT).



## B. COMPOSITION Test Cases

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



### B.5. Get VERSIONED COMPOSITION

#### B.5.a. Main flow: get existing VERSIONED COMPOSITION

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has one VERSIONED_COMPOSITION with known uid

**Postconditions:**

None

**Flow:**

1. Invoke the get VERSIONED COMPOSITION service for the ehr_id and VERSIONED_COMPOSITION uid
2. The result must return a valid VERSIONED_COMPOSITION object, referencing the VERSION<COMPOSITION> it contains

**Notes:**

1. To consider different cases, try with VERSIONED_COMPOSITION that contain just one VERSION or many VERSIONs
2. For that, the valid test cases for Create COMPOSITION could be used to comply with the preconditions of this test flow


#### B.5.b. Alternative flow 1: get non-existent VERSIONED COMPOSITION

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR doesn't have any commits

**Postconditions:**

None

**Flow:**

1. Invoke the get VERSIONED COMPOSITION service for the ehr_id and a random VERSIONED_COMPOSITION uid
2. The result must be negative and return an error related to the non existence of the VERSIONED_COMPOSITION


#### B.5.c. Alternative flow 2: get VERSIONED COMPOSITION, EHR doesn't exist

**Preconditions:**

1. The system doesn't have any EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the get VERSIONED COMPOSITION service for a random ehr_id and a random VERSIONED_COMPOSITION uid
2. The result must be negative and return an error related to the non existence of the EHR




### B.6. Create COMPOSITION

#### B.6.a. Main flow: Create new event COMPOSITION

**Preconditions:**

1. The OPT, associated with the event COMPOSITION that will be created, should exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

A new event COMPOSITION exists in the EHR.

**Flow:**

1. Invoke the create COMPOSITION service with a valid event COMPOSITION, compliant with the existing OPT, and with the known ehr_id
2. The result should be positive, return information about the new COMPOSITION added to the EHR, and the version number should be 1


#### B.6.b. Alternative flow 1: Create new persistent COMPOSITION

**Preconditions:**

1. The OPT, associated with the persistent COMPOSITION that will be created, should exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

A new persistent COMPOSITION exists in the EHR.

**Flow:**

1. Invoke the create COMPOSITION service with a valid persistent COMPOSITION, compliant with the existing OPT, and the known ehr_id
2. The result should be positive, and return information about the new COMPOSITION added to the EHR, and the version number should be 1


#### B.6.c. Alternative flow 2: Create persistent COMPOSITION for the same OPT twice

**Preconditions:**

1. The OPT, associated with the persistent COMPOSITION that will be created, should exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

A new persistent COMPOSITION exists in the EHR.

**Flow:**

1. Invoke the create COMPOSITION service with a valid persistent COMPOSITION, compliant with the existing OPT, and with the known ehr_id
2. The result should be positive, and return information about the new COMPOSITION added to the EHR, and the version number should be 1
3. Invoke the create COMPOSITION service with a valid persistent COMPOSITION and the same ehr_id as in 1., the COMPOSITION should comply with the same persistent OPT as the COMPOSITION in 1
4. The result should be negative, returning an error related to trying to create a persistent COMPOSITION for the same persistent OPT that already has a first version

**Notes:**

1. Current criteria is: only one 'create' operation is allowed for persistent COMPOSITIONs, the next operations over an existing persistent COMPOSITION should be 'modifications'.
2. This is under debate in the openEHR SEC since some implementations permit 'persistent COMPOSIITONS' to have more than one instance in the same EHR and some others not. This is due to the lack of information in the openEHR specifications. There is also a discussion to define other types of categories for COMPOSITIONs to allow different behaviors. Ref: https://discourse.openehr.org/t/specrm-89-support-for-episodic-category/51/3


#### B.6.d. Alternative flow 3: Create new invalid event COMPOSITION

**Preconditions:**

1. The OPT, associated with the event COMPOSITION that will be created, should exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

None

**Flow:**

1. Invoke the create COMPOSITION service with an invalid event COMPOSITION and the known ehr_id
2. The result should be negative, and return information about the errors in the provided COMPOSITION


#### B.6.e. Alternative flow 4: Create new invalid persistent COMPOSITION

**Preconditions:**

1. The OPT, associated with the persistent COMPOSITION that will be created, should exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

None

**Flow:**

1. Invoke the create COMPOSITION service with an invalid persistent COMPOSITION and the known ehr_id
2. The result should be negative, and return information about the errors in the provided COMPOSITION


#### B.6.f. Alternative flow 5: Create new event COMPOSITION, referenced OPT doesn't exist

**Preconditions:**

1. The OPT, referenced by the COMPOSITION to commit, doesn't exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

None

**Flow:**

1. Invoke the create COMPOSITION service with a valid event COMPOSITION and the known ehr_id
   1. The COMPOSITION should reference an OPT that doesn't exist on the server
2. The result should be negative, and return information about the non-existent OPT


#### B.6.g. Alternative flow 6: Create new event COMPOSITION, EHR doesn't exist

**Preconditions:**

1. The OPT, referenced by the COMPOSITION to commit, exists on the server
2. The server doesn't have any EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the create COMPOSITION service with a valid event COMPOSITION and a random ehr_id
2. The result should be negative, and return information about the non-existent EHR




### B.7. Update COMPOSITION

The update COMPOSITION service needs a the VERSION.preceding_version_uid attribute to be set, so the server knows which existing VERSION of the COMPOSITION will be associated with the newly committed COMPOSITION. The Service Model spec is not clear about where that attribute is defined. By takin into account the Reference Model, the COMPOSITION doesn't contain that value but the VERSION does. For the COMPOSITION update service the preceding_version_uid should be a parameter or the definition in the SM should mention this.

#### B.7.a. Main flow: update an existing event COMPOSITION

**Preconditions:**

1. The OPT referenced by the COMPOSITIONs to commit exists on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

1. A new VERSIONED_OBJECT exists on the server
2. The VERSIONED_OBJECT has two VERSIONs of COMPOSITION
3. One VERSION.commit_audit.change_type is CREATE, the other one is MODIFY

**Flow:**

1. Invoke the create COMPOSITION service with a valid event COMPOSITION and the existing ehr_id
   1. The COMPOSITION reference the existing OPT
2. The result should be positive and a new COMPOSITION should exist in the EHR
3. Invoke the update COMPOSITION service with a valid event COMPOSITION to the existing ehr_id and preceding_version_uid should be the version uid from the COMPOSITION created in 1
   1. This COMPOSITION has the same OPT as the COMPOSITION created in 1
4. The result should be positive and a new version of the existing COMPOSITION should exist in the EHR


#### B.7.b. Alternative flow 1: update an existing persistent COMPOSITION

**Preconditions:**

1. The OPT referenced by the COMPOSITIONs to commit exists on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

1. The server should contain one VERSIONED_OBJECT
2. The VERSIONED_OBJECT should have two VERSIONs of COMPOSITION
3. The COMPOSITIONs should comply with the existing OPT

**Flow:**

1. Invoke the create COMPOSITION service with a valid persistent COMPOSITION and the existing ehr_id
   1. The OPT referenced by this COMPOSITION exists on the server
2. The result should be positive and a new COMPOSITION should exist in the EHR
3. Invoke the update COMPOSITION service with a valid persistent COMPOSITION, to the existing ehr_id
   1. that has the same template as the COMPOSITION created in 1.,
   2. preceding_version_uid should be the VERSION uid from the COMPOSITION created in 1
4. The result should be positive and a new version of the existing COMPOSITION should exist in the EHR


#### B.7.c. Alternative flow 2: update a non-existent COMPOSITION

**Preconditions:**

1. The OPT referenced by the COMPOSITIONs to commit exists on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

None

**Flow:**

1. Invoke the update COMPOSITION service with a valid event COMPOSITION, the existing ehr_id and preceding_version_uid should be a random value
   1. The COMPOSITION should comply with the existing OPT
2. The result should be negative and return an error related to the inexistence of the preceding_version_id


#### B.7.d. Alternative flow 3: update an existing COMPOSITION, referencing a different template

**Preconditions:**

1. The OPTs, referenced by the COMPOSITIONs to commit, exist on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

1. The server has a new VERSIONED_OBJECT
2. The VERSIONED_OBJECT has one VERSION of a COMPOSITION

**Flow:**

1. Invoke the create COMPOSITION service with a valid event COMPOSITION and the existing ehr_id
   1. The OPT referenced by this COMPOSITION exists on the server
2. The result should be positive and a new COMPOSITION should exist in the EHR
3. Invoke the update COMPOSITION service with a valid event COMPOSITION, to the existing ehr_id and preceding_version_uid should be the version uid from the COMPOSITION created in 1
   1. The COMPOSITION references a different template than the one referenced by the COMPOSITION created in 1.
   2. The OPT referenced by this COMPOSITION exists on the server
4. The result should be negative and return an error related to the template_id mismatch



### B.8. Delete COMPOSITION

#### B.8.a. Main flow: delete event COMPOSITION

**Preconditions:**

1. The OPT referenced by the COMPOSITIONs to commit exists on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

1. The server has one VERSIONED_OBJECT
2. The VERSIONED_OBJECT contains two VERSIONS of COMPOSITION
3. The second VERSION.lifecycle_state value is the code openehr::523(deleted)

**Flow:**

1. Invoke the create COMPOSITION service with a valid event COMPOSITION and the existing ehr_id
   1. The COMPOSITION complies with the existing OPT
2. The result should be positive and a new COMPOSITION should exist in the EHR
3. Invoke the delete COMPOSITION service with the existing ehr_id and preceding_version_uid should be the version id of the COMPOSITION created in 1
4. The result should be positive, and the COMPOSITION should be deleted


**Notes:**

The common implementation of the `delete` operation is two create a new VERSION of the COMPOSITION that has VERSION.commit_audit.change_type == openehr::523(deleted), and VERSION.lifecycle_state == openehr::523(deleted). So the `delete` operation is not a physical delete but a logical delete. Some implementations might add the option of a physical deleted. This test case considers the `postcondition` to be a logical delete, which behaves like an `update` operation in which a new VERSION of an existing COMPOSITION is created.


#### B.8.b. Alternative flow 1: delete persistent COMPOSITION

**Preconditions:**

1. The OPT referenced by the COMPOSITIONs to commit exists on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

1. The server has one VERSIONED_OBJECT
2. The VERSIONED_OBJECT contains two VERSIONS of COMPOSITION
3. The second VERSION.lifecycle_state value is the code openehr::523(deleted)

**Flow:**

1. Invoke the create COMPOSITION service with a valid persistent COMPOSITION and the existing ehr_id
2. The result should be positive and a new COMPOSITION should exist in the EHR
3. Invoke the delete COMPOSITION service with the existing ehr_id and preceding_version_uid should be the version id of the COMPOSITION created in 1
4. The result should be positive, and the COMPOSITION should be deleted


#### B.8.c. Alternative flow 2: delete non-existent COMPOSITION

**Preconditions:**

1. The OPT referenced by the COMPOSITIONs to commit exists on the server
2. An EHR with known ehr_id should exist
3. The EHR should have no commits

**Postconditions:**

None

**Flow:**

1. Invoke the delete COMPOSITION service with the existing ehr_id and a random preceding_version_uid
2. The result should be negative and return an error related to the non-existent COMPOSITION
