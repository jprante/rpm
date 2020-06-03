package org.xbib.rpm.payload;

import org.xbib.rpm.exception.InvalidDirectiveException;

import java.util.EnumSet;
import java.util.List;

/**
 * Directive.
 */
public enum Directive {

    NONE(0),

    CONFIG(1),

    DOC(1 << 1),

    ICON(1 << 2),

    MISSINGOK(1 << 3),

    NOREPLACE(1 << 4),

    SPECFILE(1 << 5),

    GHOST(1 << 6),

    LICENSE(1 << 7),

    README(1 << 8),

    EXCLUDE(1 << 9),

    UNPATCHED(1 << 10),

    PUBKEY(1 << 11),

    POLICY(1 << 12);

    private final int flag;

    Directive(final int flag) {
        this.flag = flag;
    }

    public int flag() {
        return flag;
    }

    /**
     * Return a new directive set.
     *
     * @param directiveList directive list
     * @return set of directives
     * @throws InvalidDirectiveException if a directive is unknown
     */
    public static EnumSet<Directive> newDirective(List<String> directiveList) throws InvalidDirectiveException {
        EnumSet<Directive> rpmDirective = EnumSet.of(Directive.NONE);
        for (String directive : directiveList) {
            switch (directive.toLowerCase()) {
                case "config":
                    rpmDirective.add(Directive.CONFIG);
                    break;
                case "doc":
                    rpmDirective.add(Directive.DOC);
                    break;
                case "icon":
                    rpmDirective.add(Directive.ICON);
                    break;
                case "missingok":
                    rpmDirective.add(Directive.MISSINGOK);
                    break;
                case "noreplace":
                    rpmDirective.add(Directive.NOREPLACE);
                    break;
                case "specfile":
                    rpmDirective.add(Directive.SPECFILE);
                    break;
                case "ghost":
                    rpmDirective.add(Directive.GHOST);
                    break;
                case "license":
                    rpmDirective.add(Directive.LICENSE);
                    break;
                case "readme":
                    rpmDirective.add(Directive.README);
                    break;
                case "unpatched":
                    rpmDirective.add(Directive.UNPATCHED);
                    break;
                case "pubkey":
                    rpmDirective.add(Directive.PUBKEY);
                    break;
                case "policy":
                    rpmDirective.add(Directive.POLICY);
                    break;
                default:
                    throw new InvalidDirectiveException(directive);
            }
        }
        return rpmDirective;
    }
}
