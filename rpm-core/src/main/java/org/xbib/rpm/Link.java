package org.xbib.rpm;

public class Link implements Comparable<Link> {

    private final String path;

    private final String target;

    private int permissions = -1;

    public Link(String path, String target) {
        this(path, target, -1);
    }

    public Link(String path, String target, int permissions) {
        this.path = path;
        this.target = target;
        this.permissions = permissions;
    }

    public String getPath() {
        return path;
    }

    public String getTarget() {
        return target;
    }

    public int getPermissions() {
        return permissions;
    }

    @Override
    public int compareTo(Link o) {
        return (path + "->" + target).compareTo(o.path + "->" + o.target);
    }
}
