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

import hudson.FilePath;
import hudson.util.DirScanner;
import hudson.util.DirScanner.Glob;
import hudson.util.FileVisitor;
import hudson.util.io.ArchiverFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
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
    
    public void sendBackResults(String tool, String directory, String pattern, String jobName, FilePath workspace) throws MalformedURLException {
    	
    	URL feedbackUrl = new URL(serverUrl + "/import/" + jobName + "?tool=" + tool + "&pattern=" + pattern + "&directory=" + directory);
		
    	FilePath workspacePartToSent = workspace;
    	if (directory != null && !"".equals(directory)) {
    		System.out.println("changing the workspace to a subdir of the workspace");
    		LOGGER.info("logger: changing the workspace to a subdir of the workspace");
    		workspacePartToSent = new FilePath(workspace, directory);
    	}
        HttpURLConnection connection = null;
        try {
        	LOGGER.info("logger: Trying to sent workspace: " + workspacePartToSent.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
        	System.out.println("Trying to sent workspace: " + workspacePartToSent.toURI().toString() + " to XL Test on URL: " + feedbackUrl.toString());
            //byte[] data = summary.toString().getBytes("UTF-8");
            connection = (HttpURLConnection) feedbackUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/zip");
            //connection.setRequestProperty("Content-Length", Integer.toString(data.length));
            ArchiverFactory factory = ArchiverFactory.ZIP;
            DirScanner scanner = new Glob(pattern, null);
            File dirToScan = new File(workspacePartToSent.getRemote());
            System.out.println("Going to scan dir: " + dirToScan + " for files to zip using pattern: " + pattern);
            LOGGER.info("logger: Going to scan dir: " + dirToScan + " for files to zip using pattern: " + pattern);

			
			
			
			
			scanner.scan(dirToScan, new FileVisitor() {
                @Override public void visit(File f, String relativePath) throws IOException {
                	System.out.println("scanner looked at file: " + f.getAbsolutePath());
                	LOGGER.info("logger:scanner looked at file: " + f.getAbsolutePath());
                }
            });
            
            
            OutputStream os = connection.getOutputStream();
            int numberOfFilesArchived = workspacePartToSent.archive(factory, os, scanner);
            
            os.flush();
            os.close();

            // Need this to trigger the sending of the request
            int responseCode = connection.getResponseCode();

            LOGGER.info("Zip sent containing: " + numberOfFilesArchived +" files. Response code from XL Test was: " + responseCode);
            System.out.println("Zip sent containing: " + numberOfFilesArchived +" files. Response code from XL Test was: " + responseCode);

            
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
    
    public void sendFile(HttpServletResponse response, String fileName)  
    {  
       response.setContentType("application/zip");  
       response.setContentLength(2048);  
       response.setHeader("Content-Disposition","attachment;filename=\"" + fileName + "\"");  
       try  
       {  
  
          ByteArrayOutputStream baos = new ByteArrayOutputStream();  
          ZipOutputStream zos = new ZipOutputStream(baos);  
          byte bytes[] = new byte[2048];  
  
          FileInputStream fis = new FileInputStream(fileName);  
          BufferedInputStream bis = new BufferedInputStream(fis);  
          zos.putNextEntry(new ZipEntry(fileName));  
          int bytesRead;  
          while ((bytesRead = bis.read(bytes)) != -1)  
          {  
            zos.write(bytes, 0, bytesRead);  
          }  
  
           zos.closeEntry();  
           bis.close();  
           fis.close();  
  
           zos.flush();  
           baos.flush();  
           zos.close();  
           baos.close();  
  
           ServletOutputStream op = response.getOutputStream();  
           op.write(baos.toByteArray());  
           op.flush();  
             
       }catch(IOException ioe)  
       {  
           ioe.printStackTrace();  
       }  
    } 

}
