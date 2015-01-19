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

import org.eclipse.jgit.transport.CredentialsProvider
import org.gradle.api.Project

class CredentialProviderFactory {
  public final static String GIT_USER_PROPERTY = 'gitUser'
  public final static String GIT_PASS_PHRASE_PROPERTY = 'gitPassPhrase'
  public final static String PASS_PHRASE_INTERACTIVE_PROPERTY = 'gitCredentialsInteractive'

  private CredentialProviderFactory() {
  }

  private static def getProjectProperty(Project p, String property) {
    if(p.hasProperty(property)) {
      return p.getProperty(property)
    }
    null
  }

  // NOTE: the instance seems to be cached somewhere in jgit (or the CredentialItems it coughs up)
  // Since the creds on the ListRepoContentTask are reused for the pushes
  public static CredentialsProvider create(Project p) {
    boolean interactive = false
    if (p.hasProperty(PASS_PHRASE_INTERACTIVE_PROPERTY)) {
      interactive = Boolean.parseBoolean(p.getProperty(PASS_PHRASE_INTERACTIVE_PROPERTY)).booleanValue()
    }

    def credProvider = new PassPhraseProvider(interactive,
        getProjectProperty(p, GIT_USER_PROPERTY),
        getProjectProperty(p, GIT_PASS_PHRASE_PROPERTY),
    )
    credProvider
  }
}
