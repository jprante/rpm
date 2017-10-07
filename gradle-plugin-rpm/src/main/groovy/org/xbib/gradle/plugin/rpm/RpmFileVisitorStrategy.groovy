package org.xbib.gradle.plugin.rpm

import org.xbib.rpm.RpmBuilder
import org.xbib.rpm.payload.Directive
import org.gradle.api.file.FileCopyDetails

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RpmFileVisitorStrategy {

    protected final RpmBuilder builder

    RpmFileVisitorStrategy(RpmBuilder builder) {
        this.builder = builder
    }

    void addFile(FileCopyDetails details, Path source, int mode, int dirmode, EnumSet<Directive> directive, String uname, String gname, boolean addParents) {
        try {
            if (!Files.isSymbolicLink(Paths.get(details.file.parentFile.path))) {
                addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
            }
        }
        catch (UnsupportedOperationException e) {
            // For file details that have filters, accessing the file throws this exception
            addFileToBuilder(details, source, mode, dirmode, directive, uname, gname, addParents)
        }
    }

    void addDirectory(FileCopyDetails details, int permissions, EnumSet<Directive> directive, String uname,
                      String gname, boolean addParents) {
        try {
            if (Files.isSymbolicLink(Paths.get(details.file.path))) {
                addLinkToBuilder(details)
            }
            else {
                addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
            }
        } catch (UnsupportedOperationException e) {
            // For file details that have filters, accessing the directory throws this exception
            addDirectoryToBuilder(details, permissions, directive, uname, gname, addParents)
        }
    }

    protected void addFileToBuilder(FileCopyDetails details, Path source, int mode, int dirmode, EnumSet<Directive> directive, String uname, String gname, boolean addParents) {
        builder.addFile(getRootPath(details), source, mode, dirmode, directive, uname, gname, addParents)
    }

    protected void addDirectoryToBuilder(FileCopyDetails details, int permissions, EnumSet<Directive> directive, String uname, String gname, boolean addParents) {
        builder.addDirectory(getRootPath(details), permissions, directive, uname, gname, addParents)
    }

    private void addLinkToBuilder(FileCopyDetails details) {
        Path path = Paths.get(details.file.path)
        Path target = Files.readSymbolicLink(path)
        builder.addLink(getRootPath(details), target.toFile().path)
    }

    private static String getRootPath(FileCopyDetails details) {
        "/${details.path}".toString()
    }
}
