package org.xbib.rpm;

public class Directory implements Comparable<Directory> {

    private String path;

    private int permissions = -1;

    private String user = null;

    private String group = null;

    private boolean addParents = false;

    public Directory setPath(String path) {
        this.path = path;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Directory setPermissions(int permissions) {
        this.permissions = permissions;
        return this;
    }

    public int getPermissions() {
        return permissions;
    }

    public Directory setUser(String user) {
        this.user = user;
        return this;
    }

    public String getUser() {
        return user;
    }

    public Directory setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public Directory setAddParents(boolean addParents) {
        this.addParents = addParents;
        return this;
    }

    public boolean isAddParents() {
        return addParents;
    }

    @Override
    public int compareTo(Directory o) {
        return path.compareTo(o.path);
    }
}
