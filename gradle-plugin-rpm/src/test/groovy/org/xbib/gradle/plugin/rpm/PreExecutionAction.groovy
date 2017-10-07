package org.xbib.gradle.plugin.rpm

interface PreExecutionAction {

    void execute(File projectDir, List<String> arguments, List<String> jvmArguments)
}
