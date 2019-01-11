package org.xbib.gradle.plugin.test

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.base.StandardSystemProperty

interface GradleRunner {
    // These predicates are here, instead of on GradleRunnerFactory due to a Groovy static compiler bug
    // https://issues.apache.org/jira/browse/GROOVY-7159

    static final Predicate<URL> CLASSPATH_GRADLE_CACHE = new Predicate<URL>() {
        @Override
        boolean apply(URL url) {
            return url.path.contains('/caches/modules-')
        }
    }

    static final Predicate<URL> CLASSPATH_PROJECT_DIR = new Predicate<URL>() {
        @Override
        boolean apply(URL url) {
            File userDir = new File(StandardSystemProperty.USER_DIR.value())
            return url.path.startsWith(userDir.toURI().toURL().path)
        }
    }

    static final Predicate<URL> CLASSPATH_PROJECT_DEPENDENCIES = new Predicate<URL>() {
        @Override
        boolean apply(URL url) {
            return url.path.contains('/build/classes') ||
                    url.path.contains('/build/resources') ||
                    url.path.contains('/build/libs') ||
                    url.path.contains('/out/')
        }
    }

    /**
     * Attempts to provide a classpath that approximates the 'normal' Gradle runtime classpath. Use {@link #CLASSPATH_ALL}
     * to default to pre-2.2.2 behaviour.
     */
    static final Predicate<URL> CLASSPATH_DEFAULT =
            Predicates.or(CLASSPATH_PROJECT_DIR, CLASSPATH_GRADLE_CACHE, CLASSPATH_PROJECT_DEPENDENCIES)

    /**
     * Accept all URLs. Provides pre-2.2.2 behaviour.
     */
    static final Predicate<URL> CLASSPATH_ALL = new Predicate<URL>() {
        @Override
        boolean apply(URL url) {
            return true
        }
    }

    /**
     * Create handle and run build
     * @param directory
     * @param args
     * @return results from execution
     */
    ExecutionResult run(File directory, List<String> args)

    ExecutionResult run(File directory, List<String> args, List<String> jvmArgs)

    ExecutionResult run(File directory, List<String> args, List<String> jvmArgs,
                        List<PreExecutionAction> preExecutionActions)

    /**
     * Handle on instance of Gradle that can be run.
     * @param directory
     * @param args
     * @return handle
     */
    GradleHandle handle(File directory, List<String> args)

    GradleHandle handle(File directory, List<String> args, List<String> jvmArgs)

    GradleHandle handle(File directory, List<String> args, List<String> jvmArgs,
                        List<PreExecutionAction> preExecutionActions)
}