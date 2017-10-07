package org.xbib.gradle.plugin.rpm.validation

interface SystemPackagingAttributeValidator {

    boolean validate(String attribute)

    String getErrorMessage(String attribute)
}
