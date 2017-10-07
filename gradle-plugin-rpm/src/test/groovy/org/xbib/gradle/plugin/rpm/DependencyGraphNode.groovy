package org.xbib.gradle.plugin.rpm

import groovy.transform.Immutable

@Immutable
class DependencyGraphNode {

    @Delegate Coordinate coordinate

    List<Coordinate> dependencies = []

    @Override
    String toString() {
        "${group}:${artifact}:${version}"
    }
}
