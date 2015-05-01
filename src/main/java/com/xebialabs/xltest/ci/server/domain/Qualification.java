package com.xebialabs.xltest.ci.server.domain;


import com.google.common.base.Objects;

public class Qualification {
    private String type;
    private String name;
    private String description;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("name", name)
                .add("description", description)
                .toString();
    }
}
