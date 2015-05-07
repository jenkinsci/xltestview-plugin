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
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;

// TODO: should use Recorder if we want to fail the build based upon a Qualification see ArtifactArchiver
// TODO: clean up logging / system.out
public class XLTestNotifier extends Notifier {
    private final static Logger LOGGER = Logger.getLogger(XLTestNotifier.class.getName());

    public static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    public static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");

    public List<TestSpecificationDescribable> testSpecifications = Collections.emptyList();

    // constructor arguments must match config.jelly fields
    @DataBoundConstructor
    public XLTestNotifier(List<TestSpecificationDescribable> testSpecifications) {
        // System.out.printf("constructor %s\n", testSpecifications);
        this.testSpecifications = testSpecifications;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (!build.getResult().completeBuild) {
            listener.getLogger().printf("[XL Test] Not sending test run data since the build was aborted\n");
            // according to javadoc we have to do this...
            // TODO: or throw an exception?
            return true;
        }
        FilePath workspace = build.getWorkspace();

        // TODO: metadata.put("buildEnvironment", build.getEnvironment(listener));
        // TODO: metadata.put("ciServerVersion", Jenkins.getVersion().toString());

        listener.getLogger().printf("[XL Test] Uploading test run data to '%s'\n", getDescriptor().getServerUrl());

        String rootUrl = Jenkins.getInstance().getRootUrl();
        if (rootUrl == null) {
            LOGGER.log(Level.SEVERE, "Unable to determine root URL for jenkins instance aborting post build step.");
            listener.getLogger().printf("[XL Test] unable to determine root URL for the jenkins instance\n");
            throw new IllegalStateException("Unable to determine root URL for jenkins instance. Aborting XL Test post build step.");
        }
        String jobUrl = rootUrl + build.getProject().getUrl();
        String buildUrl = rootUrl + build.getUrl();
        String buildResult = translateResult(build.getResult());
        String buildNumber = Integer.toString(build.getNumber());

        for (TestSpecificationDescribable ts : testSpecifications) {
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("source", "jenkins");
            metadata.put("serverUrl", rootUrl);
            metadata.put("buildResult", buildResult);
            metadata.put("buildNumber", buildNumber);
            metadata.put("jobName", build.getProject().getFullName());
            metadata.put("jobUrl", jobUrl);
            metadata.put("buildUrl", buildUrl);
            metadata.put("executedOn", build.getBuiltOn().getNodeName());   // "" in case of master
            metadata.put("buildParameters", build.getBuildVariables());

            uploadTestRun(ts, metadata, workspace, listener.getLogger());
        }

        return true;
    }

    private void uploadTestRun(TestSpecificationDescribable ts, Map<String, Object> metadata, FilePath workspace, PrintStream logger) throws InterruptedException {
        try {
            // TODO: title would be nicer..
            logger.printf("[XL Test] Uploading test run for test specification with id '%s'\n", ts.getTestSpecificationId());
            logger.printf("[XL Test] data:\n%s\n", metadata.toString());

            getXLTestServer().uploadTestRun(ts.getTestSpecificationId(), workspace, ts.getIncludes(), ts.getExcludes(), metadata, logger);
        } catch (RuntimeException e) {
            // this probably means the build was aborted in some way...
            logger.printf("[XL Test] Error uploading: %s\n", e.getMessage());
        } catch (InterruptedException e) {
            // this probably means the build was aborted in some way...
            logger.println("[XL Test] Upload interrupted...");
            throw e;
        }
    }

    @Override
    public XLTestDescriptor getDescriptor() {
        return (XLTestDescriptor) super.getDescriptor();
    }

    private String translateResult(Result result) {
        if (result.isBetterOrEqualTo(Result.SUCCESS)) {
            return "SUCCESS";
        } else {
            return "FAILURE";
        }
    }

    private XLTestServer getXLTestServer() {
        XLTestDescriptor desc = getDescriptor();
        return XLTestServerFactory.newInstance(desc.getServerUrl(), desc.getProxyUrl(),
                lookupSystemCredentials(desc.getCredentialsId()));
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
            System.out.printf("XLTestDescriptor.configure(%s)", json.toString());

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
                if (credentialsId == null || credentialsId.isEmpty()) {
                    return FormValidation.error("No credentials specified");
                }
                StandardUsernamePasswordCredentials credentials = XLTestNotifier.lookupSystemCredentials(credentialsId);
                if (credentials == null) {
                    return FormValidation.error(String.format("Could not find credential with id '%s'", credentialsId));
                }
                if (serverUrl == null || serverUrl.isEmpty()) {
                    return FormValidation.error("No server URL specified");
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
