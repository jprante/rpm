package org.xbib.gradle.plugin.rpm

import groovy.transform.Immutable

@Immutable
class Coordinate {

    String group

    String artifact

    String version

    @Override
    String toString() {
        "${group}:${artifact}:${version}"
    }
}
