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
package com.xebialabs.gradle.plugins.git;

import org.eclipse.jgit.api.Git
import org.gradle.api.GradleException

class CheckRepoCleanTask extends GitTask {
    @Override
    void action(Git git) {
        def status = git.status().call()
        if (!status.isClean()) {
            throw new GradleException("The repo ${git.repository} is not clean. Please commit all changes before doing a release.")
        }
    }
}
