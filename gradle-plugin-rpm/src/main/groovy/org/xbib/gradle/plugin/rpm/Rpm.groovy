package org.xbib.gradle.plugin.rpm

import groovy.util.logging.Log4j
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GUtil
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.lead.Os
import org.xbib.rpm.lead.PackageType

import javax.annotation.Nullable
import java.nio.file.Path
import java.util.concurrent.Callable

/**
 *
 */
@Log4j
class Rpm extends AbstractArchiveTask {

    final ObjectFactory objectFactory

	@Input
    @Optional
	Path changeLogFile

    @Delegate
    @Nested
    SystemPackagingExtension systemPackagingExtension

    ProjectPackagingExtension projectPackagingExtension

    Rpm() {
        super()
        objectFactory = project.objects
        systemPackagingExtension = new SystemPackagingExtension()
        projectPackagingExtension = project.extensions.findByType(ProjectPackagingExtension)
        if (projectPackagingExtension) {
            getRootSpec().with(projectPackagingExtension.delegateCopySpec)
        }
        archiveExtension.set('rpm')

        // override archive file name provider in Gradle 5
        Callable<String> archiveFileNameProvider = new Callable<String>() {
            @Override
            String call() throws Exception {
                constructArchiveFile()
            }
        }
        getArchiveFileName().set(getProject().provider(archiveFileNameProvider))
    }

    @Override
    RpmCopyAction createCopyAction() {
        new RpmCopyAction(this)
    }

    @Override
    @TaskAction
    protected void copy() {
        use(CopySpecEnhancement) {
            super.copy()
        }
    }

    @Override
    Rpm from(Object... sourcePaths) {
        for (Object sourcePath : sourcePaths) {
            from(sourcePath, {})
        }
        this
    }

    @Override
    Rpm from(Object sourcePath, Closure c) {
        def preserveSymlinks = FromConfigurationFactory.preserveSymlinks(this)
        use(CopySpecEnhancement) {
            getMainSpec().from(sourcePath, c << preserveSymlinks)
        }
        this
    }

    @Override
    Rpm into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            getMainSpec().into(destPath, configureClosure)
        }
        this
    }

    @Override
    Rpm exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            getMainSpec().exclude(excludeSpec)
        }
        this
    }

    @Override
    Rpm filter(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().filter(closure)
        }
        this
    }

    @Override
    Rpm rename(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().rename(closure)
        }
        this
    }

    @Input
    @Optional
    List<Object> getAllConfigurationPaths() {
        getConfigurationPaths() + (projectPackagingExtension?.getConfigurationPaths()?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreInstallCommands() {
        getPreInstallCommands() + (projectPackagingExtension?.getPreInstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostInstallCommands() {
        getPostInstallCommands() + (projectPackagingExtension?.getPostInstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreUninstallCommands() {
        getPreUninstallCommands() + (projectPackagingExtension?.getPreUninstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostUninstallCommands() {
        getPostUninstallCommands() + (projectPackagingExtension?.getPostUninstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreTransCommands() {
        getPreTransCommands() + projectPackagingExtension?.getPreTransCommands()
    }

    @Input
    @Optional
    List<Object> getAllPostTransCommands() {
        getPostTransCommands() + projectPackagingExtension?.getPostTransCommands()
    }

    @Input
    @Optional
    List<Object> getAllCommonCommands() {
        getCommonCommands() + projectPackagingExtension?.getCommonCommands()
    }

    @Input
    @Optional
    List<Object> getAllSupplementaryControlFiles() {
        getSupplementaryControlFiles() + (projectPackagingExtension?.getSupplementaryControlFiles() ?: [])
    }

    @Input
    @Optional
    List<Link> getAllLinks() {
        if (projectPackagingExtension) {
            return getLinks() + projectPackagingExtension.getLinks()
        } else {
            return getLinks()
        }
    }

    @Input
    @Optional
    List<Dependency> getAllDependencies() {
        if (projectPackagingExtension) {
            return getDependencies() + projectPackagingExtension.getDependencies()
        } else {
            return getDependencies()
        }
    }

    @Input
    @Optional
    def getAllPrefixes() {
        if (projectPackagingExtension) {
            return (getPrefixes() + projectPackagingExtension.getPrefixes()).unique()
        } else {
            return getPrefixes()
        }
    }

    @Input
    @Optional
    List<Dependency> getAllProvides() {
        if (projectPackagingExtension) {
            return projectPackagingExtension.getProvides() + getProvides()
        } else {
            return getProvides()
        }
    }

    @Input
    @Optional
    List<Dependency> getAllObsoletes() {
        if (projectPackagingExtension) {
            return getObsoletes() + projectPackagingExtension.getObsoletes()
        } else {
            return getObsoletes()
        }
    }

    @Input
    @Optional
    List<Dependency> getAllConflicts() {
        if (projectPackagingExtension) {
            return getConflicts() + projectPackagingExtension.getConflicts()
        } else {
            return getConflicts()
        }
    }

    /**
     * Defines input files annotation with @SkipWhenEmpty as a workaround to force building the archive even if no
     * from clause is declared. Without this method the task would be marked UP-TO-DATE - the actual archive creation
     * would be skipped.
     *
     * The provided file collection is not supposed to be used or modified anywhere else in the task.
     *
     * @return Collection of files
     */
    @InputFiles
    @SkipWhenEmpty
    private final FileCollection getFakeFiles() {
        project.files('fake')
    }

    void applyConventions() {
        ConventionMapping mapping = ((IConventionAware) this).getConventionMapping()

        mapping.map('packageName', {
            projectPackagingExtension?.getPackageName()?:getArchiveBaseName().getOrNull()?:'test'
        })
        mapping.map('version', {
            sanitizeVersion(projectPackagingExtension?.getVersion()?:project.getVersion().toString())
        })
        mapping.map('release', {
            projectPackagingExtension?.getRelease()?:''
        })
        mapping.map('epoch', {
            projectPackagingExtension?.getEpoch()?:0
        })
        mapping.map('signingKeyId', {
            projectPackagingExtension?.getSigningKeyId()
        })
        mapping.map('signingKeyPassphrase', {
            projectPackagingExtension?.getSigningKeyPassphrase()
        })
        mapping.map('signingKeyRing', {
            projectPackagingExtension?.getSigningKeyRing()
        })
        mapping.map('signingKeyHashAlgo', {
            projectPackagingExtension?.getSigningKeyHashAlgo()
        })
        mapping.map('user', {
            projectPackagingExtension?.getUser()?:getPackager()
        })
        mapping.map('maintainer', {
            projectPackagingExtension?.getMaintainer()?:getPackager()
        })
        mapping.map('uploaders', {
            projectPackagingExtension?.getUploaders()?:getPackager()
        })
        mapping.map('permissionGroup', {
            projectPackagingExtension?.getPermissionGroup()?:''
        })
        mapping.map('packageGroup', {
            projectPackagingExtension?.getPackageGroup()
        })
        mapping.map('buildHost', {
            projectPackagingExtension?.getBuildHost()?: getLocalHostName()
        })
        mapping.map('summary', {
            projectPackagingExtension?.getSummary()?:getPackageName()
        })
        mapping.map('packageDescription', {
            String packageDescription = projectPackagingExtension?.getPackageDescription()?:project.getDescription()
            packageDescription ?: ''
        })
        mapping.map('license', {
            projectPackagingExtension?.getLicense()?:''
        })
        mapping.map('packager', {
            projectPackagingExtension?.getPackager()?:System.getProperty('user.name', '')
        })
        mapping.map('distribution', {
            projectPackagingExtension?.getDistribution()?:''
        })
        mapping.map('vendor', {
            projectPackagingExtension?.getVendor()?:''
        })
        mapping.map('url', {
            projectPackagingExtension?.getUrl()?:''
        })
        mapping.map('sourcePackage', {
            projectPackagingExtension?.getSourcePackage()?:''
        })
        mapping.map('createDirectoryEntry', {
            projectPackagingExtension?.getCreateDirectoryEntry()?:false
        })
        mapping.map('priority', {
            projectPackagingExtension?.getPriority()?:'optional'
        })
        mapping.map('preInstall', {
            projectPackagingExtension?.getPreInstall()
        })
        mapping.map('postInstall', {
            projectPackagingExtension?.getPostInstall()
        })
        mapping.map('preUninstall', {
            projectPackagingExtension?.getPreUninstall()
        })
        mapping.map('postUninstall', {
            projectPackagingExtension?.getPostUninstall()
        })
        mapping.map('fileType', {
            projectPackagingExtension?.getFileType()
        })
        mapping.map('addParentDirs', {
            projectPackagingExtension?.getAddParentDirs()?:true
        })
        mapping.map('arch', {
            projectPackagingExtension?.arch?:Architecture.NOARCH
        })
        mapping.map('os', {
            projectPackagingExtension?.os?:Os.UNKNOWN
        })
        mapping.map('type', {
            projectPackagingExtension?.type?:PackageType.BINARY
        })
        mapping.map('prefixes', {
            projectPackagingExtension?.getPrefixes()?:[]
        })
        mapping.map('archiveName', {
            constructArchiveFile()
        })
        mapping.map('archivePath', {
            determineArchivePath()
        })
        mapping.map('archiveFile', {
            determineArchiveFile()
        })
    }

    void prefixes(String... addPrefixes) {
        systemPackagingExtension.prefixes.addAll(addPrefixes)
    }

    List<String> getPrefixes() {
        systemPackagingExtension.prefixes
    }

	void setChangeLogFile(Path changeLogFile) {
		this.changeLogFile = changeLogFile
	}

    Path getChangeLogFile() {
        changeLogFile
    }

    Provider<RegularFile> determineArchiveFile() {
        Property<RegularFile> regularFile = objectFactory.fileProperty()
        regularFile.set(new DestinationFile(new File(getDestinationDirectory().get().asFile.path, constructArchiveFile())))
        regularFile
    }

    File determineArchivePath() {
        determineArchiveFile().get().asFile
    }

    String constructArchiveFile() {
        String name = GUtil.elvis(getPackageName(), "")
        name += maybe(name, '-', getVersion())
        name += maybe(name, '-', getRelease())
        name += maybe(name, '.', getArch().name().toLowerCase())
        String extension = archiveExtension.getOrNull()
        name += GUtil.isTrue(extension) ? "." + extension : ""
        name
    }

    static String sanitizeVersion(String version) {
        version.replaceAll(/\+.*/, '').replaceAll(/-/, '~')
    }

    static String getLocalHostName() {
        try {
            return InetAddress.localHost.hostName
        } catch (UnknownHostException ignore) {
            return "unknown"
        }
    }

    static String maybe(@Nullable String prefix, String delimiter, @Nullable String value) {
        if (GUtil.isTrue(value)) {
            if (GUtil.isTrue(prefix)) {
                return delimiter.concat(value)
            } else {
                return value
            }
        }
        ""
    }

    static class DestinationFile implements RegularFile {
        private final File file

        DestinationFile(File file) {
            this.file = file
        }

        String toString() {
            return this.file.toString()
        }

        @Override
        File getAsFile() {
            return this.file
        }
    }
}
