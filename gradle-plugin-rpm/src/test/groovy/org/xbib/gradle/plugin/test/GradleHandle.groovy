package org.xbib.gradle.plugin.test

interface GradleHandle {

    ExecutionResult run()

    void registerBuildListener(GradleHandleBuildListener buildListener)

    boolean isForkedProcess()
}
