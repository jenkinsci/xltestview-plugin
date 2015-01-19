/*
 * Copyright 2015 XebiaLabs B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xebialabs.gradle.plugins.release.util;

import static com.xebialabs.gradle.plugins.release.util.VersionUtil.incrPatch
import static com.xebialabs.gradle.plugins.release.util.VersionUtil.unsnapshot
import static com.xebialabs.gradle.plugins.release.util.VersionUtil.unsnapshotLenient
import static com.xebialabs.gradle.plugins.release.util.VersionUtil.writeVersion

import org.gradle.api.Project
import org.gradle.api.Task

// make non static utils and have project in it ?
class ReleaseSupport {
    static def tag(Project project) {
        "${project.rootProject.name}-${unsnapshotLenient(project.versionInfo.version)}"
    }

    static def preBuildConfiguration(Project project, Task task) {
        task.configure {
            message = "${project.rootProject.name}-${ -> project.versionInfo.version}"
            file = project.relativePath(project.versionPropertiesFile)
            doFirst {
                project.versionInfo.version = unsnapshot(project.versionInfo.version)
                writeVersion(project.versionPropertiesFile, project.versionInfo)
            }
        }
    }
    static def postBuildConfiguration(Project project, Task task) {
        task.configure {
            message = "${project.rootProject.name}-${ -> project.versionInfo.version}"
            file = project.relativePath(project.versionPropertiesFile)
            doFirst {
                project.versionInfo.version = "${incrPatch(project.versionInfo.version)}-SNAPSHOT".toString()
                writeVersion(project.versionPropertiesFile, project.versionInfo)
            }
        }
    }
}
