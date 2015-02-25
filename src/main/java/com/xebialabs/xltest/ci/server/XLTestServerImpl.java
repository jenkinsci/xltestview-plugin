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
package com.xebialabs.xltest.ci.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.Base64;

import hudson.FilePath;
import hudson.util.DirScanner;
import hudson.util.DirScanner.Glob;
import hudson.util.io.ArchiverFactory;

public class XLTestServerImpl implements XLTestServer {

    private final static Logger LOGGER = Logger.getLogger(XLTestServerImpl.class.getName());
    private String user;
    private String password;
    private String proxyUrl;
    private String serverUrl;


    XLTestServerImpl(String serverUrl, String proxyUrl, String username, String password) {
        this.user = username;
        this.password = password;
        this.proxyUrl = proxyUrl;
        this.serverUrl = serverUrl + "/api/internal";
    }


    @Override
    public void newCommunicator() {
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        WebResource service = client.resource(serverUrl);

        LOGGER.info("Check that XL Test is running");
        ClientResponse response = service.path("data").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        switch (response.getStatus()) {
            case 200:
                return;
            case 401:
                throw new IllegalStateException("Credentials are invalid");
            case 404:
                throw new IllegalStateException("URL is invalid or server is not running");
            default:
                throw new IllegalStateException("Unknown error. Status code: " + response.getStatus() + ". Response message: " + response.toString());

        }
    }

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public void sendBackResults(String tool, String pattern, String jobName, FilePath workspace, String jenkinsUrl, String slave, int buildNumber, String jobResult, Map<String, String> buildVariables) throws IOException, InterruptedException {
        URL feedbackUrl = new URL(serverUrl + "/import/" + jobName + "?tool=" + tool + "&pattern=" + pattern + "&jenkinsUrl=" + jenkinsUrl + "&slave=" + slave + "&jobResult=" + jobResult + "&buildNumber=" + buildNumber + makeRequestParameters(buildVariables));
        HttpURLConnection connection = null;
        try {
            LOGGER.info("Trying to send workspace: " + workspace.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
            connection = (HttpURLConnection) feedbackUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            String authorization = "Basic " + new String(Base64.encode((user + ":" + password).getBytes()));
            connection.setRequestProperty("Authorization", authorization);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/zip");
            ArchiverFactory factory = ArchiverFactory.ZIP;
            DirScanner scanner = new Glob(pattern + "," + pattern + "/**", null); // no excludes supported (yet)
            LOGGER.info("Going to scan dir: " + workspace.getRemote() + " for files to zip using pattern: " + pattern);

            int numberOfFilesArchived;
            try (OutputStream os = connection.getOutputStream()) {
                numberOfFilesArchived = workspace.archive(factory, os, scanner);
                os.flush();
            }

            // Need this to trigger the sending of the request
            int responseCode = connection.getResponseCode();

            LOGGER.info("Zip sent containing: " + numberOfFilesArchived + " files. Response code from XL Test was: " + responseCode);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not deliver page information", e);
            throw e;
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Execution was interrupted", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String makeRequestParameters(Map<String, String> buildVariables) {
        StringBuilder builder = new StringBuilder(256);
        for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
            builder.append('&')
                    .append(entry.getKey())
                    .append('=')
                    .append(entry.getValue());
        }
        return builder.toString();
    }

}
