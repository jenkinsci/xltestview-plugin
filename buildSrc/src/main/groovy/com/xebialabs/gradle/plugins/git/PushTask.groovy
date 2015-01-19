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

import org.eclipse.jgit.api.Git;

public class PushTask extends GitTask {
    @Override
    void action(Git git) {
        logger.info "Pushing version changes"
        def pusher = git.push()
        def tagsPusher = git.push().setPushTags()

        PassPhraseProvider credProvider = CredentialProviderFactory.create(project)
        pusher = pusher.setCredentialsProvider(credProvider)
        tagsPusher = tagsPusher.setCredentialsProvider(credProvider)

        // note: these have to be separate pushes for the commit and the tag.
        pusher.call()
        tagsPusher.call()
    }

}

