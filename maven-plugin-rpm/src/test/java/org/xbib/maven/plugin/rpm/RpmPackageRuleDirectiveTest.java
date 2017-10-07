package org.xbib.maven.plugin.rpm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.xbib.rpm.exception.InvalidDirectiveException;
import org.xbib.rpm.payload.Directive;

import java.util.EnumSet;

/**
 *
 */
public class RpmPackageRuleDirectiveTest {

    @Test
    public void configDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.CONFIG);
        assertTrue(directiveList.contains(Directive.CONFIG));
        assertFalse(directiveList.contains(Directive.DOC));
    }

    @Test
    public void docDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.DOC);
        assertTrue(directiveList.contains(Directive.DOC));
        assertFalse(directiveList.contains(Directive.ICON));
    }

    @Test
    public void iconDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.ICON);
        assertTrue(directiveList.contains(Directive.ICON));
        assertFalse(directiveList.contains(Directive.MISSINGOK));
    }

    @Test
    public void missingOkDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.MISSINGOK);
        assertTrue(directiveList.contains(Directive.MISSINGOK));
        assertFalse(directiveList.contains(Directive.NOREPLACE));
    }

    @Test
    public void noReplaceDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.NOREPLACE);
        assertTrue(directiveList.contains(Directive.NOREPLACE));
        assertFalse(directiveList.contains(Directive.SPECFILE));
    }

    @Test
    public void specFileDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.SPECFILE);
        assertTrue(directiveList.contains(Directive.SPECFILE));
        assertFalse(directiveList.contains(Directive.GHOST));
    }

    @Test
    public void ghostDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.GHOST);
        assertTrue(directiveList.contains(Directive.GHOST));
        assertFalse(directiveList.contains(Directive.LICENSE));
    }

    @Test
    public void licenseDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.LICENSE);
        assertTrue(directiveList.contains(Directive.LICENSE));
        assertFalse(directiveList.contains(Directive.README));
    }

    @Test
    public void readmeDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.README);
        assertTrue(directiveList.contains(Directive.README));
        assertFalse(directiveList.contains(Directive.EXCLUDE));
    }

    @Test
    public void excludeDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.EXCLUDE);
        assertTrue(directiveList.contains(Directive.EXCLUDE));
        assertFalse(directiveList.contains(Directive.UNPATCHED));
    }

    @Test
    public void unpatchedDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.UNPATCHED);
        assertTrue(directiveList.contains(Directive.UNPATCHED));
        assertFalse(directiveList.contains(Directive.POLICY));
    }

    @Test
    public void pubkeyDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.PUBKEY);
        assertTrue(directiveList.contains(Directive.PUBKEY));
        assertFalse(directiveList.contains(Directive.POLICY));
    }

    @Test
    public void policyDirective() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.POLICY);
        assertTrue(directiveList.contains(Directive.POLICY));
    }

    @Test
    public void multipleDirectives() throws InvalidDirectiveException {
        EnumSet<Directive> directiveList = EnumSet.of(Directive.NONE);
        directiveList.add(Directive.CONFIG);
        directiveList.add(Directive.NOREPLACE);
        directiveList.add(Directive.LICENSE);
        directiveList.add(Directive.README);
        assertTrue(directiveList.contains(Directive.CONFIG));
        assertTrue(directiveList.contains(Directive.NOREPLACE));
        assertTrue(directiveList.contains(Directive.LICENSE));
        assertTrue(directiveList.contains(Directive.README));
        assertFalse(directiveList.contains(Directive.UNPATCHED));
        assertFalse(directiveList.contains(Directive.PUBKEY));
        assertFalse(directiveList.contains(Directive.POLICY));
        assertFalse(directiveList.contains(Directive.DOC));
    }
}
