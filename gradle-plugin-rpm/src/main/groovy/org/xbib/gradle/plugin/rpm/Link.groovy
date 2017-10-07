package org.xbib.gradle.plugin.rpm

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Link implements Serializable {

    String path

    String target

    int permissions = -1
}
