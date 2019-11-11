# EHRBASE Integration Tests with Robot Framework

## How to run test locally

> PREREQUISITES
>
> - Docker, Python 3 with Pip are installed
> - RF dependencies are installed (`cd tests/`, `pip install -r requirements.txt`)
> - No DB / no server running!
> - ports `8080` and `5432` not used by any other application!


## Execution of tests under Linux, Mac and Windows
Local execution of all integration tests takes about 30 minutes. To avoid waiting for all results you can specify exactly which test-suite you want to execute. There are six test-suites from which you can choose by passing the proper TAG to `robot` command: 

TEST SUITE | SUPER TAG | SUB TAGs | EXAMPLE
-- | -- | -- | --
COMPOSITION_TESTS   | composition   | json, json1, <br>json2, xml, <br>xml1, xml2 | `robot -i composition`
CONTRIBUTION_TESTS  | contribution  | 0     | 21
DIRECTORY_TESTS     | directory     | 18    | 19
KNOWLEDGE_TESTS     | knowledge     | 24    | 4
QUERY_SERVICE_TESTS | AQL           | 0     | 2
EHR_SERVICE_TESTS   | ehr_service   | 9     | 10

- COMPOSITION_TESTS (TAG: composition)
- CONTRIBUTION_TESTS (TAG: )
- DIRECTORY_TESTS (TAG: )
- EHR_SERVICE_TESTS (TAG: )
- KNOWLEDGE_TESTS (TAG)
- QUERY_SERVICE_TESTS




```bash
. run_local_tests.sh
```

OR - Use robot [command line options](http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#using-command-line-options) to fine tune execution:

```bash
robot -d results --noncritical not-ready -L TRACE robot/
```

> NOTE: Never change the target `robot/` !!!

### Execution of tests under Windows

```shell
robot -d results --noncritical not-ready -L TRACE robot/
```


## ERRORS and WARNINGS

You will see `[WARN]` and `[ERROR]` in console output and in log.html
`[ERROR]` --> take a closer look, probably important
`[WARN]`  --> minor issues like wrong status code

> NOTE: `[WARN]	Response body content is not JSON. Content-Type is: text/html`
>
> You will see this warning very often. IGNORE it! It's caused by a RF library.

## Fine control test execution by including/excluding tags

It is possible to filter the tests to be executed by [using tags](http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#by-tag-names). Tags are case insensitive.

- `robot -i create_ehr robot/` = `robot --include create_ehr robot/`
- `robot -i CREATE_EHR robot/`
- `robot -i create_ehrANDopt14 robot/`
- `robot --exclude create_ehr robot/` = `robot -e create_ehr robot/`
- `robot -i create_ehr -i compsotion robot/`


The `__init__.robot` inside each
super-test-suite folder (all UPERCASE) sets the tags for all it's sub-suites
with the `Force Tags` keyword. Additionally sub-suites can set further tags with
the same keyword, too. Lastly each test case can be tagged manually with a [Tags]
setting.

> Available TAGS (TODO: update after pull-requests are merged)
> EHR_SERVICE: create_ehr, has_ehr, composition ...
> KNOWLEDGE: OPT14, OPT20, ...
> QUERY: ...


## Auto-generated test report summary and detailed log

After each test run Robot creates a report.html (summary) and a log.html
(details) in results folder. The files are overwritten after each run by default.
If you want to prevent this behavior you can [time-stamp](http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#timestamping-output-files) the output files.
