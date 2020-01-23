(optional) Summary: What is this issue about?

## Steps To Reproduce This Issue
> path to test case file

tests/robot/CONTRIBUTION_TESTS/C.1_COMMIT_CONTRIBUTION/...



> robot command to execute related test case(s) - run it in your terminal/console

```shell
# by test case name (wildcards possible)
robot -t "*Bug Case 01*" -d results -L TRACE robot/TEST_SUITE FOLDER

# by tag
robot -i failing -d results -L TRACE robot/TEST_SUITE_FOLDER

# TEST SUITE FOLDERS
COMPOSITION_TESTS
CONTRIBUTION_TESTS
DIRECTORY_TESTS
EHR_SERVICE_TESTS
KNOWLEDGE_TESTS
QUERY_SERVICE_TESTS
```

## Actual Result


## Expected Result
