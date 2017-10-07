package org.xbib.gradle.plugin.rpm

import spock.lang.Specification

import java.nio.file.Paths

class SystemPackagingExtensionTest extends Specification {

    SystemPackagingExtension extension = new SystemPackagingExtension()

    def "Can define required package name without version and flag"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.requires(packageName)

        then:
        extension.dependencies.size() == 1
        Dependency dep = extension.dependencies[0]
        dep.packageName == packageName
        dep.version == ''
        dep.flag == 0
    }

    def "Can define required package name with version and without flag"(){
        given:
        String packageName = 'myPackage'

        when:
        extension.requires(packageName, '1.0.0')

        then:
        extension.dependencies.size() == 1
        Dependency dep = extension.dependencies[0]
        dep.packageName == packageName
        dep.version == '1.0.0'
        dep.flag == 0
    }

    def "Can define required package name with version and flag"() {
        given:
        String packageName = 'myPackage'

        when:
        extension.requires(packageName, '1.0.0', 5)

        then:
        extension.dependencies.size() == 1
        Dependency dep = extension.dependencies[0]
        dep.packageName == packageName
        dep.version == '1.0.0'
        dep.flag == 5
    }

    def "Cannot define required package name containing comma without version and flag"() {
        given:
        String packageName = 'myPackage,something'

        when:
        extension.requires(packageName)

        then:
        Throwable t = thrown(IllegalArgumentException)
    }

    def "Cannot define required package name containing comma with version and flag"() {
        given:
        String packageName = 'myPackage,something'

        when:
        extension.requires(packageName, '1.0.0', 5)

        then:
        Throwable t = thrown(IllegalArgumentException)
    }
}
