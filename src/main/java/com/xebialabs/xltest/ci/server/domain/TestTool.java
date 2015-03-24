package com.xebialabs.xltest.ci.server.domain;


public class TestTool {

    private final String pattern;
    private final String name;

    public TestTool(String name, String pattern) {
        this.name = name;
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public String getName() {
        return name;
    }

}
