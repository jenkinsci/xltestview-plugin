package com.xebialabs.xltest.fitnesse;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Sends events to XL Test with information found in the wiki pages.
 */
public class FeedbackEventPrinter implements Feedback {
    private static final Logger LOG = Logger.getLogger(FeedbackEventPrinter.class.getName());

    private final URL feedbackUrl;

    public FeedbackEventPrinter(URL url) {
        this.feedbackUrl = url;
    }

    @Override
    public void found(Object summary) {
    	String json = summary.toString();
    	System.out.println("should send: " + json);
    }
}
