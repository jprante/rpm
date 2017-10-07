package org.xbib.gradle.plugin.rpm

import org.gradle.api.file.CopySpec
import org.gradle.api.internal.file.copy.CopySpecWrapper
import org.gradle.internal.impldep.org.apache.commons.lang.reflect.FieldUtils

/**
 *
 */
@Category(CopySpec)
class CopySpecEnhancement {

    static void appendFieldToCopySpec(CopySpec spec, String fieldName, Object value) {
        def directSpec = spec
        if (spec instanceof CopySpecWrapper) {
            def delegateField = FieldUtils.getField(CopySpecWrapper, 'delegate', true)
            directSpec = delegateField.get(spec)
        }
        directSpec.metaClass["get${fieldName.capitalize()}"] = { value }
    }

    static void user(CopySpec spec, String user) {
        appendFieldToCopySpec(spec, 'user', user)
    }

    static void setUser(CopySpec spec, String userArg) {
        user(spec, userArg)
    }

    static void permissionGroup(CopySpec spec, String permissionGroup) {
        appendFieldToCopySpec(spec, 'permissionGroup', permissionGroup)
    }

    static void setPermissionGroup(CopySpec spec, String permissionGroupArg) {
        permissionGroup(spec, permissionGroupArg)
    }

    static void setFileType(CopySpec spec, List<String> fileTypeArg) {
        fileType(spec, fileTypeArg)
    }

    static void fileType(CopySpec spec, List<String> fileType) {
        appendFieldToCopySpec(spec, 'fileType', fileType)
    }

    static void addParentDirs(CopySpec spec, boolean addParentDirs) {
        appendFieldToCopySpec(spec, 'addParentDirs', addParentDirs)
    }

    static void setAddParentDirs(CopySpec spec, boolean addParentDirsArg) {
        addParentDirs(spec, addParentDirsArg)
    }

    static void createDirectoryEntry(CopySpec spec, boolean createDirectoryEntry) {
        appendFieldToCopySpec(spec, 'createDirectoryEntry', createDirectoryEntry)
    }

    static void setCreateDirectoryEntry(CopySpec spec, boolean createDirectoryEntryArg) {
        createDirectoryEntry(spec, createDirectoryEntryArg)
    }

    static void uid(CopySpec spec, int uid) {
        appendFieldToCopySpec(spec, 'uid', uid)
    }

    static void setUid(CopySpec spec, int uidArg) {
        uid(spec, uidArg)
    }

    static void gid(CopySpec spec, int gid) {
        appendFieldToCopySpec(spec, 'gid', gid)
    }

    static void setGid(CopySpec spec, int gidArg) {
        gid(spec, gidArg)
    }
}
