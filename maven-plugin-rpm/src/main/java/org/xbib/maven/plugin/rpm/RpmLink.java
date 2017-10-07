package org.xbib.maven.plugin.rpm;

/**
 *
 */
public class RpmLink extends RpmBaseObject {

    private RpmPackage rpmPackage;

    private String path;

    private String target;

    /**
     * Get associated RPM package.
     *
     * @return RPM package
     */
    @Override
    public RpmPackage getPackage() {
        return this.rpmPackage;
    }

    /**
     * Set associated RPM package.
     *
     * @param rpmPackage RPM package
     */
    public void setPackage(RpmPackage rpmPackage) {
        this.rpmPackage = rpmPackage;
    }

    /**
     * Get symlink path.
     *
     * @return symlink path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set symlink path.
     *
     * @param path symlink path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get symlink target.
     *
     * @return symlink target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Set symlink target.
     *
     * @param target symlink target
     */
    public void setTarget(String target) {
        this.target = target;
    }
}
