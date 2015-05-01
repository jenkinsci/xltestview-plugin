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
package com.xebialabs.xltest.ci.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.xebialabs.xltest.ci.server.authentication.UsernamePassword;
import com.xebialabs.xltest.ci.server.domain.TestSpecification;
import hudson.FilePath;
import hudson.util.DirScanner;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XLTestServerImpl implements XLTestServer {
    private final static Logger LOGGER = Logger.getLogger(XLTestServerImpl.class.getName());

    public static final String XL_TEST_LOG_PREFIX = "[XL Test]";
    public static final TypeReference<Map<String, TestSpecification>> MAP_OF_TESTSPECIFICATION = new TypeReference<Map<String, TestSpecification>>() {
    };

    public static final String API_CONNECTION_CHECK = "/api/internal/data";
    public static final String API_TESTSPECIFICATIONS_EXTENDED = "/api/internal/testspecifications/extended";
    public static final String API_IMPORT = "/api/internal/import";

    private OkHttpClient client = new OkHttpClient();

    private URI proxyUrl;
    private URI serverUrl;

    private UsernamePassword credentials;

    XLTestServerImpl(String serverUrl, String proxyUrl, UsernamePassword credentials) {
        this.serverUrl = URI.create(serverUrl);
        this.credentials = credentials;
        this.proxyUrl = proxyUrl != null && !proxyUrl.isEmpty() ? URI.create(proxyUrl) : null;
        setupHttpClient();
    }

    private void setupHttpClient() {
        // TODO: make configurable ?
        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setWriteTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);

        if (proxyUrl != null) {
            Proxy p = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyUrl.getHost(), proxyUrl.getPort()));
            client.setProxy(p);
        }
    }

    private Request createRequestFor(String relativeUrl) {
        try {
            URL url = new URI(serverUrl.toString() + relativeUrl).toURL();

            return new Request.Builder()
                    .url(url)
                    .header("Accept", "application/json; charset=utf-8")
                    .header("Authorization", createCredential())
                    .build();

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void checkConnection() {
        try {
            LOGGER.info("Checking connection to " + serverUrl);
            Request request = createRequestFor(API_CONNECTION_CHECK);

            Response response = client.newCall(request).execute();
            switch (response.code()) {
                case 200:
                    return;
                case 401:
                    throw new IllegalStateException("Credentials are invalid");
                case 404:
                    throw new IllegalStateException("URL is invalid or server is not running");
                default:
                    throw new IllegalStateException("Unknown error. Status code: " + response.code() + ". Response message: " + response.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createCredential() {
        return Credentials.basic(credentials.getUsername(), credentials.getPassword());
    }

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public void sendBackResults(FilePath workspace, PrintStream logger) throws IOException, InterruptedException {
//        try {
//            LOGGER.info("Uploading results to " + serverUrl);
//            Request request = createRequestFor(API_IMPORT);
//
//            Response response = client.newCall(request).execute();
//            switch (response.code()) {
//                case 200:
//                    return;
//                case 401:
//                    throw new IllegalStateException("Credentials are invalid");
//                case 404:
//                    throw new IllegalStateException("URL is invalid or server is not running");
//                default:
//                    throw new IllegalStateException("Unknown error. Status code: " + response.code() + ". Response message: " + response.toString());
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

//        UriBuilder builder = UriBuilder.fromPath(serverUrl).path("/import/{arg1}");
//        //addRequestParameters(builder, queryParameters);
//        URL feedbackUrl = builder.build("testspecidgoeshere").toURL();
//
//        String user = getUsername();
//        String password = getPassword();
//
//        HttpURLConnection connection = null;
//        try {
//            log(logger, Level.INFO, "Trying to send workspace: " + workspace.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
//            LOGGER.info("Trying to send workspace: " + workspace.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
//
//            connection = (HttpURLConnection) feedbackUrl.openConnection();
//            connection.setDoOutput(true);
//            connection.setDoInput(true);
//            connection.setInstanceFollowRedirects(false);
//            connection.setUseCaches(false);
//
//            String authorization = "Basic " + new String(Base64.encode((user + ":" + password).getBytes()));
//            connection.setRequestProperty("Authorization", authorization);
//
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/zip");
//            ArchiverFactory factory = ArchiverFactory.ZIP;
//
//            // TODO: pattern + I don't get the logic behind what's passed into Glob
//        String pattern = "**/*.xml";
//        DirScanner scanner = new DirScanner.Glob(pattern + "," + pattern + "/**", null);
//        // no excludes supported (yet)
//        log(logger, Level.INFO, "Going to scan dir: " + workspace.getRemote() + " for files to zip using pattern: " + pattern);
//        LOGGER.info("Going to scan dir: " + workspace.getRemote() + " for files to zip using pattern: " + pattern);
//
//            int numberOfFilesArchived;
//            OutputStream os = null;
//            try {
//                os = connection.getOutputStream();
//                numberOfFilesArchived = workspace.archive(factory, os, scanner);
//                os.flush();
//            } finally {
//                closeQuietly(os);
//            }
//
//            // Need this to trigger the sending of the request
//            int responseCode = connection.getResponseCode();
//            log(logger, Level.INFO, "Zip sent containing: " + numberOfFilesArchived + " files. Response code from XL Test was: " + responseCode);
//            if (responseCode != 200) {
//                log(logger, Level.INFO, "Response message: " + connection.getResponseMessage());
//            }
//        } catch (IOException e) {
//            log(logger, Level.SEVERE, "Could not deliver page information", e);
//            LOGGER.log(Level.SEVERE, "Could not deliver page information", e);
//            throw e;
//        } catch (InterruptedException e) {
//            log(logger, Level.SEVERE, "Execution was interrupted", e);
//            LOGGER.log(Level.SEVERE, "Execution was interrupted", e);
//            throw e;
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
    }

    private ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // make things lenient...
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Override
    public Map<String, TestSpecification> getTestSpecifications() {
        try {
            Request request = createRequestFor(API_TESTSPECIFICATIONS_EXTENDED);
            Response response = client.newCall(request).execute();

            ObjectMapper mapper = createMapper();
            Map<String, TestSpecification> testSpecifications = mapper.readValue(response.body().byteStream(), MAP_OF_TESTSPECIFICATION);
            LOGGER.finer("Received test specifications: " + testSpecifications);
            return testSpecifications;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
