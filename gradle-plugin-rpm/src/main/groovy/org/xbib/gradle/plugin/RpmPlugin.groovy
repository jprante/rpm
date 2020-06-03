package org.xbib.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion

class RpmPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        checkVersion()
        project.plugins.apply(BasePlugin)
        project.ext.Rpm = Rpm.class
        project.with {
            createExtension(project)
        }
        /*project.afterEvaluate {
            addTasks(project)
        }*/
    }

    private static void checkVersion() {
        String version = '6.4'
        if (GradleVersion.current() < GradleVersion.version(version)) {
            throw new GradleException("need Gradle ${version} or higher")
        }
    }

    private static void createExtension(Project project) {
        project.extensions.create('rpm', RpmExtension)
    }

    /*private static void addTasks(Project project) {
        TaskProvider<Rpm> taskProvider = project.tasks.register('buildRpm', Rpm) {
            group = 'rpm'
            description = 'Generate RPM archive'
        }
        project.tasks.findByName('build').dependsOn taskProvider
    }*/
}
