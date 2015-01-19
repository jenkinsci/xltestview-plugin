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
package com.xebialabs.gradle.plugins.release;

import groovy.lang.Closure;

import org.gradle.api.Task;

import com.xebialabs.gradle.plugins.git.CheckTagAbsentTask;

class ReleaseType {
    final String name

    def tag
    Task preBuildTask
    Task postBuildTask
    Task checkTagAbsentTask
    Task tagBuildTask

    ReleaseType(String name) {
        this.name = name
    }
    def tag(Closure tag) {
        // TODO can we check that the closure uses { -> x } ?
        this.tag = tag
        tagBuildTask.tag = tag
        checkTagAbsentTask.tag = tag
    }
    def preBuild(Closure cfg) {
        preBuildTask.configure cfg
    }
    def postBuild(Closure cfg) {
        postBuildTask.configure cfg
    }
}
