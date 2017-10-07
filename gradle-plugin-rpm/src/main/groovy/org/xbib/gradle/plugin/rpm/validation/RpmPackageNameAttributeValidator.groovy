package org.xbib.gradle.plugin.rpm.validation

class RpmPackageNameAttributeValidator implements SystemPackagingAttributeValidator {

    @Override
    boolean validate(String packageName) {
        matchesExpectedCharacters(packageName)
    }

    private static boolean matchesExpectedCharacters(String packageName) {
        packageName ==~ /[a-zA-Z0-9-._+]+/
    }

    @Override
    String getErrorMessage(String attribute) {
        "Invalid package name '$attribute' - a valid package name must only contain [a-zA-Z0-9-._+]"
    }
}
