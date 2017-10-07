package org.xbib.gradle.plugin.rpm;

interface ExecutionResult {

    Boolean getSuccess()

    String getStandardOutput()

    String getStandardError()

    boolean wasExecuted(String taskPath)

    boolean wasUpToDate(String taskPath)

    boolean wasSkipped(String taskPath)

    Throwable getFailure()

    ExecutionResult rethrowFailure()
}
