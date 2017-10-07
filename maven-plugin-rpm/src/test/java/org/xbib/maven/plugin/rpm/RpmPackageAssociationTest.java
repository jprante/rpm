package org.xbib.maven.plugin.rpm;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class RpmPackageAssociationTest {

    private RpmPackageAssociation association;

    @Before
    public void setUp() {
        association = new RpmPackageAssociation();
    }

    @Test
    public void nameAccessors() {
        assertEquals(null, association.getName());
        association.setName("testname");
        assertEquals("testname", association.getName());
    }

    @Test
    public void unassignedVersion() {
        assertEquals(null, association.getVersion());
        assertEquals(null, association.getMinVersion());
        assertEquals(null, association.getMaxVersion());
    }

    @Test
    public void latestVersion() {
        association.setVersion(null);
        assertEquals(null, association.getVersion());
        assertEquals(null, association.getMinVersion());
        assertEquals(null, association.getMaxVersion());
        association.setVersion("");
        assertEquals(null, association.getVersion());
        assertEquals(null, association.getMinVersion());
        assertEquals(null, association.getMaxVersion());
        association.setVersion("RELEASE");
        assertEquals(null, association.getVersion());
        assertEquals(null, association.getMinVersion());
        assertEquals(null, association.getMaxVersion());
    }

    @Test
    public void specificVersion() {
        association.setVersion("1.2.3");
        assertEquals("1.2.3", association.getVersion());
        assertEquals(null, association.getMinVersion());
        assertEquals(null, association.getMaxVersion());
    }

    @Test
    public void minVersionRange() {
        association.setVersion("[1.2.3,)");
        assertEquals(null, association.getVersion());
        assertEquals("1.2.3", association.getMinVersion());
        assertEquals(null, association.getMaxVersion());
    }

    @Test
    public void maxVersionRange() {
        association.setVersion("[,1.2.3)");
        assertEquals(null, association.getVersion());
        assertEquals(null, association.getMinVersion());
        assertEquals("1.2.3", association.getMaxVersion());
    }

    @Test
    public void minMaxVersionRange() {
        association.setVersion("[1.2.3,1.2.5)");
        assertEquals(null, association.getVersion());
        assertEquals("1.2.3", association.getMinVersion());
        assertEquals("1.2.5", association.getMaxVersion());
    }
}
