/**
 * Copyright (c) 2014, XebiaLabs B.V., All rights reserved.
 *
 *
 * The XL Test plugin for Jenkins is licensed under the terms of the GPLv2
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

package com.xebialabs.xltest.ci.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.MediaType;

import com.xebialabs.xltest.fitnesse.FeedbackEventSender;
import com.xebialabs.xltest.fitnesse.PageHistoryExtractor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.xebialabs.xltest.ci.NameValuePair;
import com.xebialabs.xltest.ci.util.CreateReleaseView;
import com.xebialabs.xltest.ci.util.ReleaseFullView;
import com.xebialabs.xltest.ci.util.TemplateVariable;

public class XLTestServerImpl implements XLTestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XLTestServerImpl.class);

    private String user;
    private String password;
    private String proxyUrl;
    private String serverUrl;

    XLTestServerImpl(String serverUrl, String proxyUrl, String username, String password) {
        this.user=username;
        this.password=password;
        this.proxyUrl=proxyUrl;
        this.serverUrl=serverUrl;
    }


    @Override
    public void newCommunicator() {
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource(serverUrl);

        LoggerFactory.getLogger(this.getClass()).info("Check that XL Test is running");
        String xltest = service.path("data").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class).toString();
        LoggerFactory.getLogger(this.getClass()).info(xltest + "\n");

    }

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public void sendBackResults(final String suiteNames, final String replacedFitnesseRootLocation, final String replacedCallbackUri) {
        String suiteName = suiteNames;

        LOGGER.info("Starting Page history extractor...");
        LOGGER.info("Suite: " + suiteName);
        LOGGER.info("Sending events to " + replacedCallbackUri);

        try {
            PageHistoryExtractor extractor = new PageHistoryExtractor(new FeedbackEventSender(new URL(replacedCallbackUri)));
            extractor.tellMeAboutSuite(suiteName, replacedFitnesseRootLocation);
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid URL: " + serverUrl);
        } catch (Exception e) {
            LOGGER.error("Could not extract page history: " + e);
            e.printStackTrace();
        }
    }
    
    public void sendBackResultsNewStyle(String tool, String directory, String pattern, String host, String jobName) throws MalformedURLException {
        URL feedbackUrl = new URL(serverUrl + "/import/" + jobName + "?tool=" + tool + "&pattern=" + pattern + "&directory=" + directory);
    	HttpURLConnection connection = null;
    	String summary = "En dit is de content";
        try {
            byte[] data = summary.toString().getBytes("UTF-8");
            connection = (HttpURLConnection) feedbackUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8"); // application/json;charset=UTF-8
            connection.setRequestProperty("Content-Length", Integer.toString(data.length));
            OutputStream out = connection.getOutputStream();
            out.write(data);
            out.flush();
            out.close();

            // Need this to trigger the sending of the request
            int responseCode = connection.getResponseCode();

            LOGGER.info("Sent event, response code: " + responseCode + ", " + summary);

        } catch (IOException e) {
            LOGGER.error("Could not deliver page information", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
