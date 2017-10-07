package org.xbib.rpm.ant;

import org.apache.tools.ant.types.EnumeratedAttribute;
import org.xbib.rpm.format.Flags;

/**
 *
 */
public class Depends {

    private String name;

    private String version = "";

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
        return this.comparison;
    }

    public void setComparison(ComparisonEnum comparisonEnum) {
        String comparisonValue = comparisonEnum.getValue();
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Enumerated attribute with the values "equal", "greater", "greater|equal", "less" and "less|equal".
     */
    public static class ComparisonEnum extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"equal", "greater", "greater|equal", "less", "less|equal"};
        }
    }
}
