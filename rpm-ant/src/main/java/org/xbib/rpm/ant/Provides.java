package org.xbib.rpm.ant;

/**
 * Object describing a provided capability (virtual package).
 */
public class Provides {

    private String name;

    private String version = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
