package org.xbib.gradle.plugin.rpm

import groovy.transform.EqualsAndHashCode
import org.xbib.rpm.format.Flags

@EqualsAndHashCode
class Dependency implements Serializable {

    String packageName

    String version

    int flag = 0

    Dependency alternative = null

    Dependency(String packageName, String version, int flag=0) {
        if (packageName.contains(',')) {
            throw new IllegalArgumentException('package name can not contain comma')
        }
        this.packageName = packageName
        this.version = version
        this.flag = flag
    }

    Dependency or(String packageName, String version='', int flag=0) {
        alternative = new Dependency(packageName, version, flag)
        alternative
    }

    String toDebString() {
        def signMap = [
            (Flags.GREATER|Flags.EQUAL): '>=',
            (Flags.LESS|Flags.EQUAL):    '<=',
            (Flags.EQUAL):               '=',
            (Flags.GREATER):             '>>',
            (Flags.LESS):                '<<'
        ]

        def depStr = this.packageName
        if (this.flag && this.version) {
            def sign = signMap[this.flag]
            if (sign==null) {
                throw new IllegalArgumentException()
            }
            depStr += " (${sign} ${this.version})"
        } else if (this.version) {
            depStr += " (${this.version})"
        }
        if (alternative) {
            depStr += " | " + alternative.toDebString()
        }
        depStr
    }
}
