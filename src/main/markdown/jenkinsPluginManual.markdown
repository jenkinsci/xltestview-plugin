## Preface ##

This document describes the functionality provided by **XL Test Jenkins Plugin**.

## Overview ##

The **XL Test Jenkins Plugin** provides a post-build action that allows you to send test results to XL Test.

### Features ###

* Create a release from a template
	* Select the template to create the release from
	* Enter variables as defined within the template
* Start a release

### Requirements ###

* **Jenkins**: Jenkins **LTS** version {{supportedLtsVersion}} or higher.
* **XL Test**: Grab a copy from http://xebialabs.com/products/xl-test.

## Configuration ##

There are 2 places to configure the **XL Release Jenkins Plugin**: global Jenkins configuration and job configuration.

### Plugin configuration ###

At *Manage Jenkins* -> *Configure System* you can specify the XL Test server URL and one or more sets of credentials. Different sets can be used for different jobs.

### Job configuration ###

In the Job Configuration page, choose *Post-build Actions* -> *Add post-build action* -> *Send test results to XL Test*. Specify the tool used to perform the testing and a file pattern -- this will save bandwidth as the results files are sent to XL Test for analysis.

## Release notes ##