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
package com.xebialabs.gradle.plugins.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task;
import org.gradle.api.tasks.GradleBuild

import com.xebialabs.gradle.plugins.git.*

class ReleasePlugin implements Plugin<Project> {

    private static final String CHECK_REPO_CLEAN = "checkRepoClean"

    void apply(Project project) {
        def releaseTypes = project.container(ReleaseType)
        def ext = project.extensions.create("releasePlugin", ReleasePluginExtension, project, releaseTypes)

        releaseTypes.all { ReleaseType it ->
            project.logger.info "Setting up release type $it.name"

            def prefix = it.name
            def groupName = "Release"
            def releaseTaskName = "release"

            if (it.name != "release") {
                prefix = "release${it.name.capitalize()}"
                groupName = "Release ${it.name.capitalize()}"
                releaseTaskName = "release${it.name.capitalize()}"
            }

            def checkRepoClean = project.tasks.findByName(CHECK_REPO_CLEAN)
            if(checkRepoClean == null) {
                checkRepoClean = project.task(CHECK_REPO_CLEAN, type: CheckRepoCleanTask) {
                    description = "Check that the git repository is clean"
                    group = "Check"
                }
            }

            def tagAbsent = project.task("${prefix}CheckTagAbsent", type: CheckTagAbsentTask) {
                description = "Check that the release tag for ${releaseTaskName} is not present"
                group = "Check"
            }
            it.checkTagAbsentTask = tagAbsent

            def preBuild = project.task("${prefix}PreBuild", type: CommitTask, dependsOn: [checkRepoClean, tagAbsent]) {
                description = "Set and commit versions for ${releaseTaskName} build"
                group = groupName
            }
            // sub parts are not filled in yet so put the task on the extension
            // so we can configure it when it gets set
            it.preBuildTask = preBuild

            def subBuild = project.task("${prefix}SubBuild", type: GradleBuild, dependsOn: preBuild) {
                description = "Execute build with release versions for ${releaseTaskName}"
                group = groupName
                tasks = [ 'build' ]	// TODO configurable
                doLast { println "Sub Build for ${releaseTaskName} done." }
            }

            def tagBuild = project.task("${prefix}Tag", type: CreateTagTask, dependsOn: subBuild) {
                description = "Tag ${releaseTaskName} build"
                group = groupName
            }
            it.tagBuildTask = tagBuild

            def postBuild = project.task("${prefix}PostBuild", type: CommitTask, dependsOn: tagBuild) {
                description = "Prepare for next release after ${releaseTaskName} build"
                group = groupName
            }
            it.postBuildTask = postBuild

            project.task(releaseTaskName, type: PushTask, dependsOn: postBuildTask) {
                description = "Release"
                group = groupName
            }
        }
    }
}
