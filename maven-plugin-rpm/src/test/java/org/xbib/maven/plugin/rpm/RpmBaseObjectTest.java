package org.xbib.maven.plugin.rpm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public abstract class RpmBaseObjectTest {

    protected abstract RpmBaseObject getRpmBaseObject();

    @Test
    public void modeAccessors() {
        assertEquals(0644, getRpmBaseObject().getPermissionsOrDefault());
        getRpmBaseObject().setPermissions(0755);
        assertEquals(0755, getRpmBaseObject().getPermissionsOrDefault());
    }

    @Test
    public void ownerAccessors() {
        getRpmBaseObject().setOwner("");
        assertEquals("root", getRpmBaseObject().getOwnerOrDefault());
        getRpmBaseObject().setOwner(null);
        assertEquals("root", getRpmBaseObject().getOwnerOrDefault());
        assertEquals("root", getRpmBaseObject().getOwnerOrDefault());
        getRpmBaseObject().setOwner("owner");
        assertEquals("owner", getRpmBaseObject().getOwnerOrDefault());
        getRpmBaseObject().setOwner("");
        assertEquals("root", getRpmBaseObject().getOwnerOrDefault());
    }

    @Test
    public void groupAccessors() {
        getRpmBaseObject().setGroup("");
        assertEquals("root", getRpmBaseObject().getGroupOrDefault());
        getRpmBaseObject().setGroup(null);
        assertEquals("root", getRpmBaseObject().getGroupOrDefault());
        assertEquals("root", getRpmBaseObject().getGroupOrDefault());
        getRpmBaseObject().setGroup("group");
        assertEquals("group", getRpmBaseObject().getGroupOrDefault());
        getRpmBaseObject().setGroup("");
        assertEquals("root", getRpmBaseObject().getGroupOrDefault());
    }
}
