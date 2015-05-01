/**
 * Copyright (c) 2014-2015, XebiaLabs B.V., All rights reserved.
 * <p/>
 * The XL Test plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs
 * Libraries. There are special exceptions to the terms and conditions of the
 * GPLv2 as it is applied to this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xltest-plugin/blob/master/LICENSE>.
 * <p/>
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; version 2 of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.xebialabs.xltest.ci;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.xebialabs.xltest.ci.server.XLTestServer;
import com.xebialabs.xltest.ci.server.XLTestServerFactory;
import com.xebialabs.xltest.ci.server.domain.TestSpecification;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;

public class XLTestNotifier extends Notifier {
    private final static Logger LOGGER = Logger.getLogger(XLTestNotifier.class.getName());

    public static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    public static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");

    private String credentialsId;
    private transient StandardUsernamePasswordCredentials credentials;

    @DataBoundConstructor
    public XLTestNotifier(StandardUsernamePasswordCredentials credentials) {
        System.out.println("constructor " + credentials);
        this.credentialsId = credentials == null ? null : this.credentials.getId();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // Use full name for folder plugin
        String jobName = build.getProject().getFullName();
        // perhaps we need the build number later??? Use e.g. build.getUrl(); which returns something like job/foo/32
        // String rootUrlAsString = Jenkins.getInstance().getRootUrl(); // gives http://localhost/jenkins or whatever was specified

        FilePath workspace = build.getWorkspace();
        // TODO: why use environment variable? In stead of Jenkins.getInstance().getRootUrl()
        String hudsonUrl = build.getEnvironment(listener).get("HUDSON_URL");
        // TODO: why not nodename, which is a 'technical' name in stead of a display name ?
        String slave = build.getBuiltOn().getDisplayName();
        int buildNumber = build.getNumber();
        String jobResult = build.getResult().toString().toLowerCase();
        Map<String, String> queryParams = new LinkedHashMap<String, String>();
        //queryParams.put("tool", tool);
        //queryParams.put("pattern", pattern);
        //queryParams.put("qualificationType", qualification);
        queryParams.put("jenkinsUrl", hudsonUrl);
        queryParams.put("slave", slave);
        queryParams.put("buildNumber", Integer.toString(buildNumber));
        queryParams.put("jobResult", jobResult);
        // TODO: this may interfere with the previous variables
        queryParams.putAll(build.getBuildVariables());
        listener.getLogger().println("[XL Test] Sending back results to XL Test " + buildNumber + "; " + queryParams);

        getXLTestServer().sendBackResults(workspace, listener.getLogger());

        return true;
    }

    private XLTestServer getXLTestServer() {
        System.out.println("getXlTestSErver");
        XLTestDescriptor desc = getDescriptor();
        return XLTestServerFactory.newInstance(desc.getServerUrl(), desc.getProxyUrl(), getCredentials());
    }

    @Override
    public XLTestDescriptor getDescriptor() {
        return (XLTestDescriptor) super.getDescriptor();
    }

    public StandardUsernamePasswordCredentials getCredentials() {
        System.out.println("getCreds");
        String credentialsId = this.credentialsId == null
                ? (this.credentials == null ? null : this.credentials.getId())
                : this.credentialsId;
        try {
            // lookup every time so that we always have the latest
            StandardUsernamePasswordCredentials credentials = XLTestNotifier.lookupSystemCredentials(credentialsId);
            if (credentials != null) {
                this.credentials = credentials;
                return credentials;
            }
        } catch (Throwable t) {
            // ignore
        }
        return credentials;
    }

    public static StandardUsernamePasswordCredentials lookupSystemCredentials(String credentialsId) {
        System.out.println("lookupCred " + credentialsId);
        return CredentialsMatchers.firstOrNull(
                lookupCredentials(StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
                        HTTP_SCHEME, HTTPS_SCHEME),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    @Extension
    public static final class XLTestDescriptor extends BuildStepDescriptor<Publisher> {

        // ************ SERIALIZED GLOBAL PROPERTIES *********** //

        private String serverUrl;
        private String proxyUrl;
        private String credentialsId;

        // Executed on start-up of the application...
        public XLTestDescriptor() {
            load();  //deserialize from xml
        }

        /**
         * Called by UI with JSON representation of the configuration settings.
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            System.out.println(json);

            serverUrl = json.get("serverUrl").toString();
            proxyUrl = json.get("proxyUrl").toString();
            credentialsId = json.get("credentialsId").toString();

            // TODO could check URLs here? and return false?

            save();  //serialize to xml

            return true;
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            // TODO: also add requirement on host derived from URL ?
            List<StandardUsernamePasswordCredentials> creds = lookupCredentials(StandardUsernamePasswordCredentials.class, context,
                    ACL.SYSTEM,
                    HTTP_SCHEME, HTTPS_SCHEME);

            return new StandardUsernameListBoxModel().withAll(creds);
        }

        public ListBoxModel doFillTestSpecificationItems() {

            System.out.printf("doFillTesSpecItems: %s", this.toString());
            ListBoxModel items = new ListBoxModel();

            XLTestServer xlts = XLTestServerFactory.newInstance(serverUrl, proxyUrl, XLTestNotifier.lookupSystemCredentials(credentialsId));
            Map<String, TestSpecification> ts = xlts.getTestSpecifications();
            for (Map.Entry<String, TestSpecification> t : ts.entrySet()) {
                items.add(String.format("%s > %s", t.getValue().getProject().getTitle(), t.getValue().getTitle()), t.getKey());
            }
            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.XLTestNotifier_displayName();
        }

        public FormValidation doTestConnection(@QueryParameter("serverUrl") final String serverUrl,
                                               @QueryParameter("proxyUrl") final String proxyUrl,
                                               @QueryParameter("credentialsId") final String credentialsId
        ) {
            try {
                StandardUsernamePasswordCredentials credentials = XLTestNotifier.lookupSystemCredentials(credentialsId);
                if (credentials == null) {
                    return FormValidation.error("No credentials specified");
                }
                if (serverUrl == null || serverUrl.isEmpty()) {
                    return FormValidation.error("No serverUrl specified");
                }
                // see if we can create a new instance
                XLTestServer srv = XLTestServerFactory.newInstance(serverUrl, proxyUrl, credentials);
                srv.checkConnection();
                return FormValidation.ok("Success");
            } catch (RuntimeException e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }

        /* Form 'Validation' */

        public FormValidation doCheckServerUrl(@QueryParameter String value) {
            return validateOptionalUrl(value);
        }

        public FormValidation doCheckProxyUrl(@QueryParameter String value) {
            return validateOptionalUrl(value);
        }

        private FormValidation validateOptionalUrl(String url) {
            try {
                if (!Strings.isNullOrEmpty(url)) {
                    new URL(url);
                }
            } catch (MalformedURLException e) {
                return error("%s is not a valid URL.", url);
            }
            return ok();
        }

        /* boring getters */

        public String getServerUrl() {
            return serverUrl;
        }

        public String getProxyUrl() {
            return proxyUrl;
        }

        public String getCredentialsId() {
            return credentialsId;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("serverUrl", serverUrl)
                    .add("proxyUrl", proxyUrl)
                    .add("credentialsId", credentialsId)
                    .toString();
        }
    }
}
