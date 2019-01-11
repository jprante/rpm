package org.xbib.gradle.plugin.rpm

import org.xbib.gradle.plugin.test.IntegrationSpec

class RpmPluginIntegrationTest extends IntegrationSpec {

    def "rpm task is marked up-to-date when setting arch or os property"() {

            given:
        buildFile << '''
apply plugin: 'org.xbib.gradle.plugin.rpm'

task buildRpm(type: Rpm) {
    packageName = 'rpmIsUpToDate'
    arch = org.xbib.rpm.lead.Architecture.NOARCH
    os = org.xbib.rpm.lead.Os.LINUX
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
