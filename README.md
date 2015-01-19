
#Build#
The Jenkins plugin build is powered by the <a href="https://github.com/jenkinsci/gradle-jpi-plugin">gradle-jpi-plugin</a> (see its <a href="https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin">documentation</a>).

There are following targets defined:

Builds **.hpi** file

    gradle jpi

Run development server:

    gradle server

###Debugging###

Debuggins is configured with GRADLE_OPTIONS env variable.

    GRADLE_OPTS="${GRADLE_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006" ./gradlew clean server

##How to setup your environment to run Bol Test Specifications##

###Jenkins with plugin running###
- Get it from: https://github.com/xebialabs/xltest-plugin
- Then run: ./gradlew server
- You now have a jenkins server running on localhost:8080

###Xl-Test running with bol migration succeeded###
- Use the migration guide to do this. In the end you should have a new xl-test version running with Bol data.

###Make sure your jenkins job is configured properly###
- Create a Jenkins job on your Jenkins localhost:
	- Give it a (job)name without spaces.
	- It should be parameterized, add param XL_TEST_RUN_ID (no value)
	- VCS None
	- Build steps - "execute script" with:
	   ```echo "hello" > bla.txt```
    - After build:
    	- Credentials:
    		```xltest```
    	- Tool:
    		```xUnit```
    	- Pattern:
    		```**/*TEST-*.xml```

###Make sure you have a Jenkins Host in xl-test configured###
- Add Host, type ```"overthere.JenkinsHost"```
- Operating System (case sensitive!): ```UNIX```
- Address: ```http://localhost:8080```
- Jobname: ```<the name of your job in Jenkins>```

###Make sure the Regression test specification in xl-test is configured properly###
- Working Directory: ```.```
- Test Tool Name: ```FitNesse```
- Host: ```<select host from step 4>```
- Search Pattern: ```FitNesseRoot```
- Qualification: ```<choose one, ie the functional tests qualifier>```
- Import Test Results: ```false``` (unchecked)
- Work dir: ```full path to xl-test-bol project``` + ```/src``` (check out if you don't have it) - example: /```Users/shendriks/projects/xl-test-bol/src/```
- Project Dir: ```test/resources/```
- Repository Type: ```-```
- Repository Url: ```-```
- Command Line: ```.```
- Timeout: ```0```
- Branch: ```.```
- Suite Name: ```FrontPage```
- Browser: ```.```
- Environment: ```TEST2```


