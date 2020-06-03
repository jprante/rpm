package org.xbib.rpm.trigger;

import org.xbib.rpm.format.Flags;

/**
 * A dependency on a particular version of an RPM package.
 */
public class Depends {

    protected String name;

    protected String version = "";

    protected int comparison = 0;

    public void setName( String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setComparison(String comparisonValue) {
        if ("equal".equals(comparisonValue)) {
            this.comparison = Flags.EQUAL;
        } else if ("greater".equals(comparisonValue)) {
            this.comparison = Flags.GREATER;
        } else if ("greater|equal".equals(comparisonValue)) {
            this.comparison = Flags.GREATER | Flags.EQUAL;
        } else if ("less".equals(comparisonValue)) {
            this.comparison = Flags.LESS;
        } else {
            this.comparison = Flags.LESS | Flags.EQUAL;
        }
    }

    public int getComparison() {
        if ( 0 == comparison && 0 < version.length()) {
            return Flags.GREATER | Flags.EQUAL;
        }
        if ( 0 == version.length()) {
            return 0;
        }
        return this.comparison;
    }

    public void setVersion( String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
