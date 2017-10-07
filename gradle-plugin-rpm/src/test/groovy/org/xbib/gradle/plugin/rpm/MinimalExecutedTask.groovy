package org.xbib.gradle.plugin.rpm

class MinimalExecutedTask implements ExecutedTask {

    String path

    boolean upToDate

    boolean skipped

    MinimalExecutedTask(String path, boolean upToDate, boolean skipped) {
        this.path = path
        this.upToDate = upToDate
        this.skipped = skipped
    }

    String toString() {
        "executed $path"
    }
}
