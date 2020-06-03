package org.xbib.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertEquals

class RpmPluginIntegrationTest {

    private File projectDir

    private File settingsFile

    private File buildFile

    @BeforeEach
    void setup(@TempDir File testProjectDir) throws IOException {
        this.projectDir = testProjectDir
        this.settingsFile = new File(testProjectDir, "settings.gradle")
        this.buildFile = new File(testProjectDir, "build.gradle")
    }

    @Test
    void testPlugin() {
        String settingsFileContent = '''
rootProject.name = 'rpm-test'
'''
        settingsFile.write(settingsFileContent)
        String buildFileContent = '''
plugins {
    id 'org.xbib.gradle.plugin.rpm'
}

rpm {
  enabled = true
}

task myRpm(type: Rpm) {
    packageName = 'rpmIsUpToDate'
    arch = org.xbib.rpm.lead.Architecture.NOARCH
    os = org.xbib.rpm.lead.Os.LINUX
}
'''
        buildFile.write(buildFileContent)
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(":build", "--info")
                .withPluginClasspath()
                .forwardOutput()
                .build()
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":build").getOutcome())
    }
}
