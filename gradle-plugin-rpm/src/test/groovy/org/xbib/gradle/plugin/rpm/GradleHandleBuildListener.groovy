package org.xbib.gradle.plugin.rpm

interface GradleHandleBuildListener {

    void buildStarted()

    void buildFinished()
}
