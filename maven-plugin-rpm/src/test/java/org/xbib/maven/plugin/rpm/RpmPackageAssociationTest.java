package org.xbib.maven.plugin.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class RpmPackageAssociationTest {

    private RpmPackageAssociation association;

    @BeforeEach
    public void setUp() {
        association = new RpmPackageAssociation();
    }

    @Test
    public void nameAccessors() {
        assertNull(association.getName());
        association.setName("testname");
        assertEquals("testname", association.getName());
    }

    @Test
    public void unassignedVersion() {
        assertNull(association.getVersion());
        assertNull(association.getMinVersion());
        assertNull(association.getMaxVersion());
    }

    @Test
    public void latestVersion() {
        association.setVersion(null);
        assertNull(association.getVersion());
        assertNull(association.getMinVersion());
        assertNull(association.getMaxVersion());
        association.setVersion("");
        assertNull(association.getVersion());
        assertNull(association.getMinVersion());
        assertNull(association.getMaxVersion());
        association.setVersion("RELEASE");
        assertNull(association.getVersion());
        assertNull(association.getMinVersion());
        assertNull(association.getMaxVersion());
    }

    @Test
    public void specificVersion() {
        association.setVersion("1.2.3");
        assertEquals("1.2.3", association.getVersion());
        assertNull(association.getMinVersion());
        assertNull(association.getMaxVersion());
    }

    @Test
    public void minVersionRange() {
        association.setVersion("[1.2.3,)");
        assertNull(association.getVersion());
        assertEquals("1.2.3", association.getMinVersion());
        assertNull(association.getMaxVersion());
    }

    @Test
    public void maxVersionRange() {
        association.setVersion("[,1.2.3)");
        assertNull(association.getVersion());
        assertNull(association.getMinVersion());
        assertEquals("1.2.3", association.getMaxVersion());
    }

    @Test
    public void minMaxVersionRange() {
        association.setVersion("[1.2.3,1.2.5)");
        assertNull(association.getVersion());
        assertEquals("1.2.3", association.getMinVersion());
        assertEquals("1.2.5", association.getMaxVersion());
    }
}
