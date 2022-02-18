# openEHR Conformance Testing Specification (EHR)

| Author                                           | Version              | Comments                                                                                     |
|:------------------------------------------------:|:--------------------:|:--------------------------------------------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1   (2019-02-20)   | First version covering EHR related services.                                                 |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2   (2019-03-12)   | Removed CONTRIBUTION section.                                                                |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2.1 (2019-12-30)   | Added comments about EHR_STATUS.other_details.                                               |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.3   (2020-04-07)   | Updated data sets for create EHR.                                                            |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.4   (2021-04-19)   | Refactored data sets for create EHR. Improved description of the flows, grammar and comments |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.4.1 (2022-02-11)   | Clarified test data sets and test cases related to subject_id / subject.external_ref         |


# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [EHR Validation Suite](#ehr-validation-suite)
  - [A. General Requirements](#a-general-requirements)
  - [B. EHR Service Test Cases](#b-ehr-service-test-cases)
    - [B.1. Create EHR](#b1-create-ehr)
      - [B.1.a. Main flow: Create new EHR](#b1a-main-flow-create-new-ehr)
      - [B.1.b. Alternative flow 1: Attempt to create same EHR twice](#b1b-alternative-flow-1-attempt-to-create-same-ehr-twice)
      - [B.1.c. Alternative flow 2: Create two EHRs for the same patient](#b1c-alternative-flow-2-create-two-ehrs-for-the-same-patient)
    - [B.2. Has EHR](#b2-has-ehr)
      - [B.2.a. Main flow: Check has EHR with existing EHR](#b2a-main-flow-check-has-ehr-with-existing-ehr)
      - [B.2.b. Alternative flow 1: Check has EHR with existing EHR by subject_id](#b2b-alternative-flow-1-check-has-ehr-with-existing-ehr-by-subject_id)
      - [B.2.c. Alternative flow 2: Check has EHR with non existing EHR](#b2c-alternative-flow-2-check-has-ehr-with-non-existing-ehr)
      - [B.2.d. Alternative flow 3: Check has EHR with non existing EHR by subject_id](#b2d-alternative-flow-3-check-has-ehr-with-non-existing-ehr-by-subject_id)
    - [B.3. Get EHR](#b3-get-ehr)
      - [B.3.a. Main flow: Get existing EHR](#b3a-main-flow-get-existing-ehr)
      - [B.3.b. Alternative flow 1: Get existing EHR by subject_id](#b3b-alternative-flow-1-get-existing-ehr-by-subject_id)
      - [B.3.c. Alternative flow 2: Get non existing EHR](#b3c-alternative-flow-2-get-non-existing-ehr)
      - [B.3.d. Alternative flow 3: Get non existing EHR by subject_id](#b3d-alternative-flow-3-get-non-existing-ehr-by-subject_id)
  - [C. EHR_STATUS Test Cases](#c-ehr_status-test-cases)
    - [C.1. Get EHR_STATUS](#c1-get-ehr_status)
      - [C.1.a. Main flow: Get status of an existing EHR](#c1a-main-flow-get-status-of-an-existing-ehr)
      - [C.1.b. Alternative flow 1: Get status of a non existing EHR](#c1b-alternative-flow-1-get-status-of-a-non-existing-ehr)
    - [C.2. Set EHR Queryable](#c2-set-ehr-queryable)
      - [C.2.a. Main flow: Set EHR queryable of an existing EHR](#c2a-main-flow-set-ehr-queryable-of-an-existing-ehr)
      - [C.2.b. Alternative flow 1: Set EHR queryable of non existing EHR](#c2b-alternative-flow-1-set-ehr-queryable-of-non-existing-ehr)
    - [C.3. Set EHR Modifiable](#c3-set-ehr-modifiable)
      - [C.3.a. Main flow: Set EHR modifiable of an existing EHR](#c3a-main-flow-set-ehr-modifiable-of-an-existing-ehr)
      - [C.3.b. Alternative flow 1: Set EHR modifiable of non existing EHR](#c3b-alternative-flow-1-set-ehr-modifiable-of-non-existing-ehr)
    - [C.4. Clear EHR Queryable](#c4-clear-ehr-queryable)
      - [C.4.a. Main flow: Clear EHR queryable of an existing EHR](#c4a-main-flow-clear-ehr-queryable-of-an-existing-ehr)
      - [C.4.b. Alternative flow 1: Clear EHR queryable of non existing EHR](#c4b-alternative-flow-1-clear-ehr-queryable-of-non-existing-ehr)
    - [C.5. Clear EHR Modifiable](#c5-clear-ehr-modifiable)
      - [C.5.a. Main flow: Clear EHR modifiable of an existing EHR](#c5a-main-flow-clear-ehr-modifiable-of-an-existing-ehr)
      - [C.5.b. Alternative flow 1: Clear EHR modifiable of non existing EHR](#c5b-alternative-flow-1-clear-ehr-modifiable-of-non-existing-ehr)
<!--te-->


# EHR Validation Suite

Items under this validation suite conceptually use these abstract interfaces:

- I_EHR_SERVICE
- I_EHR
- I_EHR_STATUS


Reference: https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_openehr_platform_model


## A. General Requirements

1. The server under test should support at least OPTs, 1.4 or 2, but OPT 1.4 if more frequent since there are more modeling tools supporting OPT 1.4.

2. The server should support at least the XML representation of COMPOSITIONs for committing data, since we can use the openEHR XSDs (and later JSON schemas) to validate those formats. We can't test, for now, other formats since we don't have official tools to validate them.

3. The server might be able to validate the internal contents of the COMPOSITION against the constraints in the correspondent OPT. If the server supports this, tests for that validation should be designed.


Notes:

-  OPT 1.4 is a [format specification](https://specifications.openehr.org/releases/ITS-XML/latest/components/AM/Release-1.4/).
-  OPTs 1.4 are derived from [ADL 1.4](https://specifications.openehr.org/releases/AM/Release-2.0.6/ADL1.4.html) archetypes, which are based on the [AOM 1.4](https://specifications.openehr.org/releases/AM/Release-2.0.6/AOM1.4.html).


## B. EHR Service Test Cases

### B.1. Create EHR

**Data sets:**

These are the data set classes:

1. VALID:
   1. not providing and EHR_STATUS (empty input, the server creates the default structures and data)
   2. providing a valid EHR_STATUS
2. INVALID:
   1. providing invalid EHR_STATUS


**Valid test data sets when the EHR_STATUS is provided and internal strucrures are valid (data set class 1.2):**

| No. | is_queryable | is_modifiable | subject.external_ref | other_details | ehr_id       |
| --- | ------------ | ------------- | -------------------- | ------------- | ------------ |
| 1   | true         | true          | provided             | not provided  | not provided |
| 2   | true         | false         | provided             | not provided  | not provided |
| 3   | false        | true          | provided             | not provided  | not provided |
| 4   | false        | false         | provided             | not provided  | not provided |
| 5   | true         | true          | provided             | provided      | not provided |
| 6   | true         | false         | provided             | provided      | not provided |
| 7   | false        | true          | provided             | provided      | not provided |
| 8   | false        | false         | provided             | provided      | not provided |
| 9   | true         | true          | provided             | not provided  | provided     |
| 10  | true         | false         | provided             | not provided  | provided     |
| 11  | false        | true          | provided             | not provided  | provided     |
| 12  | false        | false         | provided             | not provided  | provided     |
| 13  | true         | true          | provided             | provided      | provided     |
| 14  | true         | false         | provided             | provided      | provided     |
| 15  | false        | true          | provided             | provided      | provided     |
| 16  | false        | false         | provided             | provided      | provided     |
| 17  | true         | true          | not provided         | not provided  | not provided |
| 18  | true         | false         | not provided         | not provided  | not provided |
| 19  | false        | true          | not provided         | not provided  | not provided |
| 20  | false        | false         | not provided         | not provided  | not provided |
| 21  | true         | true          | not provided         | provided      | not provided |
| 22  | true         | false         | not provided         | provided      | not provided |
| 23  | false        | true          | not provided         | provided      | not provided |
| 24  | false        | false         | not provided         | provided      | not provided |
| 25  | true         | true          | not provided         | not provided  | provided     |
| 26  | true         | false         | not provided         | not provided  | provided     |
| 27  | false        | true          | not provided         | not provided  | provided     |
| 28  | false        | false         | not provided         | not provided  | provided     |
| 29  | true         | true          | not provided         | provided      | provided     |
| 30  | true         | false         | not provided         | provided      | provided     |
| 31  | false        | true          | not provided         | provided      | provided     |
| 32  | false        | false         | not provided         | provided      | provided     |


**Any other data set should be treated as invalid, for instance providing EHR_STATUS with:**

- missing is_queryable, is_modifiable
- empty is_queryable, is_modifiable
- missing or empty subject
- invalid subject (e.g. external_ref is present but external_ref.id is empty)
- invalid other_details


**Notes**:

1. When the ehr_id is not present, it is expected that it is assigned by the server.
2. The server should set the EHR.system_id value to a known value (defined by the server's configuration).
3. The default values that should be assigned by the server for is_modifiable and is_queryable are "true", and for the subject it defaults to an instance of PARTY_SELF with no exter_ref.
4. There are no cases to check if the provided ehr_id is valid, since in the [openEHR Service Model](https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_service_interface) the parameters is typed to UUID, any other format will be an invalid call.
5. The validity of an EHR_STATUS can be checked in it's JSON form by validating against the JSON schemas https://specifications.openehr.org/releases/ITS-JSON/latest/components/RM
6. The test cases mentioning subject_id refer to the EHR_STATUS.subject.external_ref.id value, mentioned just as `subject.external_ref` in the test data sets table above.



#### B.1.a. Main flow: Create new EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

A new EHR will exist in the system and be consistent with the data sets used.

**Flow:**

1. Invoke the create EHR service
   1. for each item in the VALID data set classes
   2. when the ehr_id is provided, should be unique for each invocation of the service
2. The server should answer with a positive response associated to the successful EHR creation



#### B.1.b. Alternative flow 1: Attempt to create same EHR twice

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).


**Postconditions:**

A new EHR will exist in the system, the first one created, and be consistent with the data sets used.


**Flow:**

1. Invoke the create EHR service
   1. for each VALID data set not providing ehr_id
   2. for each VALID data set providing ehr_id
2. The server should answer with a positive response associated to the successful EHR creation
3. Invoke the create EHR service
   1. with the same ehr_id of the EHR created in 1.1. (should be read from the response)
   2. with the same ehr_id of the EHR created in 1.2. (should be read from the test data sets)
4. The server should answer with a negative response, related to the existence of an EHR with the provided ehr_id, because ehr_id values should be unique



#### B.1.c. Alternative flow 2: Create two EHRs for the same patient

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

A new EHR will exist in the system.

**Flow:**

1. Invoke the create EHR service
   1. for each VALID data set with a provided subject.external_ref and not providing ehr_id
2. The server should answer with a positive response associated to the successful EHR creation
3. Invoke the create EHR service
   1. with the same data set used in 1.1
4. The server should answer with a negative response, related with the EHR already existing for the provided subject.external_ref



### B.2. Has EHR

#### B.2.a. Main flow: Check has EHR with existing EHR

**Preconditions:**

An EHR should exist in the system with a known ehr_id.

**Postconditions:**

None.

**Flow:**

1. Invoke has EHR service with the known ehr_id
2. The result should be positive, related to "the EHR with ehr_id exists"


#### B.2.b. Alternative flow 1: Check has EHR with existing EHR by subject_id

**Preconditions:**

An EHR should exist in the system with a known subject_id.

**Postconditions:**

None.

**Flow:**

1. Invoke has EHR service with the existing subject_id
2. The result should be positive, realted to "the EHR with subject_id exists"


Note: 'subject_id' refers to the PARTY_REF class instance containing the identifier of a patient represented by PARTY_SELF in the openEHR Reference Model.


#### B.2.c. Alternative flow 2: Check has EHR with non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None.

**Flow:**

1. Invoke has EHR service with a random ehr_id.
2. The result should be negative, related to "the EHR with ehr_id does not exist"


#### B.2.d. Alternative flow 3: Check has EHR with non existing EHR by subject_id

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None.

**Flow:**

1. Invoke has EHR service with a random subject_id
2. The result should be negative, related to "the EHR for subject_id does not exist"



### B.3. Get EHR

#### B.3.a. Main flow: Get existing EHR

**Preconditions:**

An EHR should exist in the system with a known ehr_id.

**Postconditions:**

None.

**Flow:**

1. Invoke get EHR service with the known ehr_id
2. The result should be positive and retrieve the EHR


#### B.3.b. Alternative flow 1: Get existing EHR by subject_id

**Preconditions:**

An EHR should exist in the system with a known subject_id.

**Postconditions:**

None.

**Flow:**

1. Invoke get EHR service with the existing subject_id
2. The result should be positive and retrieve the EHR


#### B.3.c. Alternative flow 2: Get non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None.

**Flow:**

1. Invoke get EHR service by a random ehr_id
2. The result should be negative, related to "EHR with ehr_id doesn't exist"


#### B.3.d. Alternative flow 3: Get non existing EHR by subject_id

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None

**Flow:**

1. Invoke get EHR service by a random subject_id
2. The result should be negative, relato to "EHR for subject_id doesn't exist"


## C. EHR_STATUS Test Cases

### C.1. Get EHR_STATUS

#### C.1.a. Main flow: Get status of an existing EHR

**Preconditions:**

An EHR with known ehr_id should exist.

**Postconditions:**

None

**Flow:**

1. Invoke the get EHR_STATUS service by the existing ehr_id
2. The result should be positive and retrieve a correspondent EHR_STATUS.
   1. The EHR_STATUS internal information should match the rules in which the EHR was created (see test flow Create EHR)
   2. Those rules should be verified: a. has or not a subject_id, b. has correct value for is_modifiable, c. has correct value for is_queryable.


#### C.1.b. Alternative flow 1: Get status of a non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None

**Flow:**

1. Invoke the get EHR_STATUS service by a random ehr_id
2. The result should be negative and the result should include an error related to "EHR with ehr_id doesn't exist".



### C.2. Set EHR Queryable

#### C.2.a. Main flow: Set EHR queryable of an existing EHR

**Preconditions:**

An EHR with known ehr_id should exist.

**Postconditions:**

EHR_STATUS.is_queryable, for the EHR with known ehr_id, should be true

**Flow:**

1. For the existing EHR, invoke the set EHR queryable service
2. The result should be positive and the corresponding EHR_STATUS.is_queryable should be true


#### C.2.b. Alternative flow 1: Set EHR queryable of non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None

**Flow:**

1. Invoke the set EHR queryable service by a random ehr_id
2. The result should be negative and the result should include an error related to "EHR with ehr_id doesn't exist".



### C.3. Set EHR Modifiable

#### C.3.a. Main flow: Set EHR modifiable of an existing EHR

**Preconditions:**

An EHR with known ehr_id should exist.

**Postconditions:**

EHR_STATUS.is_modifiable, for the EHR with known ehr_id, should be true

**Flow:**

1. For the existing EHR, invoke the set EHR modifiable service
2. The result should be positive and the corresponding EHR_STATUS.is_modifiable should be true


#### C.3.b. Alternative flow 1: Set EHR modifiable of non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None

**Flow:**

1. Invoke the set EHR modifiable service by a random ehr_id
2. The result should be negative and the result should include an error related to "EHR with ehr_id doesn't exist".



### C.4. Clear EHR Queryable

#### C.4.a. Main flow: Clear EHR queryable of an existing EHR

**Preconditions:**

An EHR with known ehr_id should exist.

**Postconditions:**

EHR_STATUS.is_queryable, for the EHR with known ehr_id, should be false.

**Flow:**

1. For the existing EHR, invoke the clear EHR queryable service
2. The result should be positive and the corresponding EHR_STATUS.is_queryable should be false


#### C.4.b. Alternative flow 1: Clear EHR queryable of non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None

**Flow:**

1. Invoke the clear EHR queryable service by a random ehr_id
2. The result should be negative and the result should include an error related to "EHR with ehr_id doesn't exist".



### C.5. Clear EHR Modifiable

#### C.5.a. Main flow: Clear EHR modifiable of an existing EHR

**Preconditions:**

An EHR with known ehr_id should exist.

**Postconditions:**

EHR_STATUS.is_modifiable, for the EHR with known ehr_id, should be false

**Flow:**

1. For the existing EHR, invoke the clear EHR modifiable service
2. The result should be positive and the corresponding EHR_STATUS.is_modifiable should be false


#### C.5.b. Alternative flow 1: Clear EHR modifiable of non existing EHR

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None

**Flow:**

1. Invoke the clear EHR modifiable service by a random ehr_id
2. The result should be negative and the result should include an error related to "EHR with ehr_id doesn't exist".



