package org.xbib.gradle.plugin.rpm

interface GradleHandle {

    ExecutionResult run()

    void registerBuildListener(GradleHandleBuildListener buildListener)

    boolean isForkedProcess()
}
