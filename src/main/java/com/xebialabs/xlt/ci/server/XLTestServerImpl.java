/**
 * Copyright (c) 2014-2015, XebiaLabs B.V., All rights reserved.
 * <p/>
 * The XL TestView plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs
 * Libraries. There are special exceptions to the terms and conditions of the
 * GPLv2 as it is applied to this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xltestview-plugin/blob/master/LICENSE>.
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
package com.xebialabs.xlt.ci.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.*;

import com.xebialabs.xlt.ci.server.authentication.UsernamePassword;
import com.xebialabs.xlt.ci.server.domain.ImportError;
import com.xebialabs.xlt.ci.server.domain.TestSpecification;

import hudson.FilePath;
import hudson.util.DirScanner;
import hudson.util.io.ArchiverFactory;
import okio.BufferedSink;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class XLTestServerImpl implements XLTestServer {
    private static final Logger LOG = LoggerFactory.getLogger(XLTestServerImpl.class);

    public static final String XL_TEST_LOG_FORMAT = "[XL TestView] [%s] %s\n";
    public static final TypeReference<Map<String, TestSpecification>> MAP_OF_TESTSPECIFICATION = new TypeReference<Map<String, TestSpecification>>() {
    };

    public static final String API_CONNECTION_CHECK = "/api/internal/data";
    public static final String API_TESTSPECIFICATIONS_EXTENDED = "/api/internal/testspecifications/extended";
    public static final String API_IMPORT = "/api/internal/import";
    public static final String APPLICATION_JSON_UTF_8 = "application/json; charset=utf-8";

    private OkHttpClient client = new OkHttpClient();

    private URI proxyUrl;
    private URL serverUrl;

    private UsernamePassword credentials;

    XLTestServerImpl(String serverUrl, String proxyUrl, UsernamePassword credentials) {
        try {
            this.serverUrl = new URL(serverUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        if (credentials == null) {
            throw new IllegalArgumentException("Need credentials to connect to " + serverUrl);
        }
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
            URL url = createUrl(relativeUrl);

            return new Request.Builder()
                    .url(url)
                    .header("Accept", APPLICATION_JSON_UTF_8)
                    .header("Authorization", createCredential())
                    .build();

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL createUrl(String relativeUrl) throws MalformedURLException, URISyntaxException {
        return new URL(serverUrl, relativeUrl);
    }

    private String createCredential() {
        return Credentials.basic(credentials.getUsername(), credentials.getPassword());
    }

    @Override
    public void checkConnection() {
        try {
            LOG.info("Checking connection to {}", serverUrl);
            String serverUrl = this.serverUrl.toString();
            if (serverUrl.length() > 1 && serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
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

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public void uploadTestRun(String testSpecificationId, FilePath workspace, String includes, String excludes, Map<String, Object> metadata, PrintStream
            logger) throws IOException, InterruptedException {
        if (testSpecificationId == null || testSpecificationId.isEmpty()) {
            throw new IllegalArgumentException("No test specification id specified. Does the test specification still exist in XL TestView?");
        }
        try {
            logInfo(logger, format("Collecting files from '%s' using include pattern: '%s' and exclude pattern '%s'",
                    workspace.getRemote(), includes, excludes));

            DirScanner scanner = new DirScanner.Glob(includes, excludes);

            ObjectMapper objectMapper = new ObjectMapper();

            RequestBody body = new MultipartBuilder().type(MultipartBuilder.MIXED)
                    .addPart(RequestBody.create(MediaType.parse(APPLICATION_JSON_UTF_8), objectMapper.writeValueAsString(metadata)))
                    .addPart(new ZipRequestBody(workspace, scanner, logger))
                    .build();

            Request request = new Request.Builder()
                    .url(createUrl(API_IMPORT + "/" + testSpecificationId))
                    .header("Accept", APPLICATION_JSON_UTF_8)
                    .header("Authorization", createCredential())
                    .header("Transfer-Encoding", "chunked")
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            ObjectMapper mapper = createMapper();
            ImportError importError;
            switch (response.code()) {
                case 200:
                    logInfo(logger, "Sent data successfully");
                    return;
                case 304:
                    logWarn(logger, "No new results were detected. Nothing was imported.");
                    throw new IllegalStateException("No new results were detected. Nothing was imported.");
                case 400:
                    importError = mapper.readValue(response.body().byteStream(), ImportError.class);
                    throw new IllegalStateException(importError.getMessage());
                case 422:
                    logWarn(logger, "Unable to process results.");
                    logWarn(logger, "Are you sure your include/exclude pattern provides all needed files for the test tool?");
                    importError = mapper.readValue(response.body().byteStream(), ImportError.class);
                    throw new IllegalStateException(importError.getMessage());
                case 401:
                    throw new IllegalStateException("Credentials are invalid");
                case 404:
                    throw new IllegalArgumentException("Test specification '" + testSpecificationId + "' does not exists?");
                default:
                    throw new IllegalStateException("Unknown error. Status code: " + response.code() + ". Response message: " + response.toString());
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("I/O error uploading test run data to " + serverUrl.toString(), e);
        }
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
            LOG.debug("Received test specifications: {}", testSpecifications);
            return testSpecifications;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void logInfo(PrintStream logger, String message) {
        LOG.info(message);
        logger.printf(XL_TEST_LOG_FORMAT, "INFO", message);
    }

    private void logWarn(PrintStream logger, String message) {
        LOG.warn(message);
        logger.printf(XL_TEST_LOG_FORMAT, "WARN", message);
    }

    private void log(PrintStream logger, String level, String message, Exception e) {
        if (e != null) {
            LOG.error("Exception ", e);
        }
    }

    private class CloseIgnoringOutputStream extends OutputStream {
        private final OutputStream wrapped;

        public CloseIgnoringOutputStream(OutputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void write(int b) throws IOException {
            wrapped.write(b);
        }

        @Override
        public void close() throws IOException {
            // let's ignore this...
        }
    }

    private class ZipRequestBody extends RequestBody {
        private final FilePath workspace;
        private final DirScanner scanner;
        private final PrintStream logger;

        public ZipRequestBody(FilePath workspace, DirScanner scanner, PrintStream logger) {
            this.workspace = workspace;
            this.scanner = scanner;
            this.logger = logger;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse("application/zip");
        }

        @Override
        public long contentLength() {
            return -1L;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            ArchiverFactory factory = ArchiverFactory.ZIP;
            OutputStream os = null;
            try {
                // the archive function 'conveniently' closes our outputstream
                os = new CloseIgnoringOutputStream(sink.outputStream());
                int numberOfFilesArchived = workspace.archive(factory, os, scanner);
                logInfo(logger, format("Zipped %d files", numberOfFilesArchived));
            } catch (InterruptedException e) {
                throw new RuntimeException("Writing of zip interrupted.", e);
            } finally {
                closeQuietly(os);
            }
        }
    }
}
