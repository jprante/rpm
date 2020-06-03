package org.xbib.maven.plugin.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RpmTriggerTest {

    @Test
    public void accessors() {
        List<RpmPackageAssociation> dependencies = new ArrayList<>();
        RpmTrigger trigger = new RpmTrigger();
        Path triggerScript = Paths.get("/path/to/file");
        assertNull(trigger.getPreInstallScriptPath());
        trigger.setPreInstallScriptPath(triggerScript);
        assertEquals(triggerScript, trigger.getPreInstallScriptPath());
        assertNull(trigger.getPreInstallProgram());
        trigger.setPreInstallProgram("/bin/sh");
        assertEquals("/bin/sh", trigger.getPreInstallProgram());
        assertNull(trigger.getPostInstallScriptPath());
        trigger.setPostInstallScriptPath(triggerScript);
        assertEquals(triggerScript, trigger.getPostInstallScriptPath());
        assertNull(trigger.getPostInstallProgram());
        trigger.setPostInstallProgram("/bin/sh");
        assertEquals("/bin/sh", trigger.getPostInstallProgram());
        assertNull(trigger.getPreUninstallScriptPath());
        trigger.setPreUninstallScriptPath(triggerScript);
        assertEquals(triggerScript, trigger.getPreUninstallScriptPath());
        assertNull(trigger.getPreUninstallProgram());
        trigger.setPreUninstallProgram("/bin/sh");
        assertEquals("/bin/sh", trigger.getPreUninstallProgram());
        assertNull(trigger.getPostUninstallScriptPath());
        trigger.setPostUninstallScriptPath(triggerScript);
        assertEquals(triggerScript, trigger.getPostUninstallScriptPath());
        assertNull(trigger.getPostUninstallProgram());
        trigger.setPostUninstallProgram("/bin/sh");
        assertEquals("/bin/sh", trigger.getPostUninstallProgram());
        trigger.setDependencies(dependencies);
        assertEquals(dependencies, trigger.getDependencies());
    }
}
