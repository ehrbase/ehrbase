# openEHR Conformance Testing Specification (FOLDER)

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2019-06-18) | First version |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2019-09-02) | Added annex with REST API proposal |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.3 (2020-03-02) | Added test cases for update directory |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 1.0 (2021-05-18) | Small corrections and fixes before publication |


# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
- [DIRECTORY Validation Suite](#directory-validation-suite)
  - [A. General Requirements](#a-general-requirements)
  - [B. DIRECTORY Test Data Sets](#b-directory-test-data-sets)
  - [C. Has DIRECTORY Flows](#c-has-directory-flows)
    - [C.1. Main flow: has directory on empty existing EHR](#c1-main-flow-has-directory-on-empty-existing-ehr)
    - [C.2. Alternative flow 1: has directory on existing EHR with directory](#c2-alternative-flow-1-has-directory-on-existing-ehr-with-directory)
    - [C.3. Alternative flow 2: has directory on non-existent EHR](#c3-alternative-flow-2-has-directory-on-non-existent-ehr)
  - [D. Has PATH Flows](#d-has-path-flows)
    - [D.1. Main flow: has path on empty EHR](#d1-main-flow-has-path-on-empty-ehr)
    - [D.2. Alternative flow 1: has path on EHR with just root directory](#d2-alternative-flow-1-has-path-on-ehr-with-just-root-directory)
    - [D.3. Alternative flow 2: has path on EHR with given folder structure](#d3-alternative-flow-2-has-path-on-ehr-with-given-folder-structure)
    - [D.4. Alternative flow 3: has path on non-existent EHR](#d4-alternative-flow-3-has-path-on-non-existent-ehr)
  - [E. Create DIRECTORY Flows](#e-create-directory-flows)
    - [E.1. Main flow: create directory on empty EHR](#e1-main-flow-create-directory-on-empty-ehr)
    - [E.2. Alternative flow 1: create directory on EHR with directory](#e2-alternative-flow-1-create-directory-on-ehr-with-directory)
    - [E.3. Alternative flow 2: create directory on non-existent EHR](#e3-alternative-flow-2-create-directory-on-non-existent-ehr)
  - [F. Get DIRECTORY](#f-get-directory)
    - [F.1. Main flow: get directory on empty EHR](#f1-main-flow-get-directory-on-empty-ehr)
    - [F.2. Alternative flow 1: get directory on EHR with just a root directory](#f2-alternative-flow-1-get-directory-on-ehr-with-just-a-root-directory)
    - [F.3. Alternative flow 2: get directory on EHR with complex directory structure and items](#f3-alternative-flow-2-get-directory-on-ehr-with-complex-directory-structure-and-items)
    - [F.4. Alternative flow 3: get directory on non-existent EHR](#f4-alternative-flow-3-get-directory-on-non-existent-ehr)
  - [G. Get DIRECTORY at time Flows](#g-get-directory-at-time-flows)
    - [G.1. Main flow: get directory at time on empty EHR](#g1-main-flow-get-directory-at-time-on-empty-ehr)
    - [G.2. Alternative flow 1: get directory at time on empty EHR with empty time](#g2-alternative-flow-1-get-directory-at-time-on-empty-ehr-with-empty-time)
    - [G.3. Alternative flow 2: get directory at time on EHR with directory](#g3-alternative-flow-2-get-directory-at-time-on-ehr-with-directory)
    - [G.4. Alternative flow 3: get directory at time on EHR with directory with empty time](#g4-alternative-flow-3-get-directory-at-time-on-ehr-with-directory-with-empty-time)
    - [G.5. Alternative flow 4: get directory at time on EHR with directory with multiple versions in multiple times](#g5-alternative-flow-4-get-directory-at-time-on-ehr-with-directory-with-multiple-versions-in-multiple-times)
    - [G.6. Alternative flow 5: get directory at time on EHR with directory with multiple versions with empty time](#g6-alternative-flow-5-get-directory-at-time-on-ehr-with-directory-with-multiple-versions-with-empty-time)
    - [G.7. Alternative flow 7: get directory at time on non existent EHR](#g7-alternative-flow-7-get-directory-at-time-on-non-existent-ehr)
  - [H. Update DIRECTORY](#h-update-directory)
    - [H.1. Main flow: update directory from EHR with directory](#h1-main-flow-update-directory-from-ehr-with-directory)
    - [H.2. Alternative flow 1: update directory on empty EHR](#h2-alternative-flow-1-update-directory-on-empty-ehr)
    - [H.3. Alternative flow 2: update directory on non-existing EHR](#h3-alternative-flow-2-update-directory-on-non-existing-ehr)
  - [I. Delete DIRECTORY](#i-delete-directory)
    - [I.1. Main flow: delete directory on empty EHR](#i1-main-flow-delete-directory-on-empty-ehr)
    - [I.2. Alternative flow 1: delete directory from EHR with directory](#i2-alternative-flow-1-delete-directory-from-ehr-with-directory)
    - [I.3. Alternative flow 2: delete directory from non-existent EHR](#i3-alternative-flow-2-delete-directory-from-non-existent-ehr)
  - [J. Has DIRECTORY version](#j-has-directory-version)
    - [J.1. Main flow: has directory from empty EHR](#j1-main-flow-has-directory-from-empty-ehr)
    - [J.2. Alternative flow 1: has directory on EHR that has two versions of directory](#j2-alternative-flow-1-has-directory-on-ehr-that-has-two-versions-of-directory)
    - [J.3. Alternative flow 2: has directory on non-existent EHR](#j3-alternative-flow-2-has-directory-on-non-existent-ehr)
  - [K. Get DIRECTORY at version](#k-get-directory-at-version)
    - [K.1. Main flow: get directory at version from empty EHR](#k1-main-flow-get-directory-at-version-from-empty-ehr)
    - [K.2. Alternative flow 1: get DIRECTORY at version from EHR that has two versions of directory](#k2-alternative-flow-1-get-directory-at-version-from-ehr-that-has-two-versions-of-directory)
    - [K.3. Alternative flow 2: get DIRECTORY at version from non existent EHR](#k3-alternative-flow-2-get-directory-at-version-from-non-existent-ehr)
  - [L. Get versioned DIRECTORY](#l-get-versioned-directory)
    - [L.1. Main flow: get versioned directory from EHR with no directory](#l1-main-flow-get-versioned-directory-from-ehr-with-no-directory)
    - [L.2. Alternative flow 1: get versioned directory from existent EHR that has two versions of directory](#l2-alternative-flow-1-get-versioned-directory-from-existent-ehr-that-has-two-versions-of-directory)
    - [L.3. Alternative flow 2: get versioned directory from non existent EHR](#l3-alternative-flow-2-get-versioned-directory-from-non-existent-ehr)
  - [Annex: proposal for FOLDER API](#annex-proposal-for-folder-api)
    - [Background](#background)
    - [Operations for the Service Model](#operations-for-the-service-model)
<!--te-->


# DIRECTORY Validation Suite

Items under this validation suite conceptually use these abstract interfaces of the [openEHR Service Model](https://specifications.openehr.org/releases/SM/latest/openehr_platform.html):

- [I_EHR_DIRECTORY](https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_directory_interface)


## A. General Requirements

---
TBD

---

## B. DIRECTORY Test Data Sets

For the creation and modification of the EHR.directory structure it is important to explore the hierarchical nature of the FOLDER structures and consider the edge cases for EHR.directory.

**Cases to test for EHR.directory:**

1. FOLDER
2. FOLDER with items
3. FOLDER with subfolders
4. FOLDER with subfolders and items on all the folders
5. FOLDER with n levels of subfolders and items (to detect any implementation limitations)

**Reference FOLDER structure**

<div align="center">

![](img/openEHR_Test_Folder_Data.png)
</div>

> Note: the image is just for reference, the items in the FOLDER are instances of VERSIONED_OBJECT, and by the current spec that could contain COMPOSITION, EHR_STATUS and FOLDER. This documentation focuses on testing COMPOSITION as content in the FOLDERs. You can find a related question [here](https://discourse.openehr.org/t/whats-allowed-in-folder-items/1539).




## C. Has DIRECTORY Flows

### C.1. Main flow: has directory on empty existing EHR

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the has DIRECTORY service for the ehr_id
2. The result must be false


### C.2. Alternative flow 1: has directory on existing EHR with directory

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR and has a directory

**Postconditions:**

None

**Flow:**

1. Invoke the has DIRECTORY service for the ehr_id
2. The result must be true


### C.3. Alternative flow 2: has directory on non-existent EHR

**Preconditions:**

1. There are no EHRs on the server

**Postconditions:**

None

**Flow:**

1. Invoke the has DIRECTORY service for a random non-existent ehr_id
2. An error should be returned, related to the EHR that doesn't exist


## D. Has PATH Flows

### D.1. Main flow: has path on empty EHR

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the has path service for the ehr_id with a random FOLDER path
2. The result must be false


### D.2. Alternative flow 1: has path on EHR with just root directory

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR and has an empty directory (no subfolders or items)

**Postconditions:**

None

**Flow:**

1. Invoke the has path service for the ehr_id and an existing path $path from the data set
2. The result must be $result from the data set

**Data set:**

| $path      | $result     |
|:----------:|:-----------:|
| "/"        | true        |
| random()   | false       |

> Note: "/" represents the string slash, which is the default name for the root FOLDER at EHR.directory, random() represents any randomly generated path that doesn't exists.


### D.3. Alternative flow 2: has path on EHR with given folder structure

**Preconditions:**

1. An EHR with known ehr_id exists
2. The EHR has a directory with an internal structure (described below)

**Postconditions:**

None

**Flow:**

1. Invoke the has path service for the ehr_id and the path $path from the data set
2. The result must be $result from the data set

**Data set:**

Consdidering the following structure exist at EHR.directory:

- /
  - emergency
    - episode-x
      - summary-composition-x
    - episode-y
      - summary-composition-y
  - hospitalization
    - summary-composition-z

> Note: these are the names of the FOLDERs and COMPOSITIONs in EHR.directory.

| $path                                        | $result     |
|:---------------------------------------------|:-----------:|
| "/"                                          | true        |
| "/emergency"                                 | true        |
| "/emergency/episode-x"                       | true        |
| "/emergency/episode-x/summary-composition-x" | true        |
| "/emergency/episode-y"                       | true        |
| "/emergency/episode-y/summary-composition-y" | true        |
| "/hospitalization"                           | true        |
| "/hospitalization/summary-composition-z"     | true        |
| "/" + random()                               | false       |
| "/emergency/" + random()                     | false       |
| "/emergency/episode-x/" + random()           | false       |
| random()                                     | false       |

> Note: the table mixes cases that exist with cases of paths which part exists and parts doesn't exist. The final one is jsut a totally random path.


### D.4. Alternative flow 3: has path on non-existent EHR

**Preconditions:**

1. The server is empty

**Postconditions:**

None

**Flow:**

1. Invoke the has path service for a random ehr_id and path
2. The service should return an error, related to the EHR that doesn't exist


## E. Create DIRECTORY Flows

### E.1. Main flow: create directory on empty EHR

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR doesn't have directory

**Postconditions:**

1. The EHR ehr_id should have a directory

**Flow:**

1. Invoke the create directory service with the existing ehr_id and a valid FOLDER structure
   1. Use any of the data sets described on the previous tests and the reference directory structure
2. The service should return a positive result related with the directory just created for the EHR
 

### E.2. Alternative flow 1: create directory on EHR with directory

**Preconditions:**

1. An EHR with ehd_id exists
2. The EHR has a directory

**Postconditions:**

None

**Flow:**

1. Invoke the create directory service with the existing ehr_id and a valid FOLDER structure
   1. Use any of the data sets described on the previous tests and the reference directory structure
3. The service should return an error, related to the EHR directory already existing


### E.3. Alternative flow 2: create directory on non-existent EHR

**Preconditions:**

1. The server should be empty

**Postconditions:**

None

**Flow:**

1. Invoke the create directory service for a random ehr_id
2. The service should return an error, related to the EHR that doesn't exist



## F. Get DIRECTORY

### F.1. Main flow: get directory on empty EHR

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the get directory service for the ehr_id
2. The service should return an empty structure

> Implementation note: considering a REST API implementation, the result could be an error status instead of an empty structure.


### F.2. Alternative flow 1: get directory on EHR with just a root directory

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR has a directory which is empty

**Postconditions:**

None

**Flow:**

1. Invoke the get directory service for the ehr_id
2. The service should return the structure of the EHR.directory: an empty FOLDER


### F.3. Alternative flow 2: get directory on EHR with complex directory structure and items

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR has a directory with a complex structure (use the data sets from the previous tests and the reference directory structure)

**Postconditions:**

None

**Flow:**

1. Invoke the get directory service for the ehr_id
2. The service should return the full structure of the complex directory for the EHR


### F.4. Alternative flow 3: get directory on non-existent EHR

**Preconditions:**

1. The server is empty

**Postconditions:**

None

**Flow:**

1. Invoke the get directory service for a random ehr_id
2. The service should return an error related with the non-existent EHR



## G. Get DIRECTORY at time Flows

### G.1. Main flow: get directory at time on empty EHR

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for the ehr_id and current time
2. The service should return an empty structure

> Implementation note: considering a REST API implementation, the result could be an error status instead of an empty structure.


### G.2. Alternative flow 1: get directory at time on empty EHR with empty time

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for the ehr_id and empty time
2. The service should return an empty structure

> Implementation note: considering a REST API implementation, the result could be an error status instead of an empty structure.


### G.3. Alternative flow 2: get directory at time on EHR with directory

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR has a directory with one version (use any of the valid datasets from the previous tests)

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for the ehr_id and current time
2. The service should return the current directory


### G.4. Alternative flow 3: get directory at time on EHR with directory with empty time

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR has a directory with one version (use any of the valid datasets from the previous tests)

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for the ehr_id and empty time
2. The service should return the current directory


### G.5. Alternative flow 4: get directory at time on EHR with directory with multiple versions in multiple times

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR has a directory with two versions (use any of the valid datasets from the previous tests, add small changes to differentiate the versions)

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for the ehr_id and a time before EHR.time_created
2. The service should return an empty structure
3. Invoke the get directory at time service for the ehr_id and a time after the first EHR.directory version was created and before the second EHR.directory version was created
4. The service should return the first version of EHR.drectory
5. Invoke the get directory at time service for the ehr_id and current time
6. The service should return the second version of EHR.directory


### G.6. Alternative flow 5: get directory at time on EHR with directory with multiple versions with empty time

**Preconditions:**

1. An EHR with ehr_id exists
2. The EHR has a directory with two versions (use any of the valid datasets from the previous tests, add small changes to differentiate the versions)

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for the ehr_id and empty time
2. The service should return the current latest directory


### G.7. Alternative flow 7: get directory at time on non existent EHR

**Preconditions:**

1. The server is empty

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at time service for a random ehr_id and current time
2. The service should return an error about the non existent EHR


---
WIP

---






## H. Update DIRECTORY

### H.1. Main flow: update directory from EHR with directory

**Preconditions:**

1. An EHR with ehr_id exists on the server
2. The EHR has a directory

**Postconditions:**

1. The EHR with ehr_id has an updated directory structure

**Flow:**

1. Invoke the update directory service for the ehr_id
   1. Use any of the valid paylaods described on the previous tests
2. The service should return a positive result related with the updated directory


### H.2. Alternative flow 1: update directory on empty EHR

**Preconditions:**

1. An EHR with ehr_id exists on the server
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the update directory service for the ehr_id
   1. Use any of the valid paylaods described on the previous tests
3. The service should return an error related to the non existent directory to update


### H.3. Alternative flow 2: update directory on non-existing EHR

**Preconditions:**

1. The server is empty, doesn't have any EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the update directory service for random ehr_id
   1. Any valid payload will suffice
2. The service should return an error related to the non existent ehr_id


## I. Delete DIRECTORY

### I.1. Main flow: delete directory on empty EHR

**Preconditions:**

1. An EHR with ehr_id exists on the server
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the delete directory service for the ehr_id
2. The service should return an error related to the non existent directory


### I.2. Alternative flow 1: delete directory from EHR with directory

**Preconditions:**

1. An EHR with ehr_id exists on the server
2. The EHR has a directory

**Postconditions:**

1. The EHR ehr_id doesn't have directory

> Note: the directory exists as a new deleted version (that is VERSION.lifecycle_state=deleted).

**Flow:**

1. Invoke the delete directory service for the ehr_id
2. The service should return a positive result related with the deleted directory


### I.3. Alternative flow 2: delete directory from non-existent EHR

**Preconditions:**

1. The server is empty, there are no EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the get directory service for a random ehr_id
2. The service should return an error related to the non existent EHR


## J. Has DIRECTORY version

### J.1. Main flow: has directory from empty EHR

**Preconditions:**

1. An EHR with known ehr_id exists on the server
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the has directory service for the ehr_id and a random version uid
2. The service should return false


### J.2. Alternative flow 1: has directory on EHR that has two versions of directory

**Preconditions:**

1. An EHR with known ehr_id exists on the server
2. The EHR has two directory versions

**Postconditions:**

None

**Flow:**

1. Invoke the has directory service for the ehr_id and the version_uid of the first version of directory
2. The service should return true
3. Invoke the has directory service for the ehr_id and the version_uid of the second version of directory
4. The service should return true


### J.3. Alternative flow 2: has directory on non-existent EHR

**Preconditions:**

1. The server doesn't have EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the has directory service for a random ehr_id and version uid
2. The service should return an error related with the non existence of the EHR


## K. Get DIRECTORY at version

### K.1. Main flow: get directory at version from empty EHR

**Preconditions:**

1. An EHR with known ehr_id exists on the server
2. The EHR doesn't have a directory

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at version service for the ehr_id and a random version uid
2. The service should return an error related to the non existence of the EHR directory version


### K.2. Alternative flow 1: get DIRECTORY at version from EHR that has two versions of directory

**Preconditions:**

1. An EHR with known ehr_id exists on the server
2. The EHR has two versions of directory

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at version service for the ehr_id and the version_uid of the first version of directory
2. The service should return the first version of the directory
3. Invoke the get directory at version service for the ehr_id and the version_uid of the second version of directory
4. The service should return the second version of the directory


### K.3. Alternative flow 2: get DIRECTORY at version from non existent EHR

**Preconditions:**

1. The server doesn't have EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the get directory at version service for a random ehr_id and version uid
2. The service should return an error related with the non existence of the EHR


## L. Get versioned DIRECTORY

### L.1. Main flow: get versioned directory from EHR with no directory

**Preconditions:**

1. An EHR with known ehr_id exists on the server

**Postconditions:**

None

**Flow:**

1. Invoke the get versioned directory service for the ehr_id
2. The service should return an error because the versioned directory doesn't exist

> Note: depending on the implementation, a valid result could also be returning an empty result instead of an error.


### L.2. Alternative flow 1: get versioned directory from existent EHR that has two versions of directory

**Preconditions:**

1. An EHR with known ehr_id exists on the server
2. The EHR has two versions of directory

**Postconditions:**

None

**Flow:**

1. Invoke the get versioned directory service for the ehr_id
2. The service should return the versioned folder and should reference the two exsinting versions


### L.3. Alternative flow 2: get versioned directory from non existent EHR

**Preconditions:**

1. The server doesn't have any EHRs

**Postconditions:**

None

**Flow:**

1. Invoke the get directory service for a random ehr_id
2. The service should return an error related with the non existence of the EHR


---

## Annex: proposal for FOLDER API

REF: https://wiki.vitagroup.ag/display/ETHERCIS/FOLDER+related+decisions+-+internal+implementation%2C+API+and+conformance

openEHR ticket: https://openehr.atlassian.net/browse/SPECPR-338?jql=status%20in%20(Analysis%2C%20Implementation%2C%20%22In%20Progress%22%2C%20%22In%20Review%22%2C%20Open%2C%20%22To%20Do%22)%20AND%20text%20~%20%22folder%22 

### Background

Current service model related for EHR.directory might lead to some complexities and issues for internal implementation and for the REST API. The goal is to discuss those issues and decide upon our internal implementation rules for FOLDER in general, and particularly for EHR.directory.

### Operations for the Service Model

**Current operations are:**

1. has_directory(ehr_id): Boolean
2. has_path(ehr_id, path): Boolean   // path from the root EHR.directory, also the idea of this path is that is defined by archetypes (this is another issue mentioned below)
3. create_directory(ehr_id, folder) //root directory
4. get_directory(ehr_id): FOLDER   // this might need to be VERSION<FOLDER>
5. get_directory_at_time(ehr_id, time): FOLDER   // this might also be VERSION<FOLDER>
6. update_directory(ehr_id, folder)   // folder is the full EHR.directory modified
7. delete_directory(ehr_id)
8. has_directory_version(ehr_id, version_uid): Boolean
9. get _directory_at_version(ehr_id, version_uid): FOLDER   // this might also be VERSION<FOLDER>
10. get_versioned_directory(ehr_id): VERSIONED_FOLDER


**Issues:**

1. To update, the client has to get the full EHR.directory structure, do changes on the client side (that means the management happens on the client), then the update needs to commit the whole structure with the modifications, this adds a lot of complexity on the client side and might not be the most natural way of managing an EHR.directory. (Luis: agree. FOLDERS in my view should be self standing structures and the system should allow for updating only one of them (e.g. change its items or details) as long as the edition of the FOLDER does not introduce inconsistencies in other FOLDERs.)
2. The has_path operation uses a path that should be defined by an archetype (mentioned by Thomas on the SEC Slack), my interpretation was those where instance paths considering the EHR.directory tree structure, which makes sense since it is impractical to have the whole EHR.directory structure defined by archetypes, and even some of those FOLDERs will be created in an ad-hoc way (IMO most will be created this way and using a generic archetype for definition, this is also the approach of Code24 which has been using folders for 6 years). Also paths are name-based, which makes them language dependent and creates the need for a constraint to have sibling FOLDERs with unique names. (Luis: the uid inherited from LOCATABLE (now optional 0..1) should be mandatory in our implementation for implementation reasons (1..1). At the moment is is the Primary Key in the Database, thus it is mandatory and unique).
3. Also related to paths, the current spec shows name-based paths to reference internal FOLDERs, but to reference to items in a FOLDER, the path uses numeric indexes, which seems inconsistent. One possibility is to use the item name on the path, the issue that creates is the items are really VERSIONED_OBJECT, which doesn't have a name, but the VERSIONED_OBJECT.latest_version() which is VERSION<T> has a name if T is LOCATABLE, so FOLDER.item[i].latest_version().data.name could be used in the path, but again, that creates another couple of issues: a. the name is not really form the item but from the contained data, and b. since the data could be updated, the name could change, changing the path. So the name-based path IMO is not really useful for any use case.
4. That last part makes me think of the name-based paths for FOLDERs, since FOLDER.name could also change, since FOLDERs could be created, renamed, deleted, etc. so the paths that were valid at one point could be invalid later. And one idea of these paths was to use them also for AQL, but IMO is almost impossible to get something very detailed from AQL using paths for FOLDERs, since I think most FOLDERs will be created ad-hoc and might not have a full structure defined by archetypes, only the basic structure, and maybe the new FOLDER.details structure, which could be archetyped but also could be used in an ad-hoc way.
5. Not issues from the operations but from the model: a. a FOLDER could have more than one parent, b. a FOLDER could have an ancestor as subfolder. These break the tree structure and openEHR needs to add some invariants to prevent this on the model.
6. We should clearly commit to implement FOLDERs directory as trees in the computational sense. The aim of this is to guarantee some performance issues (approx. O(lgn) when rearranged optimally) and avoid possible cycles that may derive from graph-like directories. This is in contradiction with some implementations that allow to virtually define graphs using the LINK class.
7. The operation "has_directory(ehr_id): Boolean" makes sense in EHRs, however for phenotyping in clinical research it may be actually the opposite. For example, a clinical study on back pain surgery may have a folder containing many EHRs rather than the other way around.


**Proposals for operations:**

1. has_directory(ehr_id): Boolean      // MAINTAIN
2. has_folder(ehr_id, folder_uid): Boolean   // NEW, uses uid not path
3. has_path(ehr_id, path): Boolean    // MAINTAIN - 1. spec needs to explicitly state "path" is an archetype path, not an instance path, 2. add an example with archetype paths to show how this operation will work, I think looks good on paper but it can be difficult to implement
4. create_directory(ehr_id, folder)     // MAINTAIN - discuss about the EHR and support self-standing FOLDERs without belonging to an EHR.
5. get_directory(ehr_id): FOLDER      // MAINTAIN
6. get_directory_at_time(ehr_id, time): FOLDER     // MAINTAIN
7. get_folder(ehr_id, folder_uid): FOLDER     // NEW, like cd + ls commands (this is optional since the information will be included in the result of get_directory). This will return the latest version of the directory provided that folder is not versioned.
8. create_folder(ehr_id, parent_folder_uid, new_folder)    // NEW, like mkdir command, if no parent_folder_uid is provided, the new_folder will be created under the EHR.directory
9. update_folder(ehr_id, updated_folder)    // NEW, allows to modify an individual FOLDER and what it contains, including name, details, folders and items. The updated_folder contains it's uid so there is no need for an extra parameter. If subfolders are deleted in the updated folder, they are deleted in the directory as well in EHRbase.
10. remove_folder(ehr_id, folder_uid)    // NEW, like rmdir -r (removes also subfolders and items)
11. add_item(ehr_id, folder_uid, versioned_object_uid)    // NEW, like the touch command, adds the item to the FOLDER.items via OBJECT_REF (TODO: verify OBJECT_REF needs namespace and type values but I think those could be set to default values set on the server config so we might not need to add extra parameters for those)
12. remove_item(ehr_id, folder_uid, versioned_object_uid)   // NEW, like the rm command, removes the versioned object reference from the FOLDER.items
13. delete_directory(ehr_id)    // MAINTAIN, but is contained in remove_folder when it is invoked with the EHR.directory.uid as folder_uid value
14. has_directory_version(ehr_id, version_uid): Boolean    // MAINTAIN
15. get _directory_at_version(ehr_id, version_uid): FOLDER   // MAINTAIN
16. get_versioned_directory(ehr_id): VERSIONED_FOLDER   // MAINTAIN


**Notes:**

1. Referencing FOLDERs by uid requires that the FOLDER.uid is set for all FOLDERs by the server. In the RM the uid is optional, so this could be an implementation constraint but still "spec valid".
2. The added operations seem to be a more natural way of managing with FOLDERs and their items like a user could do on a Linux Terminal and avoids the extra complexity of managing the whole EHR.directory on the client side for creating new FOLDERs and adding new references to items, also for deleting stuff, instead of having one big operation, we could map one action from a user to one operation on the Service Model. Still the create_folder() operation could receive a full FOLDER structure with subfolders and references to items, or just the basic data like name and details, and then it could be modified using the other operations, or the same create_folder() to add subfolders to it. That also adds more flexibility for client-side implementation.
3. About versioning, from the spec, the only versionable FOLDER is the EHR.directory, no internal FOLDERs could be versioned. Considering the new operations, each creation, update and removal of FOLDERs and items, would generate a new version of the containing EHR.directory, so this is an implementation consideration. Either way this should be done with the current operations in the SM spec, this is just to note that individual FOLDERs shouldn't be versioned (Code24 is versioning individual FOLDERs and they might propose a change request to make that valid in the spec, but won't be any time soon).
4. Using the parent_folder_id to create new FOLDERs prevent the generation of non-tree structures, since a. FOLDER.uid should always be assigned by the server and 2. only children to a given parent could be created.
5. TODO: we still need to discuss AQL requirements for FOLDERs and what will be needed to support those (from archetype modeling to internal implementation).


