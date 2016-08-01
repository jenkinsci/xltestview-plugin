package com.xebialabs.xlt.ci.server.domain;

import com.google.common.base.Objects;

public class ServerInfo {
    private String name;
    private String vendor;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("vendor", vendor)
                .add("version", version)
                .toString();
    }
}
