package com.xebialabs.xltest.ci.server.domain;

import com.google.common.base.Objects;

public class Project {
    private String id;
    private String title;
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("title", title)
                .toString();
    }
}
