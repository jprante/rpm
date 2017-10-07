package org.xbib.gradle.plugin.rpm.validation

import org.gradle.api.Task

interface SystemPackagingTaskPropertiesValidator<T extends Task> {

    void validate(T task)
}
