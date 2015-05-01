package com.xebialabs.xltest.ci.server.domain;

import com.google.common.base.Objects;

public class TestSpecification {
    private String id;
    private String title;
    private String type;

    private Project project;
    private Qualification qualification;

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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Qualification getQualification() {
        return qualification;
    }

    public void setQualification(Qualification qualification) {
        this.qualification = qualification;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("title", title)
                .add("project", project)
                .add("qualification", qualification)
                .toString();
    }
}
