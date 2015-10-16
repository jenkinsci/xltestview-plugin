package com.xebialabs.xlt.ci.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import com.xebialabs.xlt.ci.TestSpecificationDescribable;
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

    MockWebServer xltestviewMock;
    XLTestServerImpl xlTestServer;

    UsernamePassword cred = Mockito.mock(UsernamePassword.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws IOException {
        xltestviewMock = new MockWebServer();
        xltestviewMock.start();

        when(cred.getUsername()).thenReturn("admin");
        when(cred.getPassword()).thenReturn("admin");


        xlTestServer = new XLTestServerImpl(String.format("http://127.0.0.1:%d", xltestviewMock.getPort()), null, cred);
    }

    @After
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

        PrintStream logger = new PrintStream(System.out);

        Map<String, Object> metadata = createMetadata();

        xlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, logger);

        RecordedRequest request = xltestviewMock.takeRequest();
        verifyUploadRequest(request);
    }

    @Test
    public void shouldImportWithTrailingSlash() throws IOException, InterruptedException, MessagingException {
        String TRAILING_SLASH = "/";

        XLTestServerImpl slashedXlTestServer = new XLTestServerImpl(String.format("http://127.0.0.1:%d" + TRAILING_SLASH, xltestviewMock.getPort()), null, cred);

        xltestviewMock.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{ \"testRunId\": \"testrunid\" }"));
        FilePath fp = new FilePath(new File(this.getClass().getResource("/demo_test_results").getPath()));

        PrintStream logger = new PrintStream(System.out);

        Map<String, Object> metadata = createMetadata();

        slashedXlTestServer.uploadTestRun("testspecid", fp, "**/*.xml", null, metadata, logger);

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


    @Test
    public void shouldFailCheckConnectionWithNotFound() throws IOException, InterruptedException, MessagingException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("URL is invalid or server is not running");

        xltestviewMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.SC_NOT_FOUND));

        xlTestServer.checkConnection();

        xltestviewMock.takeRequest();
    }

    @Test
    public void shouldFailCheckConnectionWithBadCredentials() throws IOException, InterruptedException, MessagingException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Credentials are invalid");

        xltestviewMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.SC_UNAUTHORIZED));

        xlTestServer.checkConnection();

        xltestviewMock.takeRequest();
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
}
