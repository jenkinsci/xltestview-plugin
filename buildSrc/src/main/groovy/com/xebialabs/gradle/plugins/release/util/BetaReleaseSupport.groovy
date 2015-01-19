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

/** Supported flow:
 *
 * 1.0.0-SNAPSHOT
 * 1.0.0-beta-1
 * 1.0.0-SNAPSHOT
 * 1.0.0-beta-2
 * 1.0.0-SNAPSHOT
 * 1.0.0
 *
 * And not:
 * 1.0.0-SNAPSHOT
 * 1.0.0-beta-1-SNAPSHOT
 * 1.0.0-beta-1
 * 1.0.0-beta-2-SNAPSHOT
 */
class BetaReleaseSupport {

    static def tag(Project project) {
        "${project.rootProject.name}-${unsnapshotLenient(project.versionInfo.version)}"
    }

    static String makeReleaseVersion(Properties versionInfo) {
        "${unsnapshotLenient(versionInfo.version)}-beta-${versionInfo.beta}".toString()
    }

    static def preBuildConfiguration(Project project, Task task) {
        task.configure {
            message = "${makeReleaseVersion(project.versionInfo) }"
            file = project.relativePath(project.versionPropertiesFile)
            doFirst {
                String v = makeReleaseVersion(project.versionInfo)
                println "prebuild version becomes $v"
                project.versionInfo.version = v
                writeVersion(project.versionPropertiesFile, project.versionInfo)
            }
        }
    }

    static def unbeta(def version) {
      def idx = version.indexOf('-beta-')
      if(idx == -1) {
        throw new IllegalArgumentException("version is not a beta: ${version}")
      }
      version.substring(0, idx)
    }

    static def postBuildConfiguration(Project project, Task task) {
        task.configure {
            message = "${project.rootProject.name}-${ -> project.versionInfo.version} (beta=${ -> project.versionInfo.beta})"
            file = project.relativePath(project.versionPropertiesFile)
            doFirst {
                logger.info("postBuild start version ${project.versionInfo}")
                project.versionInfo.version = "${unbeta(project.versionInfo.version)}-SNAPSHOT".toString()
                project.versionInfo.beta++
                println "postBuild commit version ${project.versionInfo}"
                writeVersion(project.versionPropertiesFile, project.versionInfo)
            }
        }
    }
}
