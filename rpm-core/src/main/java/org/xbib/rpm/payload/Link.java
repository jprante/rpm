package org.xbib.rpm.payload;

/**
 * Object describing a symbolic link to be generated on the target machine during installation of the RPM archive.
 */
public class Link {

    private String path;

    private String target;

    private int permissions = -1;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }
}
