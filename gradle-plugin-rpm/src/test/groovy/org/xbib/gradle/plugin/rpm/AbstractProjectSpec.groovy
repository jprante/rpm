package org.xbib.gradle.plugin.rpm

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

abstract class AbstractProjectSpec extends Specification {

    static final String CLEAN_PROJECT_DIR_SYS_PROP = 'cleanProjectDir'

    File ourProjectDir

    @Rule TestName testName = new TestName()

    String canonicalName

    Project project

    MultiProjectHelper helper

    void setup() {
        ourProjectDir = new File("build/nebulatest/${this.class.canonicalName}/${testName.methodName.replaceAll(/\W+/, '-')}").absoluteFile
        if (ourProjectDir.exists()) {
            ourProjectDir.deleteDir()
        }
        ourProjectDir.mkdirs()
        canonicalName = testName.getMethodName().replaceAll(' ', '-')
        project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(ourProjectDir).build()
        helper = new MultiProjectHelper(project)
    }

    void cleanup() {
        if (deleteProjectDir()) {
            ourProjectDir.deleteDir()
        }
    }

    boolean deleteProjectDir() {
        String cleanProjectDirSystemProperty = System.getProperty(CLEAN_PROJECT_DIR_SYS_PROP)
        cleanProjectDirSystemProperty ? cleanProjectDirSystemProperty.toBoolean() : true
    }

    Project addSubproject(String subprojectName) {
        helper.addSubproject(subprojectName)
    }

    Project addSubprojectWithDirectory(String subprojectName) {
        helper.addSubprojectWithDirectory(subprojectName)
    }
}
