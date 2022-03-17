

TODO @WLAD clean this up and move to dev docs

# HOW TO GET EHRBASE CODE COVERAGE FROM ALL TESTS

> Prerequisite: jacoco-0.8.6 installed (agent, cli)
  ```
  wget https://repo1.maven.org/maven2/org/jacoco/jacoco/0.8.6/jacoco-0.8.6.zip
  unzip -uo jacoco-0.8.6.zip -d ~/JACOCO
  ```

1. Run EHRbase with "on-the-fly" Jacoco instrumentation
java -javaagent:/home/wlad/JACOCO/lib/jacocoagent.jar=output=tcpserver,address=127.0.0.1 -jar application/target/application-0.14.0.jar

2. Execute (external) tests (i.e. SKD, Robot, etc.)
   - run SKD integration tests OR
   - run Robot integration tests OR
   - run manual tests

3. Dump Jacoco execution information to a (properly named) file

```
java -jar ~/JACOCO/lib/jacococli.jar dump --destfile=./test-coverage/target/jacoco-sdk-it-coverage.exec
java -jar ~/JACOCO/lib/jacococli.jar dump --destfile=./test-coverage/target/jacoco-robot-it-coverage.exec
java -jar ~/JACOCO/lib/jacococli.jar dump --destfile=./test-coverage/target/jacoco-manual-test-coverage.exec
...

```

4. Generate human readable reports from jacoco.exec files
Create report for specifi tests only:

```
java -jar ~/JACOCO/lib/jacococli.jar report test-coverage/target/jacoco-sdk-it-coverage.exec \
     --classfiles api/target/classes/ \
     --classfiles application/target/classes/ \
     --classfiles base/target/classes/ \
     --classfiles jooq-pq/target/classes/ \
     --classfiles rest-ehr-scape/target/classes/ \
     --classfiles rest-openehr/target/classes/ \
     --classfiles service/target/classes/ \
     --sourcefiles api/src/main/java/ \
     --sourcefiles application/src/main/java/ \
     --sourcefiles base/src/main/java/ \
     --sourcefiles jooq-pq/src/main/java/ \
     --sourcefiles rest-ehr-scape/src/main/java/ \
     --sourcefiles rest-openehr/src/main/java/ \
     --sourcefiles service/src/main/java/ \
     --html test-coverage/sdk-it-coverage-report \
     --name "EHRbase Code Coverage w/ SDK Integration Tests"
```

OR

Merge multiple .exec files first, then generate an overall coverage report form it:

```
java -jar ~/JACOCO/lib/jacococli.jar merge ./*/target/jacoco*.exec \
     --destfile test-coverage/jacoco-all-tests-coverage.exec

java -jar ~/JACOCO/lib/jacococli.jar report test-coverage/jacoco-all-tests-coverage.exec \
     --classfiles api/target/classes/ \
     --classfiles application/target/classes/ \
     --classfiles base/target/classes/ \
     --classfiles jooq-pq/target/classes/ \
     --classfiles rest-ehr-scape/target/classes/ \
     --classfiles rest-openehr/target/classes/ \
     --classfiles service/target/classes/ \
     --sourcefiles api/src/main/java/ \
     --sourcefiles application/src/main/java/ \
     --sourcefiles base/src/main/java/ \
     --sourcefiles jooq-pq/src/main/java/ \
     --sourcefiles rest-ehr-scape/src/main/java/ \
     --sourcefiles rest-openehr/src/main/java/ \
     --sourcefiles service/src/main/java/ \
     --html test-coverage/overall-coverage-report \
     --name "EHRbase Code Coverage w/ All Tests (Unit, SDK, Robot)"
```

5. Configure sonarcloud analysis to use the Overall Coverage report
TODO





CLASS FILES
===========

api/target/classes/
application/target/classes/
base/target/classes/
jooq-pq/target/classes/
rest-ehr-scape/target/classes/
rest-openehr/target/classes/
service/target/classes/



SOURCE FILES
============

api/src/main/java/
application/src/main/java/
base/src/main/java/
jooq-pq/src/main/java/
rest-ehr-scape/src/main/java/
rest-openehr/src/main/java/
service/src/main/java/



javaagent:lib/jacocoagent.jar=destfile=/execs/jacoco.exec,output=file,append=true
java -javaagent:/home/wlad/JACOCO/lib/jacocoagent.jar=output=tcpserver,address=127.0.0.1 -jar application/target/application-0.14.0.jar
