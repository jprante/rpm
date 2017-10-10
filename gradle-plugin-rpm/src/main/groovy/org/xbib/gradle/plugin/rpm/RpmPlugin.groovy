package org.xbib.gradle.plugin.rpm

import org.gradle.api.plugins.BasePlugin
import org.xbib.rpm.RpmBuilder
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.format.Flags
import org.xbib.rpm.lead.Os
import org.xbib.rpm.lead.PackageType
import org.xbib.rpm.payload.Directive
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 *
 */
class RpmPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply(BasePlugin)

        project.ext.Rpm = Rpm.class

        RpmBuilder.metaClass.getDefaultSourcePackage() {
            format.getLead().getName() + ".src.rpm"
        }

        project.tasks.withType(Rpm) { Rpm task ->
            applyAliases(task)
            task.applyConventions()
        }
    }

    def static applyAliases(def dynamicObjectAware) {
        aliasEnumValues(Architecture.values(), dynamicObjectAware)
        aliasEnumValues(Os.values(), dynamicObjectAware)
        aliasEnumValues(PackageType.values(), dynamicObjectAware)
        aliasStaticInstances(Directive.class, dynamicObjectAware)
        aliasStaticInstances(Flags.class, int.class, dynamicObjectAware)
    }

    private static <T extends Enum<T>> void aliasEnumValues(T[] values, dynAware) {
        for (T value : values) {
            dynAware.metaClass."${value.name()}" = value
        }
    }

    private static <T> void aliasStaticInstances(Class<T> forClass, dynAware) {
        aliasStaticInstances(forClass, forClass, dynAware)
    }

    private static <T, U> void aliasStaticInstances(Class<T> forClass, Class<U> ofClass, dynAware) {
        for (Field field : forClass.fields) {
            if (field.type == ofClass && hasModifier(field, Modifier.STATIC)) {
                dynAware.metaClass."${field.name}" = field.get(null)
            }
        }
    }

    private static boolean hasModifier(Field field, int modifier) {
        (field.modifiers & modifier) == modifier
    }
}
