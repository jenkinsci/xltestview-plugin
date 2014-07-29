/**
 * Copyright (c) 2014, XebiaLabs B.V., All rights reserved.
 *
 *
 * The XL Release plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xltest-plugin/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */

package com.xebialabs.xltest.ci;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.xebialabs.xltest.ci.server.XLTestServer;
import com.xebialabs.xltest.ci.server.XLTestServerFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.google.common.base.Strings;

import com.xebialabs.xltest.ci.util.JenkinsReleaseListener;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import static hudson.util.FormValidation.warning;

public class XLTestNotifier extends Notifier {

    public final String credential;

    public final String suiteNames;

    public final String fitnesseRootLocation;

    public final String callbackUri;


    @DataBoundConstructor
    public XLTestNotifier(String credential, String suiteNames, String fitnesseRootLocation, String callbackUri) {
        this.credential = credential;
        this.suiteNames = suiteNames;
        this.fitnesseRootLocation = fitnesseRootLocation;
        this.callbackUri = callbackUri;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        final JenkinsReleaseListener deploymentListener = new JenkinsReleaseListener(listener);

        final EnvVars envVars = build.getEnvironment(listener);
        String replacedSuiteNames = envVars.expand(this.suiteNames);

        String replacedFitnesseRootLocation = envVars.expand(this.fitnesseRootLocation);
        String replacedCallbackUri = envVars.expand(this.callbackUri);

        sendBackResults(replacedSuiteNames, replacedFitnesseRootLocation, replacedCallbackUri, deploymentListener);

        return true;
    }

    private void sendBackResults(final String replacedSuiteNames, final String replacedFitnesseRootLocation, final String replacedCallbackUri, final JenkinsReleaseListener deploymentListener) {
        deploymentListener.info(Messages.XLTestNotifier_sendBackResults(suiteNames));

        // send back results to XL Test
        getXLTestServer().sendBackResults(replacedSuiteNames, replacedFitnesseRootLocation, replacedCallbackUri);
    }

    private XLTestServer getXLTestServer() {
        return getDescriptor().getXLTestServer(credential);
    }

    @Override
    public XLTestDescriptor getDescriptor() {
        return (XLTestDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class XLTestDescriptor extends BuildStepDescriptor<Publisher> {

        // ************ SERIALIZED GLOBAL PROPERTIES *********** //

        private String xlTestServerUrl;

        private String xlTestClientProxyUrl;

        private List<Credential> credentials = newArrayList();

        // ************ OTHER NON-SERIALIZABLE PROPERTIES *********** //

        private final transient Map<String,XLTestServer> credentialServerMap = newHashMap();

        public XLTestDescriptor() {
            load();  //deserialize from xml
            mapCredentialsByName();
        }

        private void mapCredentialsByName() {
            for (Credential credential : credentials) {
                String serverUrl = credential.resolveServerUrl(xlTestServerUrl);
                String proxyUrl = credential.resolveProxyUrl(xlTestClientProxyUrl);

                credentialServerMap.put(credential.name,
                        XLTestServerFactory.newInstance(serverUrl, proxyUrl, credential.username, credential.password != null ? credential.password.getPlainText() : ""));
            }
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            //this method is called when the global form is submitted.
            xlTestServerUrl = json.get("xlTestServerUrl").toString();
            xlTestClientProxyUrl = json.get("xlTestClientProxyUrl").toString();
            credentials = req.bindJSONToList(Credential.class, json.get("credentials"));
            save();  //serialize to xml
            mapCredentialsByName();
            return true;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.XLTestNotifier_displayName();
        }

        private FormValidation validateOptionalUrl(String url) {
            try {
                if (!Strings.isNullOrEmpty(url)) {
                    new URL(url);
                }
            } catch (MalformedURLException e) {
                return error("%s is not a valid URL.",url);
            }
            return ok();

        }

        public FormValidation doCheckXLTestServerUrl(@QueryParameter String xlTestServerUrl) {
            if (Strings.isNullOrEmpty(xlTestServerUrl)) {
                return error("Url required.");
            }
            return validateOptionalUrl(xlTestServerUrl);
        }

        public FormValidation doCheckXLTestClientProxyUrl(@QueryParameter String xlTestClientProxyUrl) {
            return validateOptionalUrl(xlTestClientProxyUrl);
        }





        public List<Credential> getCredentials() {
            return credentials;
        }

        public String getXlTestServerUrl() {
            return xlTestServerUrl;
        }

        public String getXlTestClientProxyUrl() {
            return xlTestClientProxyUrl;
        }

        public ListBoxModel doFillCredentialItems() {
            ListBoxModel m = new ListBoxModel();
            for (Credential c : credentials)
                m.add(c.name, c.name);
            return m;
        }

        public FormValidation doCheckCredential(@QueryParameter String credential) {
            return warning("Changing credentials may unintentionally cause your job to fail");
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }


        private XLTestServer getXLTestServer(String credential) {
            checkNotNull(credential);
            return credentialServerMap.get(credential);
        }


        private Credential getDefaultCredential() {
            if (credentials.isEmpty())
                throw new RuntimeException("No credentials defined in the system configuration");
            return credentials.iterator().next();
        }


    }
}
