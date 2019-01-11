package org.xbib.gradle.plugin.test

import com.google.common.base.Predicate

class GradleRunnerFactory {

    static GradleRunner createTooling(boolean fork = false, String version = null,
                                      Integer daemonMaxIdleTimeInSeconds = null,
                                      Predicate<URL> classpathFilter = null) {
        GradleHandleFactory toolingApiHandleFactory =
                new ToolingApiGradleHandleFactory(fork, version, daemonMaxIdleTimeInSeconds)
        return create(toolingApiHandleFactory, classpathFilter ?: GradleRunner.CLASSPATH_DEFAULT)
    }

    static GradleRunner create(GradleHandleFactory handleFactory, Predicate<URL> classpathFilter = null) {
        ClassLoader sourceClassLoader = GradleRunnerFactory.class.getClassLoader()
        create(handleFactory, sourceClassLoader, classpathFilter ?: GradleRunner.CLASSPATH_DEFAULT)
    }

    static GradleRunner create(GradleHandleFactory handleFactory, ClassLoader sourceClassLoader,
                               Predicate<URL> classpathFilter) {
        GradleHandleFactory classpathInjectingHandleFactory =
                new ClasspathInjectingGradleHandleFactory(sourceClassLoader, handleFactory, classpathFilter)
        return new DefaultGradleRunner(classpathInjectingHandleFactory)
    }
}
