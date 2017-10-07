package org.xbib.gradle.plugin.rpm

interface GradleHandleFactory {

    GradleHandle start(File dir, List<String> arguments)

    GradleHandle start(File dir, List<String> arguments, List<String> jvmArguments)
}
