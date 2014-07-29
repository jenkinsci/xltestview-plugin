package com.xebialabs.xltest.fitnesse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends events to XL Test with information found in the wiki pages.
 */
public class FeedbackEventSender implements Feedback {
    private static final Logger LOG = Logger.getLogger(FeedbackEventSender.class.getName());

    private final URL feedbackUrl;

    public FeedbackEventSender(URL url) {
        this.feedbackUrl = url;
    }

    @Override
    public void found(Object summary) {
        HttpURLConnection connection = null;
        try {
            byte[] data = summary.toString().getBytes("UTF-8");
            connection = (HttpURLConnection) feedbackUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Content-Length", Integer.toString(data.length));
            OutputStream out = connection.getOutputStream();
            out.write(data);
            out.flush();
            out.close();

            // Need this to trigger the sending of the request
            int responseCode = connection.getResponseCode();

            LOG.info("Sent event, response code: " + responseCode + ", " + summary);

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not deliver page information", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
