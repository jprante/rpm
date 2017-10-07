package org.xbib.gradle.plugin.rpm

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.format.Flags
import org.xbib.rpm.header.Header
import org.xbib.rpm.lead.Os
import org.xbib.rpm.lead.PackageType
import org.xbib.rpm.signature.SignatureTag
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.xbib.rpm.format.Flags.*
import static org.xbib.rpm.header.HeaderTag.*
import static org.xbib.rpm.payload.CpioHeader.*

/**
 *
 */
class RpmPluginTest extends ProjectSpec {
    def 'files'() {
        Project project = ProjectBuilder.builder().build()

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        new File(srcDir, fruit).withWriter { out ->
            out.write(fruit)
        }

        File noParentsDir = new File(projectDir, 'noParentsDir')
        noParentsDir.mkdirs()
        new File(noParentsDir, 'alone').withWriter { out ->
            out.write('alone')
        }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = PackageType.BINARY
            arch = Architecture.I386.name()
            os = Os.LINUX
            permissionGroup = 'Development/Libraries'
            summary = 'Bleah blarg'
            packageDescription = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            requires('blarg', '1.0', Flags.GREATER | Flags.EQUAL)
            requires('blech')

            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                createDirectoryEntry true
                fileType = ['config', 'noreplace']
            }

            from(noParentsDir) {
                addParentDirs false
                into '/a/path/not/to/create'
            }

            link('/opt/bleah/banana', '/opt/bleah/apple')
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def result = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        'bleah' == RpmReader.getHeaderEntryString(result, NAME)
        '1.0' == RpmReader.getHeaderEntryString(result, VERSION)
        '1' == RpmReader.getHeaderEntryString(result, RELEASE)
        0 == RpmReader.getHeaderEntry(result, EPOCH).values[0]
        'i386' == RpmReader.getHeaderEntryString(result, ARCH)
        'linux' == RpmReader.getHeaderEntryString(result, OS)
        ['SuperSystem'] == RpmReader.getHeaderEntry(result, DISTRIBUTION).values
        result.files*.name.every { fileName ->
            ['./a/path/not/to/create/alone', './opt/bleah',
             './opt/bleah/apple', './opt/bleah/banana'].any { path ->
                path.startsWith(fileName)
            }
        }
        result.files*.name.every { fileName ->
            ['./a/path/not/to/create'].every { path ->
                ! path.startsWith(fileName)
            }
        }
    }

    def 'obsoletesAndConflicts'() {

        Project project = ProjectBuilder.builder().build()
        File buildDir = project.buildDir

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        new File(srcDir, fruit).withWriter { out ->
            out.write(fruit)
        }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/ObsoletesConflictsTest')
            destinationDir.mkdirs()

            packageName = 'testing'
            version = '1.2'
            release = '3'
            type = BINARY
            arch = I386
            os = LINUX
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            obsoletes('blarg', '1.0', GREATER | EQUAL)
            conflicts('blech')
            conflicts('packageA', '1.0', LESS)
            obsoletes('packageB', '2.2', GREATER)

            from(srcDir)
            into '/opt/bleah'
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def result = RpmReader.read(project.file('build/tmp/ObsoletesConflictsTest/testing-1.2-3.i386.rpm').toPath())
        def obsoletes = RpmReader.getHeaderEntry(result, OBSOLETENAME)
        def obsoleteVersions = RpmReader.getHeaderEntry(result, OBSOLETEVERSION)
        def obsoleteComparisons = RpmReader.getHeaderEntry(result, OBSOLETEFLAGS)
        def conflicts = RpmReader.getHeaderEntry(result, CONFLICTNAME)
        def conflictVersions = RpmReader.getHeaderEntry(result, CONFLICTVERSION)
        def conflictComparisons = RpmReader.getHeaderEntry(result, CONFLICTFLAGS)
        def distribution = RpmReader.getHeaderEntry(result, DISTRIBUTION)

        'blarg' == obsoletes.values[0]
        '1.0' == obsoleteVersions.values[0]
        (GREATER | EQUAL) == obsoleteComparisons.values[0]

        'blech' == conflicts.values[0]
        '' == conflictVersions.values[0]
        0 == conflictComparisons.values[0]

        'packageA' == conflicts.values[1]
        '1.0' ==conflictVersions.values[1]
        LESS == conflictComparisons.values[1]

        'packageB' == obsoletes.values[1]
        '2.2' == obsoleteVersions.values[1]
        GREATER == obsoleteComparisons.values[1]

        ['SuperSystem'] == distribution.values
    }


    def 'projectNameDefault'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        new File(srcDir, fruit).withWriter { out ->
            out.write(fruit)
        }

        when:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {})

        then:
        'projectNameDefault' == project.buildRpm.packageName

        when:
        project.tasks.buildRpm.execute()

        then:
        noExceptionThrown()
    }
	
	def 'file handle closed'() {

		when:
		project.apply plugin: 'org.xbib.gradle.plugin.rpm'
		project.task([type: Rpm], 'buildRpm', {})
		project.tasks.buildRpm.execute()
		project.tasks.clean.execute()
		then:
		noExceptionThrown()
	}

    def 'category_on_spec'() {
        project.version = '1.0.0'

        File bananaFile = new File(projectDir, 'test/banana')
        bananaFile.parentFile.mkdirs()
        bananaFile.withWriter { out -> out.write('banana') }

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        File appleFile = new File(srcDir, fruit)
        appleFile.withWriter { out -> out.write(fruit) }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = (Rpm) project.task([type: Rpm], 'buildRpm', {
            addParentDirs = true
            from(bananaFile.getParentFile()) {
                into '/usr/share/myproduct/etc'
                createDirectoryEntry false
            }
            from(appleFile.getParentFile()) {
                into '/usr/local/myproduct/bin'
                createDirectoryEntry true
            }
        })

        when:
        rpmTask.execute()

        then:
        def files = RpmReader.read(rpmTask.getArchivePath().toPath()).files
        ['./usr/local/myproduct', './usr/local/myproduct/bin', './usr/local/myproduct/bin/apple', './usr/share/myproduct', './usr/share/myproduct/etc', './usr/share/myproduct/etc/banana'] == files*.name
        [ DIR, DIR, FILE, DIR, DIR, FILE] == files*.type

    }

    def 'filter_expression'() {

        project.version = '1.0.0'
        File appleFile = new File(projectDir, 'src/apple')
        appleFile.parentFile.mkdirs()
        appleFile.withWriter { out -> out.write('{{BASE}}/apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = (Rpm) project.task([type: Rpm], 'buildRpm') {
            from(appleFile.getParentFile()) {
                into '/usr/local/myproduct/bin'
                filter({ line ->
                    return line.replaceAll(/\{\{BASE\}\}/, '/usr/local/myproduct')
                })
            }
        }

        when:
        rpmTask.execute()

        then:
        def res = RpmReader.read(rpmTask.getArchivePath().toPath())
        def scannerApple = res.files.find { it.name =='./usr/local/myproduct/bin/apple'}
        scannerApple.asString() == '/usr/local/myproduct/apple'
    }

    def 'usesArchivesBaseName'() {

        // archivesBaseName is an artifact of the BasePlugin, and won't exist until it's applied.
        project.apply plugin: BasePlugin
        project.archivesBaseName = 'foo'

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        new File(srcDir, fruit).withWriter { out ->
            out.write(fruit)
        }
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        when:
        project.task([type: Rpm], 'buildRpm', {})

        then:
        'foo' == project.buildRpm.packageName

        when:
        project.tasks.buildRpm.execute()

        then:
        noExceptionThrown()
    }

    def 'verifyValuesCanComeFromExtension'() {

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        new File(srcDir, fruit).withWriter { out ->
            out.write(fruit)
        }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        def parentExten = project.extensions.create('rpmParent', ProjectPackagingExtension, project)

        Rpm rpmTask = project.task([type: Rpm], 'buildRpm')
        rpmTask.permissionGroup = 'GROUP'
        rpmTask.requires('openjdk')
        rpmTask.link('/dev/null', '/dev/random')

        when:
        parentExten.user = 'USER'
        parentExten.permissionGroup = 'GROUP2'
        parentExten.requires('java')
        parentExten.link('/tmp', '/var/tmp')

        project.description = 'DESCRIPTION'

        then:
        'USER' == rpmTask.user // From Extension
        'GROUP' == rpmTask.permissionGroup // From task, overriding extension
        'DESCRIPTION' == rpmTask.packageDescription // From Project, even though extension could have a value
        2 == rpmTask.getAllLinks().size()
        2 == rpmTask.getAllDependencies().size()
    }

    def 'verifyCopySpecCanComeFromExtension'() {
        setup:

        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        new File(srcDir, fruit).withWriter { out ->
            out.write(fruit)
        }

        File etcDir = new File(projectDir, 'etc')
        etcDir.mkdirs()
        new File(etcDir, 'banana.conf').text = 'banana=true'

        // Simulate SystemPackagingBasePlugin
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        ProjectPackagingExtension parentExten = project.extensions.create('rpmParent', ProjectPackagingExtension, project)

        // Configure
        Rpm rpmTask = (Rpm) project.task([type: Rpm, name:'buildRpm']) {
            release 3
        }
        project.version = '1.0'

        rpmTask.from(srcDir) {
            into('/usr/local/src')
        }
        parentExten.from(etcDir) {
            createDirectoryEntry true
            into('/conf/defaults')
        }

        // Execute
        when:
        rpmTask.execute()

        then:
        // Evaluate response
        rpmTask.getArchivePath().exists()
        def res = RpmReader.read(rpmTask.getArchivePath().toPath())
        // Parent will come first
        ['./conf', './conf/defaults', './conf/defaults/banana.conf', './usr/local/src', './usr/local/src/apple'] == res.files*.name
        [DIR, DIR, FILE, DIR, FILE] == res.files*.type
    }

    def 'differentUsersBetweenCopySpecs'() {

        def srcDir = [new File(projectDir, 'src1'),
                      new File(projectDir, 'src2'),
                      new File(projectDir, 'src3')]
        def fruits = ['apple', 'banana', 'cherry']
        srcDir.eachWithIndex { file, idx ->
            file.mkdirs()
            String fruit = fruits[idx]
            new File(file, fruit).withWriter { out ->
                out.write(fruit)
            }
        }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            user = 'default'

            from(srcDir[0]) {
                user 'user1'
                // user = 'user1' // Won't work, since setter via Categories won't pass hasProperty
            }

            from(srcDir[1]) {
                // should be default user
            }

            from(srcDir[2]) {
                user 'user2'
            }
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm').toPath())
        [DIR, FILE, FILE, FILE] == res.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == res.files*.name
        ['user1', 'user1', 'default', 'user2'] == res.format.header.getEntry(FILEUSERNAME).values.toList()
    }

    def 'differentGroupsBetweenCopySpecs'() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        
        def fruits = ['apple', 'banana', 'cherry']
        def srcDir = [new File(buildDir, 'src1'), new File(buildDir, 'src2'), new File(buildDir, 'src3')]
        srcDir.eachWithIndex { file, idx ->
            file.mkdirs()
            String word = fruits[idx]
            new File(file, word).withWriter { out ->
                out.write(word)
            }
        }
        
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            permissionGroup 'default'

            from(srcDir[0]) {
                // should be default group
            }

            from(srcDir[1]) {
                //setPermissionGroup 'group2' // works
                //permissionGroup = 'group2' // Does not work
                permissionGroup 'group2' // Does not work
            }

            from(srcDir[2]) {
                // should be default group
            }
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm').toPath())
        [DIR, FILE, FILE, FILE] == res.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == res.files*.name
        def allFiles = res.files
        def groups = res.format.header.getEntry(FILEGROUPNAME).values
        ['default', 'default', 'group2', 'default'] == res.format.header.getEntry(FILEGROUPNAME).values.toList()
    }

    def 'differentPermissionsBetweenCopySpecs'() {
        File srcDir1 = new File(projectDir, 'src1')
        File srcDir2 = new File(projectDir, 'src2')
        File srcDir3 = new File(projectDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        new File(srcDir1, 'apple').withWriter { out -> out.write('apple') }
        new File(srcDir2, 'banana').withWriter { out -> out.write('banana') }
        new File(srcDir3, 'cherry').withWriter { out -> out.write('cherry') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            fileMode 0555

            from(srcDir1) {
                // should be default group
            }

            from(srcDir2) {
                fileMode 0666
            }

            from(srcDir3) {
                fileMode 0555
            }
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm').toPath())
        [DIR, FILE, FILE, FILE] == res.files*.type
        ['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'] == res.files*.name

        // #define S_IFIFO  0010000  /* named pipe (fifo) */
        // #define S_IFCHR  0020000  /* character special */
        // #define S_IFDIR  0040000  /* directory */
        // #define S_IFBLK  0060000  /* block special */
        // #define S_IFREG  0100000  /* regular */
        // #define S_IFLNK  0120000  /* symbolic link */
        // #define S_IFSOCK 0140000  /* socket */
        // #define S_ISUID  0004000 /* set user id on execution */
        // #define S_ISGID  0002000 /* set group id on execution */
        // #define S_ISTXT  0001000 /* sticky bit */
        // #define S_IRWXU  0000700 /* RWX mask for owner */
        // #define S_IRUSR  0000400 /* R for owner */
        // #define S_IWUSR  0000200 /* W for owner */
        // #define S_IXUSR  0000100 /* X for owner */
        // #define S_IRWXG  0000070 /* RWX mask for group */
        // #define S_IRGRP  0000040 /* R for group */
        // #define S_IWGRP  0000020 /* W for group */
        // #define S_IXGRP  0000010 /* X for group */
        // #define S_IRWXO  0000007 /* RWX mask for other */
        // #define S_IROTH  0000004 /* R for other */
        // #define S_IWOTH  0000002 /* W for other */
        // #define S_IXOTH  0000001 /* X for other */
        // #define S_ISVTX  0001000 /* save swapped text even after use */

        // drwxr-xr-x is 0040755
        // NOTE: Not sure why directory is getting user write permission
        [(short)0040755, (short)0100555, (short)0100666, (short)0100555] == res.format.header.getEntry(FILEMODES).values.toList()
    }

    def 'no Prefix Value'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)
        })

        when:
        rpmTask.execute()

        then:
        def scan = RpmReader.read(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm').toPath())
        null == RpmReader.getHeaderEntry(scan, PREFIXES)
    }

    def 'one Prefix Value'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes '/opt/myprefix'
        })

        when:
        rpmTask.execute()

        then:
        def scan = RpmReader.read(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm').toPath())
        '/opt/myprefix' == RpmReader.getHeaderEntryString(scan, PREFIXES)
    }

    def 'multiple Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes '/opt/myprefix', '/etc/init.d'
        })

        when:
        rpmTask.execute()

        then:
        def scan = RpmReader.read(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm').toPath())
        // NOTE: Scanner just jams things together as one string
        '/opt/myprefix/etc/init.d' == RpmReader.getHeaderEntryString(scan, PREFIXES)
    }

    def 'multiple Added then cleared Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'one-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes '/opt/myprefix', '/etc/init.d'
            prefixes.clear()
        })

        when:
        rpmTask.execute()

        then:
        def scan = RpmReader.read(project.file('build/tmp/RpmPluginTest/one-prefix-1.0-1.i386.rpm').toPath())
        null == RpmReader.getHeaderEntry(scan, PREFIXES)
    }

    def 'direct assignment of Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'multi-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX

            into '/opt/myprefix'
            from (srcDir)

            prefixes = ['/opt/myprefix', '/etc/init.d']
        })

        when:
        rpmTask.execute()

        then:
        def scan = RpmReader.read(project.file('build/tmp/RpmPluginTest/multi-prefix-1.0-1.i386.rpm').toPath())
        // NOTE: Scanner just jams things together as one string
        '/opt/myprefix/etc/init.d' == RpmReader.getHeaderEntryString(scan, PREFIXES)
    }

    def 'ospackage assignment of Prefix Values'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        //project.ospackage { prefixes = ['/opt/ospackage', '/etc/maybe'] }

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'multi-prefix'
            version = '1.0'
            release = '1'
            arch = I386
            os = LINUX
            prefix '/apps'

            into '/opt/myprefix'
            from (srcDir)
        })

        when:
        rpmTask.execute()

        then:
        def scan = RpmReader.read(project.file('build/tmp/RpmPluginTest/multi-prefix-1.0-1.i386.rpm').toPath())
        // NOTE: Scanner just jams things together as one string
        def foundPrefixes = RpmReader.getHeaderEntry(scan, PREFIXES)
        foundPrefixes.values.contains('/apps')
        //foundPrefixes.values.contains('/opt/ospackage')
        //foundPrefixes.values.contains('/etc/maybe')
    }

    def 'Avoids including empty directories'() {
        Project project = ProjectBuilder.builder().build()

        File myDir = new File(projectDir, 'my')
        File contentDir = new File(myDir, 'own/content')
        contentDir.mkdirs()
        new File(contentDir, 'myfile.txt').withWriter { out -> out.write('test') }

        File emptyDir = new File(myDir, 'own/empty')
        emptyDir.mkdirs()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            from(myDir) {
                addParentDirs false
            }
            includeEmptyDirs false
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        res.files*.name.every { './own/content/myfile.txt'.startsWith(it) }
    }

    def 'Can create empty directories'() {
        Project project = ProjectBuilder.builder().build()

        File myDir = new File(projectDir, 'my')
        File contentDir = new File(myDir, 'own/content')
        contentDir.mkdirs()
        new File(contentDir, 'myfile.txt').withWriter { out -> out.write('test') }

        File otherDir = new File(projectDir, 'other')
        otherDir.mkdirs()
        File someDir = new File(otherDir, 'some')
        someDir.mkdirs()
        File emptyDir = new File(someDir, 'empty')
        emptyDir.mkdirs()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            from(myDir) {
                addParentDirs false
            }

            from(someDir) {
                into '/inside/the/archive'
                addParentDirs false
                createDirectoryEntry true
            }

            directory('/using/the/dsl')
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        res.files*.name.containsAll(['./inside/the/archive/empty', './own/content/myfile.txt', './using/the/dsl'])
        res.files*.type.containsAll([DIR, FILE])
    }

    def 'Sets owner and group for directory DSL'() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            user 'test'
            permissionGroup 'test'

            directory('/using/the/dsl')
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        res.files*.name == ['./using/the/dsl']
        res.files*.type == [DIR]
        res.format.header.getEntry(FILEGROUPNAME).values.toList() == ['test']
    }

    def 'has epoch value'() {
        given:
        Project project = ProjectBuilder.builder().build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        def rpmTask = project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'has-epoch'
            version = '1.0'
            release = '1'
            epoch = 2
            arch = I386
            os = LINUX

            into '/opt/bleah'
            from (srcDir)
        })

        when:
        rpmTask.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/has-epoch-1.0-1.i386.rpm').toPath())
        2 == RpmReader.getHeaderEntry(res, EPOCH).values[0]
    }

    def 'Does not include signature header if signing is not fully configured'() {
        given:
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        res.format.signatureHeader.getEntry(SignatureTag.LEGACY_PGP) == null
    }

    def 'Does include signature header'() {
        given:
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            signingKeyId = 'F02C6D2C'
            signingKeyPassphrase = 'test'
            signingKeyRing = 'src/test/resources/pgp/test-secring.gpg'
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        res.format.signatureHeader.getEntry(SignatureTag.LEGACY_PGP) != null
    }

    /**
     * Verifies that a symlink can be preserved.
     *
     * The following directory structure is assumed:
     *
     * .
     * └── usr
     *     └── bin
     *         ├── foo -> foo-1.2
     *         └── foo-1.2
     *             └── foo.txt
     */
    def 'Preserves symlinks'() {
        setup:
        File symlinkDir = new File(projectDir, 'symlink')
        File binDir = new File(symlinkDir, 'usr/bin')
        File fooDir = new File(binDir, 'foo-1.2')
        binDir.mkdirs()
        fooDir.mkdirs()
        new File(fooDir, 'foo.txt').withWriter { out -> out.write('foo') }
        Files.createSymbolicLink(binDir.toPath().resolve('foo'), fooDir.toPath())

        when:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Task task = project.task('buildRpm', type: Rpm) {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386

            from(symlinkDir) {
                createDirectoryEntry true
            }
        }

        task.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        res.files*.name == ['./usr', './usr/bin', './usr/bin/foo', './usr/bin/foo-1.2', './usr/bin/foo-1.2/foo.txt']
        res.files*.type == [DIR, DIR, SYMLINK, DIR, FILE]
    }

    def "Does not throw UnsupportedOperationException when copying external artifact with createDirectoryEntry option"() {
        given:
        String testCoordinates = 'com.netflix.nebula:a:1.0.0'
        DependencyGraph graph = new DependencyGraph([testCoordinates])
        File reposRootDir = new File(project.buildDir, 'repos')
        GradleDependencyGenerator generator = new GradleDependencyGenerator(graph, reposRootDir.absolutePath)
        generator.generateTestMavenRepo()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.configurations {
            myConf
        }

        project.dependencies {
            myConf testCoordinates
        }

        project.repositories {
            maven {
                url {
                    "file://$reposRootDir/mavenrepo"
                }
            }
        }

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            packageName = 'bleah'

            from(project.configurations.myConf) {
                createDirectoryEntry = true
                into('root/lib')
            }
        }

        when:
        rpmTask.execute()

        then:
        noExceptionThrown()
    }

    @Unroll
    def "Translates package description '#description' to header entry"() {
        given:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            version = '1.0'
            packageName = 'bleah'
            packageDescription = description
        }

        when:
        rpmTask.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm').toPath())
        RpmReader.getHeaderEntryString(res, DESCRIPTION) == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'This is a description'
        ''                      | ''
        null                    | ''
    }

    @Unroll
    def "Translates project description '#description' to header entry"() {
        given:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.description = description

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            version = '1.0'
            packageName = 'bleah'
        }

        when:
        rpmTask.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm').toPath())
        RpmReader.getHeaderEntryString(res, DESCRIPTION) == headerEntry

        where:
        description             | headerEntry
        'This is a description' | 'This is a description'
        ''                      | ''
        null                    | ''
    }

    def "Can set user and group for packaged files"() {
        given:
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            version = '1.0'
            packageName = 'bleah'

            from(srcDir) {
                user = 'me'
                permissionGroup = 'awesome'
            }
        }

        when:
        rpmTask.execute()

        then:
        Header header = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm').toPath()).format.header
        ['awesome'] == header.getEntry(FILEGROUPNAME).values.toList()
        ['me'] == header.getEntry(FILEUSERNAME).values.toList()
    }

    def "Can set multiple users and groups for packaged files"() {
        given:
        File srcDir = new File(projectDir, 'src')
        File scriptDir = new File(projectDir, 'script')
        srcDir.mkdirs()
        scriptDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }
        new File(scriptDir, 'orange').withWriter { out -> out.write('orange') }
        new File(scriptDir, 'banana').withWriter { out -> out.write('banana') }

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Rpm rpmTask = project.task('buildRpm', type: Rpm) {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            version = '1.0'
            packageName = 'bleah'

            user 'defaultUser'
            permissionGroup 'defaultGroup'

            from(srcDir) {
                user 'me'
                permissionGroup 'awesome'
            }

            from(scriptDir) {
                into '/etc'
            }
        }

        when:
        rpmTask.execute()

        then:
        Header header = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0.noarch.rpm').toPath()).format.header
        ['awesome', 'defaultGroup', 'defaultGroup'] == header.getEntry(FILEGROUPNAME).values.toList()
        ['me', 'defaultUser', 'defaultUser'] == header.getEntry(FILEUSERNAME).values.toList()
    }

    @Unroll
    def 'handle semantic versions with dashes and metadata (+) expect #version to be #expected'() {
        given:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.version = version

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()
            packageName = 'semvertest'
        })

        project.tasks.buildRpm.execute()

        expect:
        project.file("build/tmp/RpmPluginTest/semvertest-${expected}.noarch.rpm").exists()

        where:
        version              | expected
        '1.0'                | '1.0'
        '1.0.0'              | '1.0.0'
        '1.0.0-rc.1'         | '1.0.0~rc.1'
        '1.0.0-dev.3+abc219' | '1.0.0~dev.3'
    }

    def 'handles multiple provides'() {
        given:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.version = '1.0'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()
            packageName = 'providesTest'
            provides 'foo'
            provides 'bar'
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/providesTest-1.0.noarch.rpm').toPath())
        def provides = RpmReader.getHeaderEntry(res, PROVIDENAME)
        ['foo', 'bar'].every { it in provides.values }
    }

    def 'Add preTrans and postTrans scripts'() {
        given:
        Path prescript = Paths.get(projectDir.toString(), 'prescript')
        Path postscript = Paths.get(projectDir.toString(), 'postscript')
        prescript.withWriter { out -> out.write('MyPreTransScript') }
        postscript.withWriter { out -> out.write('MyPostTransScript') }

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            arch = I386

            preTrans prescript
            postTrans postscript
        })

        when:
        project.tasks.buildRpm.execute()

        then:
        def res = RpmReader.read(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm').toPath())
        def PRE_TRANS_HEADER_INDEX = 1151
        def POST_TRANS_HEADER_INDEX = 1152
        res.format.header.entries[PRE_TRANS_HEADER_INDEX].values[0].contains('MyPreTransScript')
        res.format.header.entries[POST_TRANS_HEADER_INDEX].values[0].contains('MyPostTransScript')
    }

    def 'preserve symlinks without closure'() {
        given:
        Path target = Files.createTempFile("file-to-symlink-to", "sh")
        File file = project.file('bin/my-symlink')
        file.parentFile.mkdirs()
        Files.createSymbolicLink(Paths.get(file.path), target)

        when:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {
            from 'bin'
        })
        rpmTask.execute()

        then:
        def res = RpmReader.read(rpmTask.getArchivePath().toPath())
        def symlink = res.files.find { it.name == 'my-symlink' }
        symlink.header.type == SYMLINK
    }

    def 'preserve symlinks with closure'() {
        given:
        Path target = Files.createTempFile("file-to-symlink-to", "sh")
        File file = project.file('bin/my-symlink')
        file.parentFile.mkdirs()
        Files.createSymbolicLink(file.toPath(), target)

        when:
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'

        Rpm rpmTask = project.task([type: Rpm], 'buildRpm', {
            from('bin') {
                into 'lib'
            }
        })
        rpmTask.execute()

        then:
        def res = RpmReader.read(rpmTask.getArchivePath().toPath())
        def symlink = res.files.find { it.name == 'lib/my-symlink' }
        symlink.header.type == SYMLINK
    }
}
