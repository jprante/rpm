package org.xbib.gradle.plugin.rpm.validation

import org.xbib.gradle.plugin.rpm.Rpm
import org.gradle.api.InvalidUserDataException

class RpmTaskPropertiesValidator implements SystemPackagingTaskPropertiesValidator<Rpm> {

    private final SystemPackagingAttributeValidator packageNameValidator = new RpmPackageNameAttributeValidator()

    @Override
    void validate(Rpm task) {
        if (!packageNameValidator.validate(task.getPackageName())) {
            throw new InvalidUserDataException(packageNameValidator.getErrorMessage(task.getPackageName()))
        }
    }
}
