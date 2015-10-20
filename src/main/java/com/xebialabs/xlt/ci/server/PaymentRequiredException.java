package com.xebialabs.xlt.ci.server;

public class PaymentRequiredException extends IllegalStateException {

    public PaymentRequiredException(final String s) {
        super(s);
    }
}
