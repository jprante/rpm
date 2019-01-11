package org.xbib.gradle.plugin.test

import com.google.common.base.Predicate
import org.gradle.api.logging.LogLevel

abstract class IntegrationSpec extends BaseIntegrationSpec {

    private static final String DEFAULT_REMOTE_DEBUG_JVM_ARGUMENTS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

    private static final Integer DEFAULT_DAEMON_MAX_IDLE_TIME_IN_SECONDS_IN_MEMORY_SAFE_MODE = 15;

    private ExecutionResult result

    protected String gradleVersion

    protected LogLevel logLevel = LogLevel.INFO

    protected String moduleName

    protected File settingsFile

    protected File buildFile

    protected boolean fork = false

    protected boolean remoteDebug = false

    protected List<String> jvmArguments = []

    protected Predicate<URL> classpathFilter

    protected List<File> initScripts = []

    protected List<PreExecutionAction> preExecutionActions = []

    //Shutdown Gradle daemon after a few seconds to release memory. Useful for testing with multiple Gradle versions on shared CI server
    protected boolean memorySafeMode = false

    protected Integer daemonMaxIdleTimeInSecondsInMemorySafeMode = DEFAULT_DAEMON_MAX_IDLE_TIME_IN_SECONDS_IN_MEMORY_SAFE_MODE

    String findModuleName() {
         getProjectDir().getName().replaceAll(/_\d+/, '')
    }

    def setup() {
        moduleName = findModuleName()
        if (!settingsFile) {
            settingsFile = new File(getProjectDir(), 'settings.gradle')
            settingsFile.text = "rootProject.name='${moduleName}'\n"
        }
        if (!buildFile) {
            buildFile = new File(getProjectDir(), 'build.gradle')
        }
        buildFile << "// Running test for ${moduleName}\n"
    }

    protected GradleHandle launcher(String... args) {
        List<String> arguments = calculateArguments(args)
        List<String> jvmArguments = calculateJvmArguments()
        Integer daemonMaxIdleTimeInSeconds = calculateMaxIdleDaemonTimeoutInSeconds()
        GradleRunner runner = GradleRunnerFactory.createTooling(fork, gradleVersion, daemonMaxIdleTimeInSeconds, classpathFilter)
        runner.handle(getProjectDir(), arguments, jvmArguments, preExecutionActions)
    }

    List<String> calculateArguments(String... args) {
        List<String> arguments = []
        // Gradle will use these files name from the PWD, instead of the project directory. It's easier to just leave
        // them out and let the default find them, since we're not changing their default names.
        //arguments += '--build-file'
        //arguments += (buildFile.canonicalPath - projectDir.canonicalPath).substring(1)
        //arguments += '--settings-file'
        //arguments += (settingsFile.canonicalPath - projectDir.canonicalPath).substring(1)
        //arguments += '--no-daemon'

        switch (getLogLevel()) {
            case LogLevel.INFO:
                arguments += '--info'
                break
            case LogLevel.DEBUG:
                arguments += '--debug'
                break
        }
        arguments += '--stacktrace'
        arguments.addAll(args)
        arguments.addAll(initScripts.collect { file -> '-I' + file.absolutePath })
        arguments
    }

    private List<String> calculateJvmArguments() {
        return jvmArguments + (remoteDebug ? [DEFAULT_REMOTE_DEBUG_JVM_ARGUMENTS] : [] as List) as List
    }

    private Integer calculateMaxIdleDaemonTimeoutInSeconds() {
        return memorySafeMode ? daemonMaxIdleTimeInSecondsInMemorySafeMode : null
    }

    protected void addInitScript(File initFile) {
        initScripts.add(initFile)
    }

    protected void addPreExecute(PreExecutionAction preExecutionAction) {
        preExecutionActions.add(preExecutionAction)
    }

    /**
     * Override to alter its value
     * @return
     */
    protected LogLevel getLogLevel() {
        return logLevel
    }

    /*protected void copyResources(String srcDir, String destination) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(srcDir);
        if (resource == null) {
            throw new RuntimeException("Could not find classpath resource: $srcDir")
        }
        File destinationFile = file(destination)
        File resourceFile = new File(resource.toURI())
        if (resourceFile.file) {
            FileUtils.copyFile(resourceFile, destinationFile)
        } else {
            FileUtils.copyDirectory(resourceFile, destinationFile)
        }
    }*/

    protected String applyPlugin(Class pluginClass) {
        "apply plugin: $pluginClass.name"
    }

    /* Checks */
    protected boolean fileExists(String path) {
        new File(projectDir, path).exists()
    }

    @Deprecated
    protected boolean wasExecuted(String taskPath) {
        result.wasExecuted(taskPath)
    }

    @Deprecated
    protected boolean wasUpToDate(String taskPath) {
        result.wasUpToDate(taskPath)
    }

    @Deprecated
    protected String getStandardError() {
        result.standardError
    }

    @Deprecated
    protected String getStandardOutput() {
        result.standardOutput
    }

    protected ExecutionResult runTasksSuccessfully(String... tasks) {
        ExecutionResult result = runTasks(tasks)
        if (result.failure) {
            result.rethrowFailure()
        }
        result
    }

    protected ExecutionResult runTasksWithFailure(String... tasks) {
        ExecutionResult result = runTasks(tasks)
        assert result.failure
        result
    }

    protected ExecutionResult runTasks(String... tasks) {
        ExecutionResult result = launcher(tasks).run()
        this.result = result
        return checkForDeprecations(result)
    }

    protected ExecutionResult checkForDeprecations(ExecutionResult result) {
        checkForDeprecations(result.standardOutput)
        return result
    }

    File getSettingsFile() {
        return settingsFile
    }
}
