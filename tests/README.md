# EHRbase Integration Tests with Robot Framework

## How to run test locally

> PREREQUISITES
>
> - Docker, Python 3 with Pip are installed
> - RF dependencies are installed (`cd tests/`, `pip install -r requirements.txt`)
> - **No DB / no server running!**
> - ports `8080` and `5432` not used by any other application! (`netstat -tulpn`)


## Execution of tests under Linux, Mac and Windows
In general tests are executed by calling the **`robot`** command pointing it to the folder wich contains the tests, i.e.:
```
# this will run all test-cases from robot/ folder 
# when you call it from inside project_root/tests/

# Linux & Mac OS
robot robot/
robot ./robot/

# Windows
robot .\robot\


# QUICK COPY/PASTE EXAMPLES TO RUN ONLY A SPECIFIC TEST-SUITE

robot -i composition    -d results --noncritical not-ready -L TRACE robot/COMPOSITION_TESTS/
robot -i contribution   -d results --noncritical not-ready -L TRACE robot/CONTRIBUTION_TESTS/
robot -i directory      -d results --noncritical not-ready -L TRACE robot/DIRECTORY_TESTS/
robot -i ehr_service    -d results --noncritical not-ready -L TRACE robot/EHR_SERVICE_TESTS/
robot -i knowledge      -d results --noncritical not-ready -L TRACE robot/KNOWLEDGE_TESTS/
robot -i aql            -d results --noncritical not-ready -L TRACE robot/QUERY_SERVICE_TESTS/
```

Execution of **all** integration tests takes **about 30 minutes** (on a fast dev machine). To avoid waiting for all results you can specify exactly which test-suite or even which subset of it you want to execute. There are six test-suites to choose from by passing proper TAG to `robot` command via the `--include` (or short `-i`) option: 


TEST SUITE | SUPER TAG | SUB TAG(s) | EXAMPLE(s)
:----------|:----------|:-----------|:----------
COMPOSITION_TESTS   | composition   | json, json1, json2, <br> xml, xml1, xml2 | `robot --include composition` <br> `robot -i composition` <br> `robot -i compositionANDjson`
CONTRIBUTION_TESTS  | contribution  | commit_contribution, <br> list_contributions, <br> has_contribution, <br> get_contribution | `robot -i contribution`
DIRECTORY_TESTS     | directory     | create_directory, <br> update_directory, <br> get_directory, <br> delete_directory, <br> get_directory_@time, <br> ...   | `robot -i composition` <br> `robot -i create_directoryORupdate_directory`
EHR_SERVICE_TESTS   | ehr_service   | create_ehr, update_ehr, <br> has_ehr, get_ehr, <br>  ehr_status | `robot -i ehr_service`
KNOWLEDGE_TESTS     | knowledge     | opt14 | `robot -i knowledge`
QUERY_SERVICE_TESTS | aql           | adhoc-query, <br> stored-query, <br> register-query, <br> list-query   | `robot -i adhoc-query`



The **SUPER TAG** is meant to reference *all* tests from related test-suite. The **SUB TAGs** can be used (in combination with a SUPER TAG) to further narrow down which tests to include into execution. As you can see from the examples in the table above it is possible to combine TAGs with `AND` and `OR` operators. Tags themself are case insensitive but the operators have to be upper case. In addition to `--include` or `-i` option there is also an `--exclude` / `-e` option. It is possible to combine `-i` and `-e` in one call, i.e.
```bash
robot -i ehr_serviceANDget_ehr -e future robot/EHR_SERVICE_TESTS/
```
[Using TAGs to include/exclude tests] from execution is very well documented in [Robot Framework's User Guide].

There is also a prepared [shell script] which you can use to run **all** available tests at once. You can also use it as a reference to see which [command line options] are available to the `robot` command. Check examples below to see how to execute that script on your OS: 

```bash
# Linux
. run_local_tests.sh

# Mac OS
./run_local_tests.hs

# Windows
robot -d results --noncritical not-ready -L TRACE robot/

(No script there yet. TODO: create a proper .bat file)

```

## Example content of run_local_tests.sh
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



## ERRORS and WARNINGS

You will see `[WARN]` and `[ERROR]` in console output and in log.html

`[ERROR]` --> take a closer look, probably important

`[WARN]`  --> minor issues like wrong status code or keyword deprecation warning.


> NOTE: `[WARN]	Response body content is not JSON. Content-Type is: text/html`
>
> You will see this warning very often. IGNORE it! It's caused by a RF library.



## Auto-generated test report summary and detailed log

After each test run Robot creates a report.html (summary) and a log.html
(details) in results folder. The files are overwritten after each run by default.
If you want to keep history of your test runs you can [time-stamp](http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#timestamping-output-files) the output files.




[shell script]: ./run_local_tests.sh

[Robot Framework's User Guide]: http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html

[command line options]: http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#using-command-line-options

[Using TAGs to include/exclude tests]: http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#by-tag-names
