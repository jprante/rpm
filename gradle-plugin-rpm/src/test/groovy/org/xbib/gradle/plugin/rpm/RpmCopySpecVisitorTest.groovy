package org.xbib.gradle.plugin.rpm

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class RpmCopySpecVisitorTest extends ProjectSpec {

    RpmCopyAction visitor

    @Before
    void setup() {
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            packageName = 'can-execute-rpm-task-with-valid-version'
        }
        visitor = new RpmCopyAction(rpmTask)
    }

    @Test
    void withoutUtils() {
        visitor.includeStandardDefines = false
        File script = resourceFile("script.sh")
        Object result = visitor.scriptWithUtils([], [script])
        assertTrue result instanceof String
        assertEquals(
            "#!/bin/bash\n" +
            "hello\n", result)
    }

    @Test
    void withUtils() {
        visitor.includeStandardDefines = false
        Object result = visitor.scriptWithUtils([resourceFile("utils.sh")], [resourceFile("script.sh")])
        assertTrue result instanceof String
        assertEquals(
            "#!/bin/bash\n" +
            "function hello() {\n" +
            "    echo 'Hello, world.'\n" +
            "}\n" +
            "hello\n", result)
    }

    File resourceFile(String name) {
        new File(getClass().getResource(name).getPath())
    }
}
