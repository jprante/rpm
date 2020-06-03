package org.xbib.rpm;

/**
 *
 */
public class Dependency {

    private final String packageName;

    private final String version;

    private final Integer flags;

    /**
     * Creates a new dependency.
     *
     * @param packageName    Name (e.g. "httpd")
     * @param version Version (e.g. "1.0")
     * @param flags   Flags (e.g. "GREATER | Flags.EQUAL")
     */
    public Dependency(String packageName, String version, Integer flags) {
        this.packageName = packageName;
        this.version = version;
        this.flags = flags;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public Integer getFlags() {
        return flags;
    }

}
