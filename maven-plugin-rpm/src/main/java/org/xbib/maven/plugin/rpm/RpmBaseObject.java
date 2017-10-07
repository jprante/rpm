package org.xbib.maven.plugin.rpm;

/**
 *
 */
public abstract class RpmBaseObject {
    /**
     * Destination permissions.
     */
    private int permissions = 0;

    /**
     * Destination file owner.
     */
    private String owner = null;

    /**
     * Destination file group.
     */
    private String group = null;

    protected abstract RpmPackage getPackage();

    /**
     * Set permissions.
     *
     * @param permissions permissions
     */
    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    /**
     * Get permissions, or the default setting if not set.
     *
     * @return permissions
     */
    public int getPermissionsOrDefault() {
        if (0 == permissions) {
            return getPackage().getMojo().getDefaultFileMode();
        } else {
            return permissions;
        }
    }

    /**
     * Set owner.
     *
     * @param owner owner
     */
    public void setOwner(String owner) {
        if (null != owner && owner.equals("")) {
            owner = null;
        }
        this.owner = owner;
    }

    /**
     * Get owner, or the default setting if not set.
     *
     * @return  owner
     */
    public String getOwnerOrDefault() {
        if (null == this.owner) {
            return this.getPackage().getMojo().getDefaultOwner();
        } else {
            return this.owner;
        }
    }

    /**
     * Set group.
     *
     * @param group group
     */
    public void setGroup(String group) {
        if (null != group && group.equals("")) {
            group = null;
        }
        this.group = group;
    }

    /**
     * Get group, or the default setting if not set.
     *
     * @return group
     */
    public String getGroupOrDefault() {
        if (null == this.group) {
            return this.getPackage().getMojo().getDefaultGroup();
        } else {
            return this.group;
        }
    }
}
