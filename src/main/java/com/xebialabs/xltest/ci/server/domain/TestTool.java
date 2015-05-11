package com.xebialabs.xltest.ci.server.domain;


import com.google.common.base.Objects;

public class TestTool {
    private String name;
    private String category;
    private String defaultSearchPattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDefaultSearchPattern() {
        return defaultSearchPattern;
    }

    public void setDefaultSearchPattern(String defaultSearchPattern) {
        this.defaultSearchPattern = defaultSearchPattern;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("category", category)
                .add("defaultSearchPattern", defaultSearchPattern)
                .toString();
    }
}
