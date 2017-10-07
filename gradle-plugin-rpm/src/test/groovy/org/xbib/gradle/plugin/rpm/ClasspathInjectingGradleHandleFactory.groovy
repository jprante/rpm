package org.xbib.gradle.plugin.rpm;

import com.google.common.base.Predicate
import org.gradle.util.GFileUtils

class ClasspathInjectingGradleHandleFactory implements GradleHandleFactory {

    private final ClassLoader classLoader

    private final GradleHandleFactory delegateFactory

    private Predicate<URL> classpathFilter

    ClasspathInjectingGradleHandleFactory(ClassLoader classLoader, GradleHandleFactory delegateFactory,
                                          Predicate<URL> classpathFilter) {
        this.classpathFilter = classpathFilter
        this.classLoader = classLoader
        this.delegateFactory = delegateFactory
    }

    @Override
    GradleHandle start(File projectDir, List<String> arguments, List<String> jvmArguments = []) {
        File testKitDir = new File(projectDir, ".gradle-test-kit")
        if (!testKitDir.exists()) {
            GFileUtils.mkdirs(testKitDir)
        }
        File initScript = new File(testKitDir, "init.gradle");
        ClasspathAddingInitScriptBuilder.build(initScript, classLoader, classpathFilter)
        List<String> ammendedArguments = new ArrayList<String>(arguments.size() + 2)
        ammendedArguments.add("--init-script")
        ammendedArguments.add(initScript.getAbsolutePath())
        ammendedArguments.addAll(arguments)
        return delegateFactory.start(projectDir, ammendedArguments, jvmArguments)
    }
}
