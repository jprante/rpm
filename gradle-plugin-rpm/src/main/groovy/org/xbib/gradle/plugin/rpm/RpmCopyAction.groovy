package org.xbib.gradle.plugin.rpm

import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.CopySpecResolver
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.internal.file.copy.DefaultFileCopyDetails
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.internal.UncheckedException
import org.xbib.gradle.plugin.rpm.validation.RpmTaskPropertiesValidator
import org.xbib.rpm.RpmBuilder
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.header.HeaderTag
import org.xbib.rpm.payload.Directive

import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 *
 */
class RpmCopyAction implements CopyAction {

    private static final Logger logger = Logging.getLogger(RpmCopyAction.class)

    Rpm task

    Path tempDir

    RpmBuilder builder

    boolean includeStandardDefines = true

    private final RpmTaskPropertiesValidator rpmTaskPropertiesValidator = new RpmTaskPropertiesValidator()

    private RpmFileVisitorStrategy rpmFileVisitorStrategy

    RpmCopyAction(Rpm task) {
        this.task = task
        rpmTaskPropertiesValidator.validate(task)
    }

    WorkResult execute(CopyActionProcessingStream stream) {
        try {
            startVisit(this)
            stream.process(new StreamAction())
            endVisit()
        } catch (Exception e) {
            UncheckedException.throwAsUncheckedException(e)
        }
        WorkResults.didWork(true)
    }

    private class StreamAction implements CopyActionProcessingStreamAction {

        @Override
        void processFile(FileCopyDetailsInternal details) {
            def ourSpec = extractSpec(details)
            if (details.isDirectory()) {
                visitDir(details, ourSpec)
            } else {
                visitFile(details, ourSpec)
            }
        }
    }

    void endVisit() {
        for (Link link : task.getAllLinks()) {
            logger.debug "adding link {} -> {}", link.path, link.target
            addLink link
        }
        for (Dependency dep : task.getAllDependencies()) {
            logger.debug "adding dependency on {} {}", dep.packageName, dep.version
            addDependency dep
        }
        for (Dependency obsolete: task.getAllObsoletes()) {
            logger.debug "adding obsoletes on {} {}", obsolete.packageName, obsolete.version
            addObsolete obsolete
        }
        for (Dependency conflict : task.getAllConflicts()) {
            logger.debug "adding conflicts on {} {}", conflict.packageName, conflict.version
            addConflict conflict
        }
        for (Dependency provides : task.getAllProvides()) {
            logger.debug "adding provides on {} {}", provides.packageName, provides.version
            addProvides(provides)
        }
        task.directories.each { directory ->
            logger.debug "adding directory {}", directory.path
            addDirectory(directory)
        }
        end()
    }

    static String concat(Collection<Object> scripts) {
        String shebang
        StringBuilder result = new StringBuilder()
        scripts.each { script ->
            script?.eachLine { line ->
                if (line.matches('^#!.*$')) {
                    if (!shebang) {
                        shebang = line
                    } else if (line != shebang) {
                        throw new IllegalArgumentException("mismatching #! script lines")
                    }
                } else {
                    result.append line
                    result.append "\n"
                }
            }
        }
        if (shebang) {
            result.insert(0, shebang + "\n")
        }
        result.toString()
    }

    static CopySpecInternal extractSpec(FileCopyDetailsInternal fileDetails) {
        if (fileDetails instanceof DefaultFileCopyDetails) {
            def startingClass = fileDetails.getClass()
            while( startingClass != null && startingClass != DefaultFileCopyDetails) {
                startingClass = startingClass.superclass
            }
            Field specField = startingClass.getDeclaredField('specResolver')
            specField.setAccessible(true)
            CopySpecResolver specResolver = specField.get(fileDetails)
            Field field = DefaultCopySpec.DefaultCopySpecResolver.class.getDeclaredField('this$0')
            field.setAccessible(true)
            CopySpecInternal spec = field.get(specResolver)
            return spec
        } else {
            return null
        }
    }

    Path extractPath(FileCopyDetailsInternal fileDetails) {
        Path path
        try {
            path = fileDetails.getFile().toPath()
        } catch (UnsupportedOperationException uoe) {
            path = tempDir.resolve(fileDetails.path)
            fileDetails.copyTo(path.toFile())
        }
        path
    }

    void startVisit(CopyAction action) {
        tempDir = task.getTemporaryDir().toPath()
        if (!task.getVersion()) {
            throw new IllegalArgumentException('RPM requires a version string')
        }
        if ([task.preInstall, task.postInstall, task.preUninstall, task.postUninstall].any()) {
            logger.warn('at least one of (preInstall|postInstall|preUninstall|postUninstall) is defined ' +
                    'and will be ignored for RPM builds')
        }
        builder = new RpmBuilder()
        builder.setPackage task.packageName, task.version, task.release, task.epoch
        builder.setType task.type
        builder.setPlatform Architecture.valueOf(task.archStr.toUpperCase()), task.os
        builder.setGroup task.packageGroup
        builder.setBuildHost task.buildHost
        builder.setSummary task.summary
        builder.setDescription task.packageDescription ?: ''
        builder.setLicense task.license
        builder.setPackager task.packager
        builder.setDistribution task.distribution
        builder.setVendor task.vendor
        builder.setUrl task.url
        if (task.allPrefixes) {
            builder.setPrefixes(task.allPrefixes as String[])
        }
        if (task.getSigningKeyId() && task.getSigningKeyPassphrase() && task.getSigningKeyRing()) {
            builder.setPrivateKeyId task.getSigningKeyId()
            builder.setPrivateKeyPassphrase task.getSigningKeyPassphrase()
            builder.setPrivateKeyRing task.getSigningKeyRing()
        }
        String sourcePackage = task.sourcePackage
        if (!sourcePackage) {
            sourcePackage = builder.defaultSourcePackage
        }
        builder.addHeaderEntry HeaderTag.SOURCERPM, sourcePackage
        builder.setPreInstall task.getPreInstall()
        builder.setPostInstall task.getPostInstall()
        builder.setPreUninstall task.getPreUninstall()
        builder.setPostUninstall task.getPostUninstall()
        builder.setPreTrans task.getPreTrans()
        builder.setPostTrans task.getPostTrans()
        builder.setPreInstallValue(scriptWithUtils(task.allCommonCommands, task.allPreInstallCommands))
        builder.setPostInstallValue(scriptWithUtils(task.allCommonCommands, task.allPostInstallCommands))
        builder.setPreUninstallValue(scriptWithUtils(task.allCommonCommands, task.allPreUninstallCommands))
        builder.setPostUninstallValue(scriptWithUtils(task.allCommonCommands, task.allPostUninstallCommands))
        builder.setPreTransValue(scriptWithUtils(task.allCommonCommands, task.allPreTransCommands))
        builder.setPostTransValue(scriptWithUtils(task.allCommonCommands, task.allPostTransCommands))
		if (((Rpm) task).changeLogFile != null) {
			builder.addChangelog(((Rpm) task).changeLogFile)
		}
        rpmFileVisitorStrategy = new RpmFileVisitorStrategy(builder)
    }

    void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString
        def inputFile = extractPath(fileDetails)
        EnumSet<Directive> fileType = lookup(specToLookAt, 'fileType')
        String user = lookup(specToLookAt, 'user') ?: task.user
        String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup
        int fileMode = lookup(specToLookAt, 'fileMode') ?: fileDetails.mode
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir!=null ? specAddParentsDir : task.addParentDirs
        rpmFileVisitorStrategy.addFile(fileDetails, inputFile, fileMode, -1, fileType, user, group, addParentsDir)
    }

    void visitDir(FileCopyDetailsInternal dirDetails, def specToLookAt) {
        if (specToLookAt == null) {
            logger.info("Got an empty spec from ${dirDetails.class.name} for ${dirDetails.path}/${dirDetails.name}")
            return
        }
        // Have to take booleans specially, since they would fail an elvis operator if set to false
        def specCreateDirectoryEntry = lookup(specToLookAt, 'createDirectoryEntry')
        boolean createDirectoryEntry = specCreateDirectoryEntry!=null ? specCreateDirectoryEntry : task.createDirectoryEntry
        def specAddParentsDir = lookup(specToLookAt, 'addParentDirs')
        boolean addParentsDir = specAddParentsDir != null ? specAddParentsDir : task.addParentDirs
        if (createDirectoryEntry) {
            logger.debug 'adding directory {}', dirDetails.relativePath.pathString
            int dirMode = lookup(specToLookAt, 'dirMode') ?: dirDetails.mode
            List<String> directiveList = (lookup(specToLookAt, 'fileType') ?: task.fileType) as List<String>
            EnumSet<Directive> directive = makeDirective(directiveList)
            String user = lookup(specToLookAt, 'user') ?: task.user
            String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup
            rpmFileVisitorStrategy.addDirectory(dirDetails, dirMode, directive, user, group, addParentsDir)
        }
    }

    protected void addLink(Link link) {
        builder.addLink link.path, link.target, link.permissions
    }

    protected void addDependency(Dependency dep) {
        builder.addDependency(dep.packageName, dep.flag, dep.version)
    }

    protected void addConflict(Dependency dep) {
        builder.addConflicts(dep.packageName, dep.flag, dep.version)
    }

    protected void addObsolete(Dependency dep) {
        builder.addObsoletes(dep.packageName, dep.flag, dep.version)
    }

    protected void addProvides(Dependency dep) {
        builder.addProvides(dep.packageName, dep.version, dep.flag)
    }

    protected void addDirectory(Directory directory) {
        def user = directory.user ? directory.user : task.user
        def permissionGroup = directory.permissionGroup ? directory.permissionGroup : task.permissionGroup
        builder.addDirectory(directory.path, directory.permissions, null, user, permissionGroup, directory.addParents)
    }

    protected void end() {
        Path path = task.getArchivePath().toPath()
        Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING).withCloseable { ch ->
            builder.build(ch)
        }
        logger.info 'Created RPM archive {}', path
    }

    String standardScriptDefines() {
        includeStandardDefines ?
            String.format(" RPM_ARCH=%s \n RPM_OS=%s \n RPM_PACKAGE_NAME=%s \n RPM_PACKAGE_VERSION=%s \n RPM_PACKAGE_RELEASE=%s \n\n",
                task.getArchString(),
                task.os?.toString()?.toLowerCase() ?: '',
                task.getPackageName(),
                task.getVersion(),
                task.getRelease()) : null
    }

    String scriptWithUtils(List<Object> utils, List<Object> scripts) {
        def list = []
        def stdDefines = standardScriptDefines()
        if (stdDefines) {
            list.add(stdDefines)
        }
        list.addAll(utils)
        list.addAll(scripts)
        concat(list)
    }

    static <T> T lookup(def specToLookAt, String propertyName) {
        if (specToLookAt?.metaClass?.hasProperty(specToLookAt, propertyName) != null) {
            def prop = specToLookAt.metaClass.getProperty(specToLookAt, propertyName)
            if (prop instanceof MetaBeanProperty) {
                return prop?.getProperty(specToLookAt) as T
            } else {
                return prop as T
            }
        } else {
            return null
        }
    }

    static EnumSet<Directive> makeDirective(List<String> strings) {
        EnumSet<Directive> set = EnumSet.of(Directive.NONE)
        for (String string : strings) {
            set.add(Directive.valueOf(string))
        }
        set
    }
}
