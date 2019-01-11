package org.xbib.gradle.plugin.test

class DefaultGradleRunner implements GradleRunner {

    private final GradleHandleFactory handleFactory

    DefaultGradleRunner(GradleHandleFactory handleFactory) {
        this.handleFactory = handleFactory
    }

    @Override
    ExecutionResult run(File projectDir, List<String> arguments, List<String> jvmArguments = [],
                        List<PreExecutionAction> preExecutionActions = []) {
        handle(projectDir, arguments, jvmArguments, preExecutionActions).run()
    }

    @Override
    GradleHandle handle(File projectDir, List<String> arguments, List<String> jvmArguments = [],
                        List<PreExecutionAction> preExecutionActions = []) {
        preExecutionActions?.each {
            it.execute(projectDir, arguments, jvmArguments)
        }
        handleFactory.start(projectDir, arguments, jvmArguments)
    }
}
