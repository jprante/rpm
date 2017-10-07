package org.xbib.gradle.plugin.rpm;

import org.gradle.api.GradleException

abstract class DefaultExecutionResult implements ExecutionResult {

    private final Boolean success

    private final String standardOutput

    private final String standardError

    private final List<? extends ExecutedTask> executedTasks

    private final Throwable failure

    DefaultExecutionResult(Boolean success, String standardOutput, String standardError,
                           List<? extends ExecutedTask> executedTasks, Throwable failure) {
        this.success = success
        this.standardOutput = standardOutput
        this.standardError = standardError
        this.executedTasks = executedTasks
        this.failure = failure
    }

    @Override
    Boolean getSuccess() {
        success
    }

    @Override
    String getStandardOutput() {
        standardOutput
    }

    @Override
    String getStandardError() {
        standardError
    }

    @Override
    boolean wasExecuted(String taskPath) {
        executedTasks.any { ExecutedTask task ->
            taskPath = normalizeTaskPath(taskPath)
            def match = task.path == taskPath
            return match
        }
    }

    @Override
    boolean wasUpToDate(String taskPath) {
        getExecutedTaskByPath(taskPath).upToDate
    }

    @Override
    boolean wasSkipped(String taskPath) {
        getExecutedTaskByPath(taskPath).skipped
    }

    String normalizeTaskPath(String taskPath) {
        taskPath.startsWith(':') ? taskPath : ":$taskPath"
    }

    private ExecutedTask getExecutedTaskByPath(String taskPath) {
        taskPath = normalizeTaskPath(taskPath)
        def task = executedTasks.find { ExecutedTask task ->
            task.path == taskPath
        }
        if (task == null) {
            throw new RuntimeException("Task with path $taskPath was not found")
        }
        task
    }

    @Override
    Throwable getFailure() {
        failure
    }

    @Override
    ExecutionResult rethrowFailure() {
        if (failure instanceof GradleException) {
            throw (GradleException) failure
        }
        if (failure != null) {
            throw new GradleException("Build aborted because of an internal error.", failure)
        }
        this
    }
}
