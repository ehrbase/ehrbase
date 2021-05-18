# openEHR Conformance Testing Specification (KNOWLEDGE)


| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2019-02-21) | First version covering OPT 1.4 related services.           |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2021-02-14) | Integration into single document, minor grammar fixes.     |



# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [Knowledge Validation Suite](#knowledge-validation-suite)
  - [A. General Requirements](#a-general-requirements)
  - [B. OPT 1.4/2 Support Validation](#b-opt-142-support-validation)
    - [Considerations](#considerations)
  - [C. OPT 1.4/2 Test cases](#c-opt-142-test-cases)
    - [C.1. Validate OPT](#c1-validate-opt)
      - [C.1.a. Main flow: validate valid OPTs](#c1a-main-flow-validate-valid-opts)
      - [C.1.b. Alternative flow 1: validate invalid OPTs](#c1b-alternative-flow-1-validate-invalid-opts)
    - [C.2. Upload OPT](#c2-upload-opt)
      - [C.2.a. Main flow: upload valid OPTs](#c2a-main-flow-upload-valid-opts)
      - [C.2.b. Alternative flow 1: upload invalid OPTs](#c2b-alternative-flow-1-upload-invalid-opts)
      - [C.2.c. Alternative flow 2: upload valid OPT twice with conflict](#c2c-alternative-flow-2-upload-valid-opt-twice-with-conflict)
      - [C.2.d. Alternative flow 3: upload valid OPT twice with no conflict](#c2d-alternative-flow-3-upload-valid-opt-twice-with-no-conflict)
    - [C.3. Retrieve OPT](#c3-retrieve-opt)
      - [C.3.a. Main flow: retrieve a single OPT](#c3a-main-flow-retrieve-a-single-opt)
      - [C.3.b. Alternative flow 1: empty server OPT retrieve fail test](#c3b-alternative-flow-1-empty-server-opt-retrieve-fail-test)
      - [C.3.c. Alternative flow 3: retrieve last version of versioned OPT](#c3c-alternative-flow-3-retrieve-last-version-of-versioned-opt)
      - [C.3.d. Alternative flow 4: retrieve a specific version (not last) of versioned OPT](#c3d-alternative-flow-4-retrieve-a-specific-version-not-last-of-versioned-opt)
    - [C.4. Retrieve OPTs](#c4-retrieve-opts)
      - [C.4.a. Main flow: retrieve all loaded OPTs](#c4a-main-flow-retrieve-all-loaded-opts)
      - [C.4.b. Alternative flow 1: retrieve all loaded OPTs when none is loaded](#c4b-alternative-flow-1-retrieve-all-loaded-opts-when-none-is-loaded)
    - [C.5. Delete OPT](#c5-delete-opt)
      - [C.5.a. Main flow: delete existing OPTs](#c5a-main-flow-delete-existing-opts)
      - [C.5.b. Alternative flow 1: delete last version of a versioned OPT](#c5b-alternative-flow-1-delete-last-version-of-a-versioned-opt)
      - [C.5.c. Alternative flow 2: delete non latest version of a versioned OPT](#c5c-alternative-flow-2-delete-non-latest-version-of-a-versioned-opt)
      - [C.5.d. Alternative flow 3: delete a non existing OPT, fail test](#c5d-alternative-flow-3-delete-a-non-existing-opt-fail-test)
  - [D. ADL 1.4/2 Test Cases](#d-adl-142-test-cases)
  - [E. Terminology Test cases](#e-terminology-test-cases)
<!--te-->



# Knowledge Validation Suite

Items under this validation suite conceptually use these abstract interfaces:

- I_DEFINITION_ADL14
- I_DEFINITION_ADL2
- I_TERMINOLOGY


Reference: https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_openehr_platform_model


## A. General Requirements

1. The server under test should support at least OPTs, 1.4 or 2, but OPT 1.4 if more frequent since modeling tools supporting this were around for a long time. Could also support ADL, 1.4 or 2.

2. The server should be able to validate OPTs and archetypes uploaded to it, or even provide a service to do so before uploading (useful while developing).

3. The server should support different versions of the same OPTs and archetypes.


## B. OPT 1.4/2 Support Validation

### Considerations

1. The server under tests should support the full cycle of OPT management, including: validation, loading, versioning, retrieving, delete or deactivation (data for this OPT is loaded but no new data should be accepted for it). For the delete, the internal behavior should be defined: 1. if data was committed referencing an OPT, can it be physically deleted? or should be logically deleted? 2. if there is no data, can the OPT be deleted physically? Logical delete might work as the deactivation mentioned above.

2. The test cases are the same for OPT 1.4/2, but tests should be written separately and different datasets should be created for 1.4 and 2.

3. Different implementations might use specific formats for the template IDs, when testing each server, the template IDs should be adapted to prevent failures for wrong format on the template ID. This is due openEHR not defining a format for the template IDs in the specifications.


## C. OPT 1.4/2 Test cases

### C.1. Validate OPT

**Data set:**
- minimal valid OPT (each containing each entry type)
- maximal valid OPT (all types in the RM)
- invalid OPT (empty file)
- invalid OPT (empty template_id)
- invalid OPT (removed mandatory elements)
- invalid OPT (added multiple elements that had an upper bound of 1)


#### C.1.a. Main flow: validate valid OPTs

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None (validation should not change the state of the system).


Note: some servers might not have a way to just validate the OPT, and validation might be part of the OPT upload functionality. In that case, the validation should upload and validate the OPT, and in the cases of valid OPTs, the OPT should be deleted afterwards, so the system state doesn't change. For invalid OPTs, the upload should fail.

**Flow:**

1. For each valid OPT in the data set, invoke the OPT validation service
2. The result should be positive and the server should return should be related to "OPT is valid".


#### C.1.b. Alternative flow 1: validate invalid OPTs

**Preconditions:**

The server should be empty (no EHRs, no commits, no OPTs).

**Postconditions:**

None (validation should not change the state of the system).

**Flow:**

1. For each invalid OPT in the data set, invoke the OPT validation service
2. The result should be negative related to the "OPT is invalid", would be useful if the server also returns where the problems are in the OPT


**Implementation note:** when a step says "for each X, invoke service Y", means that the test should run completely for each X, that is, the preconditions and postconditions apply to the run for X. So if we have:

Test set: a, b, c
Test case:
- pre
- post
- flow
  - 1. for each X in data set, run service Y
  - 2. verify condition

The run should be:

- pre
- invoke Y(a)
- verify condition 
- post

- pre
- invoke Y(b)
- verify condition 
- post

- pre
- invoke Y(c)
- verify condition 
- post



### C.2. Upload OPT

**Data set:**
- minimal valid OPT (each with one type of entry, cover all entries)
- minimal valid OPT, two versions
- maximal valid OPT (all types in the RM)
- invalid OPT (empty file)
- invalid OPT (empty template_id)
- invalid OPT (removed mandatory elements)
- invalid OPT (added multiple elements that had an upper bound of 1)


#### C.2.a. Main flow: upload valid OPTs

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

A new OPT with the given template_id is loaded into the server.

Note: the server should be able to retrieve the template by template_id or retrieve if an OPT exists or not by template_id.

**Flow:**

1. For each valid OPT in the data set, invoke the OPT upload service
2. The result should be positive, the server accepted the OPT and it is stored as it was uploaded


#### C.2.b. Alternative flow 1: upload invalid OPTs

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

No OPTs should be loaded on the system.

**Flow:**

1. For each invalid OPT in the data set, invoke the OPT upload service
2. The result should be negative, the server rejected the OPT because it was invalid, and would be useful if the result contains where the errors are in the uploaded OPT.


#### C.2.c. Alternative flow 2: upload valid OPT twice with conflict

Note: since there is no formal versioning mechanism for templates 1.4 (OPT 2 might use the archetype id format for the template id that also includes a version number, but this is not widely used), the OPT upload service needs to handle a version parameter, for instance this is the solution on the openEHR REST API (https://specifications.openehr.org/releases/ITS-REST/Release-1.0.0/definitions.html#definitions-adl-1.4-template). If the version information is not available when uploading OPTs, then uploading an OPT with the same template_id twice will make the second upload fail (conflict).

An alternative solution for the version parameter is to add the version number to the other_details of the OPT, or directly into the template_id.

See: https://openehr.atlassian.net/browse/SPECBASE-30 and https://openehr.atlassian.net/browse/SPECITS-42

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

A new OPT with the given template_id is loaded into the server, and there will be only one OPT loaded.

Note: the server should be able to retrieve the template by template_id, or retrieve if an OPT exists or not by template_id.

**Flow:**

1. For each valid OPT in the data set, invoke the OPT upload service
2. The result should be positive (the server accepted the OPT)
3. Invoke the upload service with the same OPT as in 1.
4. The result should be negative (the server rejected the OPT)


#### C.2.d. Alternative flow 3: upload valid OPT twice with no conflict

Note: considering the note on the previous flow, for this flow the version parameter is provided, and both service invocations contain a different version value.

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

Two new OPTs with the given template_id and different versions are loaded into the server.

Note: the server should be able to retrieve the templates by template_id and version, or retrieve if an OPT exists or not by template_id and version. Given only the template id, the server will return just the latest version.

**Flow:**

1. For each valid OPT in the data set, invoke the OPT upload service, including the version parameter = 1
2. The result should be positive (the server accepted the OPT)
3. Invoke the upload service with the same OPT as in 1., including the version parameter = 2
4. The result should be positive (the server accepted the OPT)


### C.3. Retrieve OPT

Note: the flows of this test case will include flows from the Upload OPT test case, in order to have something to retrieve.

**Data set:**
- minimal valid OPT (covering all entry types)
- minimal valid OPT, two versions
- maximal valid OPT (all types in the RM)


#### C.3.a. Main flow: retrieve a single OPT

**Preconditions:**

All valid OPTs should be loaded into the system, only the single versioned ones.

**Postconditions:**

None (retrieve should not change the state of the system).

**Flow:**

1. Invoke the retrieve OPT service with existing template_ids
2. For each template_id, the correct OPT will be returned

Note: to check point 2, the retrieved OPT should be exactly the same as the uploaded one.


#### C.3.b. Alternative flow 1: empty server OPT retrieve fail test

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

None

**Flow:**

1. Invoke the retrieve OPT service with a random template_id
2. The service should return an error related to the non existence of the requested OPT


#### C.3.c. Alternative flow 3: retrieve last version of versioned OPT

**Preconditions:**

OPTs with more than one version should be loaded

**Postconditions:**

None

**Flow:**

1. Invoke the retrieve OPT service with existing template_ids
2. For each template_id, the correct OPT will be returned, and will be the last version

Note: to be sure the last version was returned, a small modification to the OPT could be done.


#### C.3.d. Alternative flow 4: retrieve a specific version (not last) of versioned OPT

**Preconditions:**

OPTs with more than one version should be loaded

**Postconditions:**

None

**Flow:**

1. Invoke the retrieve OPT service with existing template_ids and a version parameter value that is not the last
2. For each template_id, the correct OPT will be returned, and will be the requested version

Note: to be sure the last version was returned, a small modification to the OPT could be done.



### C.4. Retrieve OPTs

**Data set:**
- minimal valid OPT (covering each type of entry)
- minimal valid OPT, two versions
- maximal valid OPT (all types in the RM)


#### C.4.a. Main flow: retrieve all loaded OPTs

**Preconditions:**

All valid OPTs should be loaded.

**Postconditions:**

None

**Flow:**

1. Invoke the retrieve OPTs service
2. All the loaded OPTs should be returned, if there are versions of any OPTs, only the last version is retrieved


#### C.4.b. Alternative flow 1: retrieve all loaded OPTs when none is loaded

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

None

**Flow:**

1. Invoke the retrieve OPTs service
2. The service should return an empty set and should not fail.


### C.5. Delete OPT

Note: the OPT delete can only happen if there is no associated data with the OPT, or if there exists a newer revision (minor version of the same OPT) in the server under test. For all these tests, there is not data committed to the server, so the delete can happen.

Implementation recommendations: the delete could be logical, so the OPT exists in the server but is not available, and there could be a service to retrieve deleted OPTs. Those can be undeleted or physically deleted (this can't be undone), and only users with admin permissions should be able to physically delete OPTs.

**Data set:**
- minimal valid OPT
- minimal valid OPT, two versions
- maximal valid OPT (all types in the RM)


#### C.5.a. Main flow: delete existing OPTs

**Preconditions:**

All valid OPTs should be loaded into the system

**Postconditions:**

None

**Flow:**

1. For each existing template_id, invoke the delete OPT service
2. Verify the OPT is not longer available via the retrieve OPTs service


Note: for step 1, exclude versioned OPT, the result should be the same: the OPT is not available.


#### C.5.b. Alternative flow 1: delete last version of a versioned OPT

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

None (the server will be in the same state as the initial).

**Flow:**

1. Include flow: upload valid OPTs
2. Invoke the delete OPT service for all existing template_ids
3. Include flow: retrieve all loaded OPTs when none is loaded


Notes:
- for step 1, include only versioned OPT.
- for versioned OPTs, when no version parameter is present when invoking the delete OPT service, all the versions of the OPT will be deleted.


#### C.5.c. Alternative flow 2: delete non latest version of a versioned OPT

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

None (the server will be in the same state as the initial).

**Flow:**

1. Include flow: upload valid OPTs
2. Invoke the delete OPT service for an existing template_id and version of the OPT, version should not be the last
3. Include flow: retrieve all loaded OPTs
4. The OPT set retrieved on step 3 should contain the deleted OPT, since the latest version was not deleted
5. Include flow: delete existing OPTs

Note: for step 1, include only versioned OPT.

#### C.5.d. Alternative flow 3: delete a non existing OPT, fail test

**Preconditions:**

No OPTs should be loaded on the system.

**Postconditions:**

None (the server will be in the same state as the initial).

**Flow:**

1. Include flow: upload valid OPTs
2. Invoke the delete OPT service with a non existing template_id
3. The server will return an error related to the OPT not existing in the server
4. Include flow: delete existing OPTs


## D. ADL 1.4/2 Test Cases

To be done.


## E. Terminology Test cases

To be done.

Since the I_TERMINOLOGY interface is not yet defined in the openEHR Platform Model specification, we can only suggest some ideas to create test cases for the future.

Use cases to be considered:

- return part of one terminology
   - a "part" could be a domain, a chapter, a subset, etc.
   - this includes codes, rubrics and definitions if available
- retrieve descendants, given one terminology item
   - this applies only for hierarchical terminologies
   - a terminology item could be given by any of it's identifiers in the correspondent terminology
- retrieve ancestors, given one terminology item
   - this applies only for hierarchical terminologies
   - a terminology item could be given by any of it's identifiers in the correspondent terminology
- offer suggestions based on given text
   - text might be partial, complete or acronym
   - current language is needed as context
   - part of the terminology might be required to give context and constraint results
   - queried terminology is required
- retrieve mappings of terminology items between different terminologies
   - given a terminology term, it's terminology and the target terminology, retrieve the matching terminology items from the target terminology
