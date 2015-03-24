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
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.Base64;

import com.xebialabs.xltest.ci.server.domain.TestTool;
import hudson.FilePath;
import hudson.util.DirScanner;
import hudson.util.DirScanner.Glob;
import hudson.util.io.ArchiverFactory;
import org.codehaus.jackson.map.ObjectMapper;

public class XLTestServerImpl implements XLTestServer {

    private final static Logger LOGGER = Logger.getLogger(XLTestServerImpl.class.getName());
    public static final String XL_TEST_LOG_PREFIX = "[XL Test]";
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
    public void checkConnection() {
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
    public void sendBackResults(FilePath workspace,  String jobName, String pattern, Map<String, String> queryParameters, PrintStream logger) throws IOException, InterruptedException {
        UriBuilder builder = UriBuilder.fromPath(serverUrl).path("/import/{arg1}");
        addRequestParameters(builder, queryParameters);
        URL feedbackUrl = builder.build(jobName).toURL();

        HttpURLConnection connection = null;
        try {
            log(logger, Level.INFO, "Trying to send workspace: " + workspace.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
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
            log(logger, Level.INFO, "Going to scan dir: " + workspace.getRemote() + " for files to zip using pattern: " + pattern);
            LOGGER.info("Going to scan dir: " + workspace.getRemote() + " for files to zip using pattern: " + pattern);

            int numberOfFilesArchived;
            try (OutputStream os = connection.getOutputStream()) {
                numberOfFilesArchived = workspace.archive(factory, os, scanner);
                os.flush();
            }

            // Need this to trigger the sending of the request
            int responseCode = connection.getResponseCode();
            log(logger, Level.INFO, "Zip sent containing: " + numberOfFilesArchived + " files. Response code from XL Test was: " + responseCode);
            if (responseCode != 200) {
                log(logger, Level.INFO, "Response message: " + connection.getResponseMessage());
            }
        } catch (IOException e) {
            log(logger, Level.SEVERE, "Could not deliver page information", e);
            LOGGER.log(Level.SEVERE, "Could not deliver page information", e);
            throw e;
        } catch (InterruptedException e) {
            log(logger, Level.SEVERE, "Execution was interrupted", e);
            LOGGER.log(Level.SEVERE, "Execution was interrupted", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public List<TestTool> getTestTools() {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        WebResource service = client.resource(serverUrl + "/testtools");

        ClientResponse response = service.path("/").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        List<TestTool> testTools = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String, String>> testToolsJsonList = mapper.readValue(response.getEntityInputStream(), List.class);
            for (Map<String, String> entry : testToolsJsonList) {
                testTools.add(new TestTool(entry.get("name"), entry.get("pattern")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return testTools;
    }

    private void addRequestParameters(UriBuilder builder, Map<String, String> buildVariables) {
        for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
    }

    private void log(PrintStream logger, Level level, String message) {
        log(logger, level, message, null);
    }

    private void log(PrintStream logger, Level level, String message, Exception e) {
        logger.println(XL_TEST_LOG_PREFIX + " [" + level.toString() + "] " + message);
        if (e != null) {
            e.printStackTrace(logger);
        }
    }
}
