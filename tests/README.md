# EHRbase Integration Tests with Robot Framework <!-- omit in toc -->

- [Prerequisites](#prerequisites)
- [Test Environment & SUT](#test-environment--sut)
- [Local Execution (under Linux, Mac & Windows)](#local-execution-under-linux-mac--windows)
    - [With Robot Command](#with-robot-command)
    - [With Shell Script](#with-shell-script)
    - [Example Content Of Shell Script (run_local_tests.sh)](#example-content-of-shell-script-runlocaltestssh)
    - [Manually Controlled SUT](#manually-controlled-sut)
- [Execution Control - Test Suites & Tags](#execution-control---test-suites--tags)
- [Remote Execution](#remote-execution)
- [Execution In CI/CD Pipeline](#execution-in-cicd-pipeline)
- [Errors And Warnings](#errors-and-warnings)
- [Auto-Generated Test Report Summary And Detailed Log](#auto-generated-test-report-summary-and-detailed-log)

---

## Prerequisites

1) Docker, Java 11 & Maven, Python 3 & Pip are installed
2) Robot Framework & dependencies are installed (`pip install -r requirements.txt`)
3) Build artefacts created (`mvn package` --> application/target/application-x.xx.x.jar)
3) ⚠️ **No DB / no server running!**
4) ⚠️ ports `8080` and `5432` not used by any other application! (`netstat -tulpn`)



## Test Environment & SUT

The test environment of this project consists of three main parts
1) EHRbase openehr server (application-*.jar)
2) PostgreSQL database
3) OS with Docker, Java runtime, Python runtime, Robot Framework

Let's refer to the first two parts as the SUT (system under test). By default Robot Framework (RF) takes control of the SUT. That means to execute the tests locally all you have to do is to ensure your host machine meets required [prerequisites](#prerequisites). RF will take care of properly starting up, restarting and shutting down SUT as it is required for test execution. There is an option to hand over control of SUT to you, though - described in section [Manually Controlled SUT](#manually-controlled-sut).



## Local Execution (under Linux, Mac & Windows)
In general tests are executed by 1) cd into tests/ folder and 2) call the **`robot`** command with the folder wich contains the test suites as argument. Alternatively you can use prepared shell script inside this folder.


#### With Robot Command
The following examples will run all test-cases inside robot/ folder

```
# 1) from project's root
cd tests/

# 2) call robot command
robot robot/     # Linux
robot ./robot/   # Mac OS
robot .\robot\   # Windoofs
```

Everything between `robot` command and the last argument are commandline option to fine control test execution and the processing of test results. Examples:

```
# QUICK COPY/PASTE EXAMPLES TO RUN ONLY A SPECIFIC TEST-SUITE

robot -i composition    -d results --noncritical not-ready -L TRACE robot/COMPOSITION_TESTS/
robot -i contribution   -d results --noncritical not-ready -L TRACE robot/CONTRIBUTION_TESTS/
robot -i directory      -d results --noncritical not-ready -L TRACE robot/DIRECTORY_TESTS/
robot -i ehr_service    -d results --noncritical not-ready -L TRACE robot/EHR_SERVICE_TESTS/
robot -i ehr_status     -d results --noncritical not-ready -L TRACE robot/EHR_STATUS_TESTS/
robot -i knowledge      -d results --noncritical not-ready -L TRACE robot/KNOWLEDGE_TESTS/
robot -i aql            -d results --noncritical not-ready -L TRACE robot/QUERY_SERVICE_TESTS/
```


#### With Shell Script
Use [shell script] to run **all** available tests at once or use it as a reference to see which [command line options] are available to the `robot` command. Examples below demonstrate it's usage: 

```bash
# Linux
. run_local_tests.sh

# Mac OS
./run_local_tests.hs

# Windows
robot -d results --noncritical not-ready -L TRACE robot/

(No script there yet. TODO: create a proper .bat file)

```


#### Example Content Of Shell Script (run_local_tests.sh)
```bash
robot --include get_contribution \
      --exclude TODO -e future -e obsolete -e libtest \
      --loglevel TRACE \
      --noncritical not-ready \
      --flattenkeywords for \
      --flattenkeywords foritem \
      --flattenkeywords name:_resources.* \
      --outputdir results \
      --name CONTRIBUTION \
      robot/CONTRIBUTION_TESTS/
```


#### Manually Controlled SUT

In case you don't want Robot to start up and shut down server and database for you - i.e. during local development iterations - there is a command line option (`-v nodocker`) to turn this off. **This option should be used with some precaution, though!**

> ⚠️ 
>
> Test Suite Setups and Teardowns will NOT be orchestrated by Robot any more making it impossible to run ALL tests at once (i.e. with `robot robot/`) - test will simply impact each other. You will have to pass at least a test suite folder as argument or limit test selection by using tags to avoid this (see section below). More over
> - start the server with cache DISABLED (`--cache.enabled=false`)
> - ensure server applies to Robot's DEV configuration
> - restart server and rollback/reset database properly
> - when in doubt compare with results in CI pipeline
> - YOU HAVE BEEN WARNED!
>
> ⚠️

Usage examples:
```
robot --variable nodocker:true robot/TEST_SUITE_FOLDER

# short variant
robot -v nodocker robot/TEST_SUITE_FOLDER
robot -v nodocker -i get_ehr robot/EHR_SERVICE_TESTS
```

Robot will print proper warning in console if it can't connect to server or database:
```
[ WARN ] //////////////////////////////////////////////////////////
[ WARN ] //                                                     ///
[ WARN ] // YOU HAVE CHOSEN TO START YOUR OWN TEST ENVIRONMENT! ///
[ WARN ] // BUT IT IS NOT AVAILABLE OR IS NOT SET UP PROPERLY!  ///
[ WARN ] //                                                     ///
[ WARN ] //////////////////////////////////////////////////////////
[ WARN ] 
[ WARN ] [ check "Manually Controlled SUT" in test README ]
[ WARN ] [ https://github.com/ehrbase/ehrbase/blob/develop/tests/README.md#manually-sut ]
[ WARN ] 
[ ERROR ] ABORTING EXECUTION DUE TO TEST ENVIRONMENT ISSUES:
[ ERROR ] Could not connect to server!
[ ERROR ] Could not connect to database!
```



## Execution Control - Test Suites & Tags
Execution of **all** integration tests takes **about 30 minutes** (on a fast dev machine). To avoid waiting for all results you can specify exactly which test-suite or even which subset of it you want to execute. There are seven test-suites to choose from by passing proper TAG to `robot` command via the `--include` (or short `-i`) option: 


TEST SUITE | SUPER TAG | SUB TAG(s) | EXAMPLE(s)
:----------|:----------|:-----------|:----------
COMPOSITION_TESTS   | composition   | json, json1, json2, <br> xml, xml1, xml2 | `robot --include composition` <br> `robot -i composition` <br> `robot -i compositionANDjson`
CONTRIBUTION_TESTS  | contribution  | commit_contribution, <br> list_contributions, <br> has_contribution, <br> get_contribution | `robot -i contribution`
DIRECTORY_TESTS     | directory     | create_directory, <br> update_directory, <br> get_directory, <br> delete_directory, <br> get_directory_@time, <br> ...   | `robot -i composition` <br> `robot -i create_directoryORupdate_directory`
EHR_SERVICE_TESTS   | ehr_service   | create_ehr, update_ehr, <br> has_ehr, get_ehr, <br>  ehr_status | `robot -i ehr_service`
EHR_STATUS_TESTS   | ehr_status   | get_ehr_status, <br> set_ehr_status, <br>  set_queryable, <br> clear_queryable, <br> set_modifiable, <br> clear_modifiable | `robot -i ehr_status`
KNOWLEDGE_TESTS     | knowledge     | opt14 | `robot -i knowledge`
QUERY_SERVICE_TESTS | aql           | adhoc-query, <br> stored-query, <br> register-query, <br> list-query   | `robot -i adhoc-query`



The **SUPER TAG** is meant to reference *all* tests from related test-suite. The **SUB TAGs** can be used (in combination with a SUPER TAG) to further narrow down which tests to include into execution. As you can see from the examples in the table above it is possible to combine TAGs with `AND` and `OR` operators. Tags themself are case insensitive but **the operators have to be upper case**. In addition to `--include` or `-i` option there is also an `--exclude` / `-e` option. It is possible to combine `-i` and `-e` in one call. Example below would include all test from EHR_SERVICE_TESTS folder which have the `ehr_service` **and** `get_ehr` tags and would irgnore all test which have the `future` tag.
```bash
robot -i ehr_serviceANDget_ehr -e future robot/EHR_SERVICE_TESTS/
```
[Using TAGs to include/exclude tests] from execution is very well documented in [Robot Framework's User Guide].



## Remote Execution
> ⚠️: NOT READY - because controlling remote database via this settings is not implemented yet! 
> 
Add a configuration to a remote server in suite_settings.robot and point robot to it via command line switch:
```
robot --variable SUT:TARGET -d results robot/TEST_SUITE

# short
robot -v sut:target -d results robot/TEST_SUITE
```



## Execution In CI/CD Pipeline
Check out .circleci/config.yml in project root for an CircleCI example pipline whiche run Robot tests in parallel.



## Errors And Warnings

⚠️ You will see `[WARN]` and `[ERROR]` in console output and in log.html

`[ERROR]` --> take a closer look, probably important

`[WARN]`  --> minor issues like wrong status code or keyword deprecation warning.


> NOTE: `[WARN]	Response body content is not JSON. Content-Type is: text/html`
>
> You will see this warning very often. IGNORE it! It's caused by a RF library.



## Auto-Generated Test Report Summary And Detailed Log

After each test run Robot creates a report.html (summary) and a log.html
(details) in results folder. The files are overwritten after each run by default.
If you want to keep history of your test runs you can [time-stamp](http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#timestamping-output-files) the output files.




[shell script]: ./run_local_tests.sh

[Robot Framework's User Guide]: http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html

[command line options]: http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#using-command-line-options

[Using TAGs to include/exclude tests]: http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#by-tag-names
