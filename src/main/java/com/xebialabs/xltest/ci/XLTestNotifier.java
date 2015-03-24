/**
 * Copyright (c) 2014-2015, XebiaLabs B.V., All rights reserved.
 *
 * The XL Test plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs
 * Libraries. There are special exceptions to the terms and conditions of the
 * GPLv2 as it is applied to this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xltest-plugin/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.xebialabs.xltest.ci;

import com.google.common.base.Strings;
import com.xebialabs.xltest.ci.server.XLTestServer;
import com.xebialabs.xltest.ci.server.XLTestServerFactory;
import com.xebialabs.xltest.ci.server.domain.Qualification;
import com.xebialabs.xltest.ci.server.domain.TestTool;
import hudson.Extension;
import hudson.FilePath;
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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static hudson.util.FormValidation.*;

public class XLTestNotifier extends Notifier {
    private final static Logger LOGGER = Logger.getLogger(XLTestNotifier.class.getName());

    // properties needed to create a TestSpecification at the XL Test server end
    public final String tool;
    public final String pattern;
    public final String qualification;
    public final String credential;

    @DataBoundConstructor
    public XLTestNotifier(String tool, String pattern, String credential, String qualification) {
        this.tool = tool;
        this.pattern = pattern;
        this.credential = credential;
        this.qualification = qualification;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * The post-build step is invoking this method...
     *
     * @param build
     * @param launcher
     * @param listener
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        String jobName = (build.getProject()).getName();
        // perhaps we need the build number later??? Use e.g. build.getUrl(); which returns something like job/foo/32
        // String rootUrlAsString = Jenkins.getInstance().getRootUrl(); // gives http://localhost/jenkins or whatever was specified

        FilePath workspace = build.getWorkspace();
        String hudsonUrl = build.getEnvironment(listener).get("HUDSON_URL");
        String slave = build.getBuiltOn().getDisplayName();
        int buildNumber = build.getNumber();
        String jobResult = build.getResult().toString().toLowerCase();
        Map<String, String> queryParams = new LinkedHashMap<String, String>();
        queryParams.put("tool", tool);
        queryParams.put("pattern", pattern);
        queryParams.put("jenkinsUrl", hudsonUrl);
        queryParams.put("slave", slave);
        queryParams.put("buildNumber", Integer.toString(buildNumber));
        queryParams.put("jobResult", jobResult);
        queryParams.put("qualificationType", qualification);
        queryParams.putAll(build.getBuildVariables());
        listener.getLogger().println("[XL Test] Sending back results to XL Test " + buildNumber + "; " + queryParams);
        getXLTestServer().sendBackResults(workspace, jobName, pattern, queryParams, listener.getLogger());

        return true;
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

        private List<Credential> credentials = new ArrayList<Credential>();

        // Executed on start-up of the application...
        public XLTestDescriptor() {
            load();  //deserialize from xml
            LOGGER.info("Loading credentials... This may take a while");
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            //this method is called when the global form is submitted.
            xlTestServerUrl = json.get("xlTestServerUrl").toString();
            xlTestClientProxyUrl = json.get("xlTestClientProxyUrl").toString();
            credentials = req.bindJSONToList(Credential.class, json.get("credentials"));
            save();  //serialize to xml
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
                return error("%s is not a valid URL.", url);
            }
            return ok();

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

        public ListBoxModel doFillToolItems() {
            List<TestTool> testTools = getFirstXLTestServer().getTestTools();
            ListBoxModel m = new ListBoxModel();

            for (TestTool testTool : testTools) {
                m.add(testTool.getName(), testTool.getName());
            }
            return m;
        }

        public ListBoxModel doFillQualificationItems() {
            List<Qualification> qualifications = getFirstXLTestServer().getQualifications();
            ListBoxModel m = new ListBoxModel();

            for (Qualification qualification : qualifications) {
                m.add(qualification.getName(), qualification.getName());
            }
            return m;
        }

        public FormValidation doCheckCredential(@QueryParameter String credential) {
            return warning("Changing credentials may unintentionally cause your job to fail");
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }

        private XLTestServer getFirstXLTestServer() {
            return getXLTestServer(getDefaultCredential());
        }

        private XLTestServer getXLTestServer(String credentialName) {
            checkNotNull(credentialName);
            for (Credential credential : credentials) {
                if (credentialName.equals(credential.getName())) {
                    return getXLTestServer(credential);
                }
            }
            throw new IllegalArgumentException("No XL Test server credentials found for " + credentialName);
        }

        private XLTestServer getXLTestServer(Credential credential) {
            String serverUrl = credential.resolveServerUrl(xlTestServerUrl);
            String proxyUrl = credential.resolveProxyUrl(xlTestClientProxyUrl);
            return XLTestServerFactory.newInstance(serverUrl, proxyUrl, credential.username, credential.password != null ? credential.password.getPlainText() : "");
        }

        private Credential getDefaultCredential() {
            if (credentials.isEmpty())
                throw new RuntimeException("No credentials defined in the system configuration");
            return credentials.iterator().next();
        }


    }
}
