package org.xbib.gradle.plugin.rpm

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.lead.Os
import org.xbib.rpm.lead.PackageType

import java.nio.file.Path

/**
 *
 */
class Rpm extends AbstractArchiveTask {

	@Input
    @Optional
	Path changeLogFile

    @Delegate
    @Nested
    SystemPackagingExtension systemPackagingExtension

    ProjectPackagingExtension projectPackagingExtension

    Rpm() {
        super()
        systemPackagingExtension = new SystemPackagingExtension()
        projectPackagingExtension = project.extensions.findByType(ProjectPackagingExtension)
        if (projectPackagingExtension) {
            getRootSpec().with(projectPackagingExtension.delegateCopySpec)
        }
        extension = 'rpm'
    }

    @Override
    @TaskAction
    protected void copy() {
        use(CopySpecEnhancement) {
            super.copy()
        }
    }

    @Override
    AbstractCopyTask from(Object... sourcePaths) {
        for (Object sourcePath : sourcePaths) {
            from(sourcePath, {})
        }
        this
    }

    @Override
    AbstractCopyTask from(Object sourcePath, Closure c) {
        def preserveSymlinks = FromConfigurationFactory.preserveSymlinks(this)
        use(CopySpecEnhancement) {
            getMainSpec().from(sourcePath, c << preserveSymlinks)
        }
        this
    }

    @Override
    AbstractArchiveTask into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            getMainSpec().into(destPath, configureClosure)
        }
        this
    }

    @Override
    AbstractCopyTask exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            getMainSpec().exclude(excludeSpec)
        }
        this
    }

    @Override
    AbstractCopyTask filter(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().filter(closure)
        }
        this
    }

    @Override
    AbstractCopyTask rename(Closure closure) {
        use(CopySpecEnhancement) {
            getMainSpec().rename(closure)
        }
        this
    }

    @Override
    RpmCopyAction createCopyAction() {
        new RpmCopyAction(this)
    }

    @Input
    @Optional
    void setArch(Object arch) {
        setArchStr((arch instanceof Architecture)?arch.name():arch.toString())
    }

    @Input
    @Optional
    List<Object> getAllConfigurationPaths() {
        return getConfigurationPaths() + (projectPackagingExtension?.getConfigurationPaths()?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreInstallCommands() {
        return getPreInstallCommands() + (projectPackagingExtension?.getPreInstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostInstallCommands() {
        return getPostInstallCommands() + (projectPackagingExtension?.getPostInstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreUninstallCommands() {
        return getPreUninstallCommands() + (projectPackagingExtension?.getPreUninstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPostUninstallCommands() {
        return getPostUninstallCommands() + (projectPackagingExtension?.getPostUninstallCommands() ?: [])
    }

    @Input
    @Optional
    List<Object> getAllPreTransCommands() {
        return getPreTransCommands() + projectPackagingExtension?.getPreTransCommands()
    }

    @Input
    @Optional
    List<Object> getAllPostTransCommands() {
        return getPostTransCommands() + projectPackagingExtension?.getPostTransCommands()
    }

    @Input
    @Optional
    List<Object> getAllCommonCommands() {
        return getCommonCommands() + projectPackagingExtension?.getCommonCommands()
    }

    @Input
    @Optional
    List<Object> getAllSupplementaryControlFiles() {
        return getSupplementaryControlFiles() + (projectPackagingExtension?.getSupplementaryControlFiles() ?: [])
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
            projectPackagingExtension?.getPackageName()?:getBaseName()
        })
        mapping.map('release', {
            projectPackagingExtension?.getRelease()?:getClassifier()
        })
        mapping.map('version', {
            sanitizeVersion(projectPackagingExtension?.getVersion()?:project.getVersion().toString())
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
        mapping.map('archiveName', {
            assembleArchiveName()
        })
        mapping.map('fileType', {
            projectPackagingExtension?.getFileType()
        })
        mapping.map('addParentDirs', {
            projectPackagingExtension?.getAddParentDirs()?:true
        })
        mapping.map('archStr', {
            projectPackagingExtension?.getArchStr()?:Architecture.NOARCH.name()
        })
        mapping.map('os', {
            projectPackagingExtension?.getOs()?:Os.UNKNOWN
        })
        mapping.map('type', {
            projectPackagingExtension?.getType()?:PackageType.BINARY
        })
        mapping.map('prefixes', {
            projectPackagingExtension?.getPrefixes()?:[]
        })
    }

    String assembleArchiveName() {
        String name = getPackageName()
        name += getVersion() ? "-${getVersion()}" : ''
        name += getRelease() ? "-${getRelease()}" : ''
        name += getArchString() ? ".${getArchString()}" : ''
        name += getExtension() ? ".${getExtension()}" : ''
        name
    }

    String getArchString() {
        getArchStr()?.toLowerCase()
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
}
