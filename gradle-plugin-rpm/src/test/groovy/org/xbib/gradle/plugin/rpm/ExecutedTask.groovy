package org.xbib.gradle.plugin.rpm

interface ExecutedTask {

    String getPath()

    boolean isUpToDate()

    boolean isSkipped()
}
