package com.xebialabs.xlt.ci.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.HttpStatus;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import com.xebialabs.xlt.ci.TestSpecificationDescribable;
import com.xebialabs.xlt.ci.server.authentication.AuthenticationException;
import com.xebialabs.xlt.ci.server.authentication.UsernamePassword;
import com.xebialabs.xlt.ci.server.domain.TestSpecification;

import hudson.FilePath;
import hudson.util.ListBoxModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class XLTestServerImplTest {

    // TODO: this is from the demo data and not 'the whole truth'
    private static final String TEST_SPEC_RESPONSE = "{\"regressionTests\":{\"id\":\"regressionTests\",\"project\":{\"id\":\"DemoProject\",\"title\":\"Demo " +
            "Project\",\"type\":\"xlt.Project\"},\"qualification\":{\"description\":\"Description unavailable\",\"type\":\"xlt" +
            ".DefaultFunctionalTestsQualifier\"},\"title\":\"regressionTests\",\"type\":\"xlt.ShowCaseTestSpecification\"}," +
            "\"demoGatling\":{\"id\":\"demoGatling\",\"project\":{\"id\":\"DemoProject\",\"title\":\"Demo Project\",\"type\":\"xlt.Project\"}," +
            "\"qualification\":{\"description\":\"Description unavailable\",\"type\":\"xlt.DefaultPerformanceTestsQualifier\"},\"title\":\"demoGatling\"," +
            "\"type\":\"xlt.ShowCaseTestSpecification\"},\"functionalTestsComponentA\":{\"id\":\"functionalTestsComponentA\"," +
            "\"project\":{\"id\":\"DemoProject\",\"title\":\"Demo Project\",\"type\":\"xlt.Project\"},\"qualification\":{\"description\":\"Description " +
            "unavailable\",\"type\":\"xlt.DefaultFunctionalTestsQualifier\"},\"title\":\"functionalTestsComponentA\",\"type\":\"xlt" +
            ".ShowCaseTestSpecification\"},\"f3850327-69df-4f01-b063-e5d367a960f8\":{\"id\":\"f3850327-69df-4f01-b063-e5d367a960f8\"," +
            "\"project\":{\"id\":\"DemoProject\",\"title\":\"Demo Project\",\"type\":\"xlt.Project\"},\"title\":\"allMyTests\",\"type\":\"xlt" +
            ".TestSpecificationSet\"},\"calculatorTestsComponentB\":{\"id\":\"calculatorTestsComponentB\",\"project\":{\"id\":\"DemoProject\"," +
            "\"title\":\"Demo Project\",\"type\":\"xlt.Project\"},\"qualification\":{\"description\":\"Description unavailable\",\"type\":\"xlt" +
            ".DefaultFunctionalTestsQualifier\"},\"title\":\"calculatorTestsComponentB\",\"type\":\"xlt.ShowCaseTestSpecification\"},\"performance tests " +
            "(old) for Demo\":{\"id\":\"performance tests (old) for Demo\",\"project\":{\"id\":\"DemoProject\",\"title\":\"Demo Project\",\"type\":\"xlt" +
            ".Project\"},\"title\":\"performance tests (old) for Demo\",\"type\":\"xlt.ShowCaseTestSpecification\"}}";

    public static final String PLUGIN_VERSION = " 1.2.3-SNAPSHOT";

    private XLTestServerImpl xlTestServer;
    private MockWebServer xltestviewMock;

    private final UsernamePassword cred = Mockito.mock(UsernamePassword.class);
    private final Log4jStream log4jStream = new Log4jStream(null, "XLTestServerImpl");

    @BeforeMethod
    public void setup() throws IOException {
        xltestviewMock = new MockWebServer();
        xltestviewMock.start();

        when(cred.getUsername()).thenReturn("admin");
        when(cred.getPassword()).thenReturn("admin");

        xlTestServer = new XLTestServerImpl(String.format("http://127.0.0.1:%d", xltestviewMock.getPort()), null, cred) {
            @Override
            protected String getPluginVersion() {
                return PLUGIN_VERSION;
            }
        };
    }

    @AfterMethod(alwaysRun = true)
    public void teardown() throws IOException {
        xltestviewMock.shutdown();
    }

    @Test
    public void shouldCheckConnection() throws InterruptedException {
        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{}"));

        xlTestServer.checkConnection();

        RecordedRequest request = xltestviewMock.takeRequest();
        assertEquals(request.getRequestLine(), "GET /api/internal/data HTTP/1.1");
        assertEquals(request.getHeader("accept"), "application/json; charset=utf-8");
        assertEquals(request.getHeader("authorization"), "Basic YWRtaW46YWRtaW4=");
        assertEquals(request.getHeader("User-Agent"), "XL TestView Jenkins plugin 1.2.3-SNAPSHOT");
        assertEquals(request.getBody().readUtf8(), "");
    }

    @Test
    public void shouldLoadTestSpecifications() throws IOException, InterruptedException {
        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(TEST_SPEC_RESPONSE));

        Map<String, TestSpecification> testSpecs = xlTestServer.getTestSpecifications();
        assertEquals(testSpecs.size(), 6L);

        RecordedRequest request = xltestviewMock.takeRequest();
        assertEquals(request.getRequestLine(), "GET /api/internal/testspecifications/extended HTTP/1.1");
        assertEquals(request.getHeader("accept"), "application/json; charset=utf-8");
        assertEquals(request.getHeader("authorization"), "Basic YWRtaW46YWRtaW4=");
        assertEquals(request.getBody().readUtf8(), "");
    }

    @Test
    public void fillsTestSpecificationIdItems() {
        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(TEST_SPEC_RESPONSE));

        Map<String, TestSpecification> testSpecifications = xlTestServer.getTestSpecifications();
        assertEquals(testSpecifications.size(), 6);

        ListBoxModel result = TestSpecificationDescribable.TestSpecificationDescriptor.getSpecificationOptions(testSpecifications);

        assertEquals(result.size(), 5); // because we don't count the one superset in the list
    }

    @Test
    public void shouldImport() throws Exception {
        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{ \"testRunId\": \"testrunid\" }"));
        FilePath fp = new FilePath(new File(this.getClass().getResource("/demo_test_results").getPath()));


        Map<String, Object> metadata = createMetadata();

        xlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, log4jStream);

        RecordedRequest request = xltestviewMock.takeRequest();
        verifyUploadRequest(request);
    }

    @Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = "User 'admin' and the supplied password are unable to log in")
    public void shouldHandleAuthenticationErrorDuringImport() throws Exception {
        xltestviewMock.enqueue(new MockResponse().setResponseCode(401));
        FilePath fp = new FilePath(new File(this.getClass().getResource("/demo_test_results").getPath()));

        Map<String, Object> metadata = createMetadata();

        xlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, log4jStream);
    }

    @Test(expectedExceptions = PaymentRequiredException.class, expectedExceptionsMessageRegExp = "The XL TestView server does not have a valid license")
    public void shouldHandleLicenseErrorDuringImport() throws Exception {
        xltestviewMock.enqueue(new MockResponse().setResponseCode(402));
        FilePath fp = new FilePath(new File(this.getClass().getResource("/demo_test_results").getPath()));

        Map<String, Object> metadata = createMetadata();

        xlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, log4jStream);
    }

    @Test(expectedExceptions = ConnectionException.class, expectedExceptionsMessageRegExp = "Cannot find test specification 'testspecid. Please check if the XL TestView server is running and the test specification exists.")
    public void shouldHandleConnectionErrorDuringImport() throws Exception {
        xltestviewMock.enqueue(new MockResponse().setResponseCode(404));
        FilePath fp = new FilePath(new File(this.getClass().getResource("/demo_test_results").getPath()));


        Map<String, Object> metadata = createMetadata();

        xlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, log4jStream);
    }

    @Test
    public void shouldImportWithTrailingSlash() throws IOException, InterruptedException, MessagingException {
        String TRAILING_SLASH = "/";

        XLTestServerImpl slashedXlTestServer = new XLTestServerImpl(String.format("http://127.0.0.1:%d" + TRAILING_SLASH, xltestviewMock.getPort()), null, cred);

        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{ \"testRunId\": \"testrunid\" }"));
        FilePath fp = new FilePath(new File(this.getClass().getResource("/demo_test_results").getPath()));

        Map<String, Object> metadata = createMetadata();

        slashedXlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, log4jStream);

        RecordedRequest request = xltestviewMock.takeRequest();
        verifyUploadRequest(request);
    }

    @Test
    public void shouldCheckConnectionWithTrailingSlash() throws IOException, InterruptedException, MessagingException {
        String TRAILING_SLASH = "/";

        XLTestServerImpl slashedXlTestServer = new XLTestServerImpl(String.format("http://127.0.0.1:%d" + TRAILING_SLASH, xltestviewMock.getPort()), null, cred);

        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{}"));

        slashedXlTestServer.checkConnection();

        RecordedRequest request = xltestviewMock.takeRequest();
        assertEquals(request.getRequestLine(), "GET /api/internal/data HTTP/1.1");
        assertEquals(request.getHeader("accept"), "application/json; charset=utf-8");
        assertEquals(request.getHeader("authorization"), "Basic YWRtaW46YWRtaW4=");
        assertEquals(request.getBody().readUtf8(), "");
    }


    @Test(expectedExceptions = ConnectionException.class, expectedExceptionsMessageRegExp = "URL is invalid or server is not running")
    public void shouldFailCheckConnectionWithNotFound() throws IOException, InterruptedException, MessagingException {
        xltestviewMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.SC_NOT_FOUND));

        xlTestServer.checkConnection();

        xltestviewMock.takeRequest();
    }

    @Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = "User 'admin' and the supplied password are unable to log in")
    public void shouldFailCheckConnectionWithBadCredentials() throws IOException, InterruptedException, MessagingException {
        xltestviewMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.SC_UNAUTHORIZED));

        xlTestServer.checkConnection();
    }

    @Test(expectedExceptions = PaymentRequiredException.class, expectedExceptionsMessageRegExp = "The XL TestView server does not have a valid license")
    public void shouldFailCheckConnectionWithInvalidLicense() throws IOException, InterruptedException, MessagingException {
        xltestviewMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.SC_PAYMENT_REQUIRED));

        xlTestServer.checkConnection();
    }


    private void verifyUploadRequest(final RecordedRequest request) throws IOException, MessagingException {
        assertEquals(request.getRequestLine(), "POST /api/internal/import/testspecid HTTP/1.1");
        assertEquals(request.getHeader("accept"), "application/json; charset=utf-8");
        assertEquals(request.getHeader("authorization"), "Basic YWRtaW46YWRtaW4=");
        assertThat(request.getHeader("Content-Length"), is(nullValue()));
        assertThat(request.getChunkSizes().get(0), is(2048));
        assertThat(request.getChunkSizes().size(), is(23));
        assertThat(request.getChunkSizes().get(22), is(1036));

        assertTrue(request.getBodySize() > 0);

        ByteArrayDataSource bads = new ByteArrayDataSource(request.getBody().inputStream(), "multipart/mixed");
        MimeMultipart mp = new MimeMultipart(bads);
        assertTrue(request.getBodySize() > 0);

        assertEquals(mp.getCount(), 2);
        assertEquals(mp.getContentType(), "multipart/mixed");

        // TODO could do additional checks on metadata content
        BodyPart bodyPart1 = mp.getBodyPart(0);
        assertEquals(bodyPart1.getContentType(), "application/json; charset=utf-8");

        BodyPart bodyPart2 = mp.getBodyPart(1);
        assertEquals(bodyPart2.getContentType(), "application/zip");
    }


    private Map<String, Object> createMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("source", "jenkins");
        metadata.put("serverUrl", "http://my-jenkins");
        metadata.put("buildResult", "success");
        metadata.put("buildNumber", "10");
        metadata.put("jobUrl", "http://my-jenkins/job/sub-dir/my-job");
        metadata.put("jobName", "sub-dir/my-job");
        metadata.put("buildUrl", "http://my-jenkins/job/test/14");
        metadata.put("executedOn", "slave1");
        metadata.put("buildParameters", new HashMap());
        return metadata;
    }

    private static final class Log4jStream extends PrintStream {
        private final Logger LOG;

        public Log4jStream(final OutputStream out, String logName) {
            super(new NullOutputStream());
            LOG = LoggerFactory.getLogger(logName);

        }

        @Override
        public PrintStream printf(final String format, final Object... args) {
            LOG.debug(String.format(format, args));

            return null;
        }
    }
}
