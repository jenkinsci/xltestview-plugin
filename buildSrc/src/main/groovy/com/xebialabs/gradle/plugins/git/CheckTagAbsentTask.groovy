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
import org.eclipse.jgit.lib.Ref
import org.gradle.api.GradleException

class CheckTagAbsentTask extends GitTask {
    def tag

    @Override
    void action(Git git) {
        logger.info "Checking for absence of tag ${tag}"
        List<Ref> call = git.tagList().call();
        for (Ref ref : call) {
            logger.debug("Checking ${ref.name} == ${tag}")
            if(ref.name == "refs/tags/${tag}") {
                // using gstring interpolation so a closure will work
                throw new GradleException("Tag '${tag}' already exists. Aborting.")
            }
        }
    }
}
