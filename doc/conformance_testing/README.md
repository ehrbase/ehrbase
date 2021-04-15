# openEHR Conformance Testing Specification


| Author                                           | Version          | Comments                                         |
|:------------------------------------------------:|:----------------:|:------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 1.0 (2021-04-14) | First version integratig all tests suites into a single documenation.     |


# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
* [openEHR Conformance Testing Specification](#openehr-conformance-testing-specification)
* [Index](#index)
* [Introduction](#introduction)
* [Test Suites](#test-suites)
    * [Test Suite Components](#test-suite-components)
        * [Knowledge](#knowledge)
        * [EHR](#ehr)
        * [EHR/COMPOSITION](#ehrcomposition)
    * [Test Case Dependencies](#test-case-dependencies)
<!--te-->


# Introduction

This documentation contains different test suites focused on validating any implementation of the [openEHR Service Model](https://specifications.openehr.org/releases/SM/latest/openehr_platform.html) (SM). The SM is an abstract specification of services any openEHR implementation can provide. Currently the only concrete imlementable spec for SM is the [openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest), that is why the openEHR REST API is considered an ITS artifact (Implementable Technology Specification).


# Test Suites

The following diagram shows the different components, suites, and test cases inlcuded in this Conformance Testing Specification.

<div align="center">

![Test Suites](img/openEHR_Test_Suites.png "Test Suites")
</div>


## Test Suite Components

### Knowledge

This component includes the test suites for Operational Templates (OPT), Archetype Definition Language (ADL) and Terminology.

Since our focus was on OPT-based systems, the ADL component wasn't developed here.

The current status of an openEHR Terminology component is not completely defined by the current specifications, that component wasn't developed.

We'll leave both ADL and TERM test suites as placeholders here.

[Knowledge Test Suite](KNWOLEDGE.md)


### EHR

This component includes the test suites for EHR and EHR_STATUS, which test services related to EHR management.

[EHR Test Suite](EHR.md)


### EHR/COMPOSITION

This component includes the test suites COMPOSITION and CONTRIBUTION. Both are composed by test cases focused on EHR data management, including creation, modification and versioning.

[EHR/COMPOSITION Test Suite](EHR_COMPOSITION.md)


## Test Case Dependencies

The following diagram shows the dependencies between the test cases. This is important since it shows in which order the different services should be used. For instance, the data required to execute one service should be created first by using another service.

The arrows should be interpreted as "depends on".

<div align="center">

![Test Suites](img/openEHR_Test_Suite_Dependencies.png "Test Suites")
</div>