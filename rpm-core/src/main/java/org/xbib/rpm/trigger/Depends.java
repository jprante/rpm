package org.xbib.rpm.trigger;

import org.xbib.rpm.format.Flags;

/**
 * A dependency on a particular version of an RPM package.
 */
public class Depends {

    protected String name;

    protected String version = "";

    private int comparison = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getComparison() {
        if (0 == comparison && 0 < version.length()) {
            return Flags.GREATER | Flags.EQUAL;
        }
        if (0 == version.length()) {
            return 0;
        }
        return comparison;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
