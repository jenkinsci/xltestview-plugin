package com.xebialabs.xltest.ci.server.domain;


public class Qualification {

    private final String type;
    private final String name;

    public Qualification(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
