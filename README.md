# Information

* Plugin page: [https://wiki.jenkins-ci.org/display/JENKINS/XL+TestView+Plugin](https://wiki.jenkins-ci.org/display/JENKINS/XL+TestView+Plugin)
* XL TestView documentation: [https://docs.xebialabs.com/xl-testview/](https://docs.xebialabs.com/xl-testview/)
* CI: [https://jenkins.ci.cloudbees.com/job/plugins/job/xltestview-plugin/](https://jenkins.ci.cloudbees.com/job/plugins/job/xltestview-plugin/)

# Building

The Jenkins plugin build is powered by the [gradle-jpi-plugin](https://github.com/jenkinsci/gradle-jpi-plugin) (see its [documentation](https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin)).

There are following targets defined:

Builds **.hpi** file

    gradle jpi

Run development server:

    gradle server

## Debugging

Debugging is configured via the GRADLE_OPTIONS environment variable.

    GRADLE_OPTS="${GRADLE_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006" ./gradlew clean server


