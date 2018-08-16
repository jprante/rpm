package org.xbib.gradle.plugin.rpm

class RpmPluginIntegrationTest extends IntegrationSpec {

    def "rpm task is marked up-to-date when setting arch or os property"() {

            given:
        buildFile << '''
apply plugin: 'org.xbib.gradle.plugin.rpm'

task buildRpm(type: Rpm) {
    packageName = 'rpmIsUpToDate'
    arch = NOARCH
    os = LINUX
}
'''
        when:
        runTasksSuccessfully('buildRpm')

        and:
        def result = runTasksSuccessfully('buildRpm')

        then:
        result.wasUpToDate('buildRpm')
    }
}
