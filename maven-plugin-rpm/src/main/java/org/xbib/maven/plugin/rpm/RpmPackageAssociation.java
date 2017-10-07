package org.xbib.maven.plugin.rpm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RPM package association (i.e. obsoletes, dependencies, conflicts).
 */
public class RpmPackageAssociation {
    /**
     * Association name.
     */
    private String name = null;

    /**
     * Association maven style version.
     */
    private String version = null;

    /**
     * Min version requirement.
     */
    private String minVersion = null;

    /**
     * Max version requirement.
     */
    private String maxVersion = null;

    /**
     * Version requirement has range.
     */
    private boolean isRange = false;

    /**
     * Get name.
     *
     * @return Association name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set name.
     *
     * @param name Association name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Is version range.
     *
     * @return Version range, true or false
     */
    public boolean isVersionRange() {
        return isRange;
    }

    /**
     * Get version.
     *
     * @return Maven style version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set version.
     *
     * @param version Maven style version.
     */
    public void setVersion(String version) {
        if (version == null || version.equals("RELEASE") || version.equals("")) {
            isRange = false;
            this.version = null;
            minVersion = null;
            maxVersion = null;
            return;
        }
        Pattern versionPattern = Pattern.compile("\\[([0-9\\.]*),([0-9\\.]*)\\)");
        Matcher matcher = versionPattern.matcher(version);
        if (matcher.matches()) {
            this.isRange = true;
            this.version = null;
            String minVersion = matcher.group(1);
            String maxVersion = matcher.group(2);
            if (minVersion.equals("")) {
                minVersion = null;
            }
            if (maxVersion.equals("")) {
                maxVersion = null;
            }
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        } else {
            this.isRange = false;
            this.version = version;
            this.minVersion = null;
            this.maxVersion = null;
        }
    }

    /**
     * Get min version requirement.
     *
     * @return Min version requirement
     */
    public String getMinVersion() {
        return this.minVersion;
    }

    /**
     * Get max version requirement.
     *
     * @return Max version requirement
     */
    public String getMaxVersion() {
        return this.maxVersion;
    }
}
