package com.xebialabs.xlt.ci.server.domain;

import com.google.common.base.Objects;

public class TestSpecification {
    private String id;
    private String title;
    private String type;

    private Project project;
    private Qualification qualification;
    private TestTool testTool;

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

    public TestTool getTestTool() {
        return testTool;
    }

    public void setTestTool(TestTool testTool) {
        this.testTool = testTool;
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

    public String getQualificationDescription() {
        if (qualification != null) {
            return qualification.getDescription();
        }
        return "no qualification present";
    }

    public String getTestToolName() {
        if (testTool != null) {
            return testTool.getName();
        }
        return "Unknown test tool";
    }

    public String getTestToolDefaultSearchPattern() {
        if (testTool != null) {
            return testTool.getDefaultSearchPattern();
        }
        return "Unknown search pattern";
    }
}
