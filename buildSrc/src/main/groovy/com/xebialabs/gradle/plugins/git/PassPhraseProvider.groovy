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

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PassPhraseProvider extends CredentialsProvider {
    Logger logger = LoggerFactory.getLogger(PassPhraseProvider)
    String passPhrase
    String userName
    boolean interactive

    public PassPhraseProvider(boolean interactive, String userName, String passPhrase) {
        this.interactive = interactive
        this.userName = userName
        this.passPhrase = passPhrase
    }
    def boolean isUserNamePassword(CredentialItem i1, CredentialItem i2) {
        return ((i1 instanceof CredentialItem.Username && i2 instanceof CredentialItem.Password) ||
                (i2 instanceof CredentialItem.Username && i1 instanceof CredentialItem.Password))
    }
    @Override
    public boolean supports(CredentialItem... items) {
        if (items.length == 1 && items[0] instanceof CredentialItem.StringType)
            return true;
        if (items.length == 2 && isUserNamePassword(items[0], items[1])) {
            return true
        }
        return false;
    }
    @Override
    public boolean isInteractive() {
        return interactive;
    }
    String readValue(final String prompt, final String defVal, boolean password) {
        def val = defVal
        if( val == null ) {
            if (isInteractive()) {
                def console = System.console()
                logger.debug("Going interactive: '${prompt}'")
                if (console) {
                    if(password) {
                        char[] pass = console.readPassword("\nPlease enter:\n${prompt} :".toString())
                        if(pass == null) {
                            val = ''
                        }
                        else {
                            val = new String(pass)
                        }
                    }
                    else {
                        val = console.readLine("\nPlease enter:\n${prompt} :".toString())
                    }
                }
                else {
                    logger.warn("Could not get console access to read value for ${prompt}.")
                    return false
                }
            }
            else {
                throw new RuntimeException("No value set for ${prompt} and not interactive (set ${CredentialProviderFactory.PASS_PHRASE_INTERACTIVE_PROPERTY} to true in ~/.gradle/gradle.properties)")
            }
        }
        val
    }
    public boolean getSSH(CredentialItem.StringType passwordCi) {
        String pass = readValue(passwordCi.promptText, this.passPhrase, true)
        if(pass == null) {
            return false
        }
        this.passPhrase = pass
        passwordCi.setValue(pass)
        return true
    }
    boolean getHTTPS(CredentialItem i1, CredentialItem i2) {
        CredentialItem.Username userCi
        CredentialItem.Password passwordCi
        if(i1 instanceof CredentialItem.Username) {
            userCi = i1 as CredentialItem.Username
            passwordCi = i2 as CredentialItem.Password
        }
        else {
            userCi = i2 as CredentialItem.Username
            passwordCi = i1 as CredentialItem.Password
        }
        def user = readValue(userCi.promptText, this.userName, false)
        if(user == null) {
            return false
        }
        this.userName = user
        userCi.setValue(user)
        String pass = readValue(passwordCi.promptText, this.passPhrase, true)
        if(pass == null) {
            return false
        }
        this.passPhrase = pass
        passwordCi.setValue(pass.toCharArray())
        return true
    }
    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        logger.debug("get URI=${uri} items=${items}")
        if (uri.scheme == 'ssh') {
            if (items.length != 1) {
                throw new UnsupportedCredentialItem(uri, "Only expected one item for passphrase")
            }
            return getSSH(items[0] as CredentialItem.StringType)
        }
        else if (uri.scheme == 'https') {
            if (items.length != 2) {
                throw new UnsupportedCredentialItem(uri, "Only expected one item for passphrase")
            }
            return getHTTPS(items[0], items[1])
        }
        else {
            throw new UnsupportedCredentialItem(uri, "Unsupported scheme ${uri.scheme} expect ssh/https")
        }
    }
}