package org.xbib.gradle.plugin.test

interface ExecutedTask {

    String getPath()

    boolean isUpToDate()

    boolean isSkipped()
}
