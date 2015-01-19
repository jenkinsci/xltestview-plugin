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

import org.gradle.api.Project;

class VersionUtil {

    static def setVersion(Project rootProject) {
        rootProject.ext {
            versionPropertiesFile = new File(rootProject.rootDir, "version.properties")
            versionInfo = new Properties()
            versionPropertiesFile.withInputStream {
                versionInfo.load(it)
            }
        }
        rootProject.allprojects {
            it.version = versionInfo.version
        }
    }

    static def writeVersion(def versionPropertiesFile, def versionInfo) {
        versionPropertiesFile.withWriter { w ->
            versionInfo.store w, 'Release version'
        }
    }

    static def unsnapshot(def version) {
        if(version.indexOf('-SNAPSHOT') == -1) {
            throw new IllegalArgumentException("version is not a snapshot: ${version}")
        }
        version = version.substring(0, version.indexOf('-SNAPSHOT'))
    }

    static def unsnapshotLenient(version) {
        if(version.indexOf('-SNAPSHOT') == -1) {
            version
        } else {
            version = version.substring(0, version.indexOf('-SNAPSHOT'))
        }
    }

    static def incrPatch(def version) {
        if(version.indexOf('-SNAPSHOT') != -1) {
            throw new IllegalArgumentException("cannot bump version on snapshot")
        }
        def vps = version.split '\\.'
        def minor = vps[2] as int
        minor++
        vps[2] = minor as String
        vps.join('.')
    }
}
