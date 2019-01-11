package org.xbib.gradle.plugin.test

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
