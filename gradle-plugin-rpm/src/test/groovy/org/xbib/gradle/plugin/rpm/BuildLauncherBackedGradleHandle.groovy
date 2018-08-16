package org.xbib.gradle.plugin.rpm

import org.gradle.tooling.BuildException
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProgressEvent
import org.gradle.tooling.ProgressListener

class BuildLauncherBackedGradleHandle implements GradleHandle {

    final private ByteArrayOutputStream standardOutput = new ByteArrayOutputStream()

    final private ByteArrayOutputStream standardError = new ByteArrayOutputStream()

    final private BuildLauncher launcher

    final private boolean forkedProcess

    final private List<String> tasksExecuted

    public static final String PROGRESS_TASK_PREFIX = "Task :"

    private GradleHandleBuildListener buildListener

    BuildLauncherBackedGradleHandle(BuildLauncher launcher, boolean forkedProcess) {
        this.forkedProcess = forkedProcess
        launcher.setStandardOutput(standardOutput)
        launcher.setStandardError(standardError)
        tasksExecuted = new ArrayList<String>()
        launcher.addProgressListener(new ProgressListener() {
            @Override
            void statusChanged(ProgressEvent event) {
                // These are free form strings, :-(
                if (event.description.startsWith(PROGRESS_TASK_PREFIX)) { // E.g. "Task :echo"
                    String taskName = event.description.substring(PROGRESS_TASK_PREFIX.length() - 1)
                    tasksExecuted.add(taskName)
                }
            }
        })
        this.launcher = launcher
    }

    @Override
    void registerBuildListener(GradleHandleBuildListener buildListener) {
        this.buildListener = buildListener
    }

    @Override
    boolean isForkedProcess() {
        forkedProcess
    }

    private String getStandardOutput() {
        return standardOutput.toString()
    }

    private String getStandardError() {
        return standardError.toString()
    }

    @Override
    ExecutionResult run() {
        Throwable failure = null
        try {
            buildListener?.buildStarted()
            launcher.run()
        } catch(BuildException e) {
            failure = e.getCause()
        } catch(Exception e) {
            failure = e
        }
        finally {
            buildListener?.buildFinished()
        }
        String stdout = getStandardOutput()
        List<MinimalExecutedTask> tasks = new ArrayList<>()
        for (String taskName: tasksExecuted) {
            boolean upToDate = isTaskUpToDate(stdout, taskName)
            boolean skipped = isTaskSkipped(stdout, taskName)
            tasks.add(new MinimalExecutedTask(taskName, upToDate, skipped))
        }
        boolean success = failure == null
        new ToolingExecutionResult(success, stdout, getStandardError(), tasks, failure)
    }

    private isTaskUpToDate(String stdout, String taskName) {
        containsOutput(stdout, taskName, 'UP-TO-DATE')
    }

    private isTaskSkipped(String stdout, String taskName) {
        containsOutput(stdout, taskName, 'SKIPPED')
    }

    private boolean containsOutput(String stdout, String taskName, String stateIdentifier) {
        stdout.contains("$taskName $stateIdentifier".toString())
    }
}
