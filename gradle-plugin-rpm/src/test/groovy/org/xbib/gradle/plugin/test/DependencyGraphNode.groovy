package org.xbib.gradle.plugin.test

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
