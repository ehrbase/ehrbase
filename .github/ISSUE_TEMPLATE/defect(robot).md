---
name: Defect(Robot)
about: Report issues found by Robot tests
labels: bug
---



## Test Case/s To Reproduce Issue

```
# path to test case
tests/robot/CONTRIBUTION_TESTS/...
```

```
# robot command to execute related test case(s) in your terminal/console

# by test case name (wildcards possible)
robot -t "*Bug Case 01*" -d results -L TRACE robot/TEST_SUITE FOLDER

# by tag
robot -i failing -d results -L TRACE robot/TEST_SUITE_FOLDER

# Valid test suite folder names
COMPOSITION_TESTS
CONTRIBUTION_TESTS
DIRECTORY_TESTS
EHR_SERVICE_TESTS
EHR_STATUS_TESTS
KNOWLEDGE_TESTS
QUERY_SERVICE_TESTS
```

## Actual Result

foo


## Expected Result

bar
