package org.xbib.gradle.plugin

import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.AbstractArchiveTask

import java.util.concurrent.Callable

class Rpm extends AbstractArchiveTask {

    @Delegate
    @Nested
    RpmExtension rpmExtension

    Rpm() {
        rpmExtension = project.extensions.findByName('rpm') as RpmExtension
        getArchiveExtension().set("rpm")
        Callable<String> archiveFileNameProvider = new Callable<String>() {
            @Override
            String call() throws Exception {
                constructArchiveFileName()
            }
        }
        archiveFileName.set(project.provider(archiveFileNameProvider))
    }

    @Override
    protected CopyAction createCopyAction() {
        new RpmCopyAction(project, rpmExtension, this)
    }

    private String constructArchiveFileName() {
        StringBuilder sb = new StringBuilder()
        if (packageName) {
            sb.append(packageName)
        }
        if (packageVersion) {
            sb.append('-').append(packageVersion)
        }
        if (packageRelease) {
            sb.append('-').append(packageRelease)
        }
        if (arch) {
            sb.append('.').append(arch.name().toLowerCase(Locale.ROOT))
        }
        if (archiveExtension) {
            sb.append('.').append(archiveExtension.get())
        }
        sb.toString()
    }
}
