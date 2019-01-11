package org.xbib.gradle.plugin.test

interface PreExecutionAction {

    void execute(File projectDir, List<String> arguments, List<String> jvmArguments)
}
