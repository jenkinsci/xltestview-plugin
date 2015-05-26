package com.xebialabs.xltest.ci.server.domain;

import com.google.common.base.Objects;

public class ImportError {
    private String message;
    private String status;
    private String errorCode;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("message", message)
                .add("status", status)
                .add("errorCode", errorCode)
                .toString();
    }

}
