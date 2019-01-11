package org.xbib.gradle.plugin.test

import org.xbib.gradle.plugin.test.ExecutedTask

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
