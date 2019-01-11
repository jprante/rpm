package org.xbib.gradle.plugin.test

interface GradleHandleFactory {

    GradleHandle start(File dir, List<String> arguments)

    GradleHandle start(File dir, List<String> arguments, List<String> jvmArguments)
}
