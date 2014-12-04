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

import com.sun.jersey.core.util.Base64;
import hudson.FilePath;
import hudson.util.DirScanner;
import hudson.util.DirScanner.Glob;
import hudson.util.io.ArchiverFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class XLTestServerImpl implements XLTestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XLTestServerImpl.class);

    private String user;
    private String password;
    private String proxyUrl;
    private String serverUrl;
    private String jenkinsHost;
    private int jenkinsPort;
    

    XLTestServerImpl(String serverUrl, String proxyUrl, String username, String password, String jenkinsHost, int jenkinsPort) {
        this.user=username;
        this.password=password;
        this.proxyUrl=proxyUrl;
        this.serverUrl=serverUrl;
        this.jenkinsHost=jenkinsHost;
        this.jenkinsPort=jenkinsPort;
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
    
    public void sendBackResults(String tool, String pattern, String jobName, FilePath workspace) throws MalformedURLException {
    	URL feedbackUrl = new URL(serverUrl + "/import/" + jobName + "?tool=" + tool + "&pattern=" + pattern + "&jenkinsHost=" + jenkinsHost + "&jenkinsPort=" + jenkinsPort);

        HttpURLConnection connection = null;
        try {
        	LOGGER.info("logger: Trying to sent workspace: " + workspace.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
            connection = (HttpURLConnection) feedbackUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            String authorization = "Basic " + new String(Base64.encode((user + ":" + password).getBytes()));
            LOGGER.info("Authorization token: " + authorization);
            connection.setRequestProperty("Authorization", authorization);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/zip");
            ArchiverFactory factory = ArchiverFactory.ZIP;
            DirScanner scanner = new Glob(pattern, null); // no excludes supported (yet)
            LOGGER.info("logger: Going to scan dir: " + workspace.getRemote() + " for files to zip using pattern: " + pattern);
            
            OutputStream os = connection.getOutputStream();
            int numberOfFilesArchived = workspace.archive(factory, os, scanner);
            
            os.flush();
            os.close();

            // Need this to trigger the sending of the request
            int responseCode = connection.getResponseCode();

            LOGGER.info("Zip sent containing: " + numberOfFilesArchived +" files. Response code from XL Test was: " + responseCode);
                        
        } catch (IOException e) {
            LOGGER.error("Could not deliver page information", e);
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
