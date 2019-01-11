package org.xbib.gradle.plugin.rpm

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.lead.Os
import org.xbib.rpm.lead.PackageType

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Extension that can be used to configure RPM.
 */
class SystemPackagingExtension {

    @Input @Optional
    String packageName

    @Input @Optional
    String release

    @Input @Optional
    String version

    @Input @Optional
    Integer epoch

    @Input @Optional
    String signingKeyPassphrase

    @Input @Optional
    String signingKeyRing

    @Input @Optional
    String signingKeyId

    @Input @Optional
    String signingKeyHashAlgo

    @Input @Optional
    String user

    @Input @Optional
    String permissionGroup

    @Input @Optional
    String packageGroup

    @Input @Optional
    String buildHost

    @Input @Optional
    String summary

    @Input @Optional
    String packageDescription

    @Input @Optional
    String license

    @Input @Optional
    String packager

    @Input @Optional
    String distribution

    @Input @Optional
    String vendor

    @Input @Optional
    String url

    @Input @Optional
    String sourcePackage

    @Input @Optional
    List<String> fileType

    @Input @Optional
    Boolean createDirectoryEntry

    @Input @Optional
    Boolean addParentDirs

    //String archStr

    @Input @Optional
    Architecture arch
    //void setArch(Object arch) {
    //    archStr = (arch instanceof Architecture) ? arch.name() : arch.toString()
    //}

    @Input @Optional
    Os os

    @Input @Optional
    PackageType type

    List<String> prefixes = new ArrayList<String>()

    def prefix(String prefixStr) {
        prefixes << prefixStr
        return this
    }

    @Input @Optional
    Integer uid

    @Input @Optional
    Integer gid

    @Input @Optional
    String maintainer

    @Input @Optional
    String uploaders

    @Input @Optional
    String priority

    @Input @Optional
    final List<Object> supplementaryControlFiles = []

    def supplementaryControl(Object file) {
        supplementaryControlFiles << file
        return this
    }

    @Input @Optional
    String preInstall

    @Input @Optional
    String postInstall

    @Input @Optional
    String preUninstall

    @Input @Optional
    String postUninstall

    @Input @Optional
    String preTrans

    @Input @Optional
    String postTrans

    final List<Object> configurationPaths = []

    final List<Object> preInstallCommands = []

    final List<Object> postInstallCommands = []

    final List<Object> preUninstallCommands = []

    final List<Object> postUninstallCommands = []

    final List<Object> preTransCommands = []

    final List<Object> postTransCommands = []

    final List<Object> commonCommands = []

    def setInstallUtils(Path script) {
        installUtils(script)
    }

    def installUtils(String script) {
        commonCommands << script
        return this
    }

    def installUtils(Path script) {
        commonCommands << script
        return this
    }

    def setConfigurationPath(String script) {
        configurationPath(script)
    }

    def configurationPath(String path) {
        configurationPaths << path
        this
    }

    def setPreInstall(String script) {
        preInstall(script)
    }
    def setPreInstall(Path script) {
        preInstall(script)
    }
    def preInstall(String script) {
        preInstall(Paths.get(script))
    }
    def preInstall(Path script) {
        if (Files.exists(script)) {
            preInstallValue(script.text)
        }
        this
    }
    def preInstallValue(String content) {
        preInstallCommands << content
        this
    }

    def setPostInstall(String script) {
        postInstall(script)
    }
    def setPostInstall(Path script) {
        postInstall(script)
    }
    def postInstall(String script) {
        postInstall(Paths.get(script))
    }
    def postInstall(Path script) {
        if (Files.exists(script)) {
            postInstallValue(script.text)
        }
        this
    }
    def postInstallValue(String content) {
        postInstallCommands << content
        this
    }

    def setPreUninstall(String script) {
        preUninstall(script)
    }
    def setPreUninstall(Path script) {
        preUninstall(script)
    }
    def preUninstall(String script) {
        preUninstall(Paths.get(script))
    }
    def preUninstall(Path script) {
        if (Files.exists(script)) {
            preUninstallValue(script.text)
        }
        this
    }
    def preUninstallValue(String script) {
        preUninstallCommands << script
        this
    }

    def setPostUninstall(String script) {
        postUninstall(script)
    }
    def setPostUninstall(Path script) {
        postUninstall(script)
    }
    def postUninstall(String script) {
        postUninstall(Paths.get(script))
    }
    def postUninstall(Path script) {
        if (Files.exists(script)) {
            postUninstallValue(script.text)
        }
        this
    }
    def postUninstallValue(String content) {
        postUninstallCommands << content
        this
    }

    def setPreTrans(String script) {
        preTrans(script)
    }
    def setPreTrans(Path script) {
        preTrans(script)
    }
    def preTrans(String script) {
        preTrans(Paths.get(script))
    }
    def preTrans(Path script) {
        if (Files.exists(script)) {
            preTransValue(script.text)
        }
        this
    }
    def preTransValue(String script) {
        preTransCommands << script
        this
    }

    def setPostTrans(String script) {
        postTrans(script)
    }
    def setPostTrans(Path script) {
        postTrans(script)
    }
    def postTrans(String script) {
        postTrans(Paths.get(script))
    }
    def postTrans(Path script) {
        if (Files.exists(script)) {
            postTransValue(script.text)
        }
        return this
    }
    def postTransValue(String script) {
        postTransCommands << script
        return this
    }

    List<Link> links = []

    Link link(String path, String target) {
        link(path, target, -1)
    }

    Link link(String path, String target, int permissions) {
        Link link = new Link()
        link.path = path
        link.target = target
        link.permissions = permissions
        links.add(link)
        link
    }

    List<Dependency> dependencies = []

    List<Dependency> obsoletes = []

    List<Dependency> conflicts = []

    List<Dependency> recommends = []

    List<Dependency> suggests = []

    List<Dependency> enhances = []

    List<Dependency> preDepends = []

    List<Dependency> breaks = []

    List<Dependency> replaces = []

    List<Dependency> provides = []

    Dependency requires(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        dependencies.add(dep)
        dep
    }

    Dependency requires(String packageName, String version){
        requires(packageName, version, 0)
    }

    Dependency requires(String packageName) {
        requires(packageName, '', 0)
    }

    Dependency obsoletes(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        obsoletes.add(dep)
        dep
    }

    Dependency obsoletes(String packageName) {
        obsoletes(packageName, '', 0)
    }

    Dependency conflicts(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        conflicts.add(dep)
        dep
    }

    Dependency conflicts(String packageName) {
        conflicts(packageName, '', 0)
    }

    Dependency recommends(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        recommends.add(dep)
        dep
    }

    Dependency recommends(String packageName) {
        recommends(packageName, '', 0)
    }

    Dependency suggests(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        suggests.add(dep)
        dep
    }

    Dependency suggests(String packageName) {
        suggests(packageName, '', 0)
    }

    Dependency enhances(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        enhances.add(dep)
        dep
    }

    Dependency enhances(String packageName) {
        enhances(packageName, '', 0)
    }

    Dependency preDepends(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        preDepends.add(dep)
        dep
    }

    Dependency preDepends(String packageName) {
        preDepends(packageName, '', 0)
    }

    Dependency breaks(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        breaks.add(dep)
        dep
    }

    Dependency breaks(String packageName) {
        breaks(packageName, '', 0)
    }

    Dependency replaces(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        replaces.add(dep)
        dep
    }

    Dependency replaces(String packageName) {
        replaces(packageName, '', 0)
    }

    Dependency provides(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        provides.add(dep)
        dep
    }

    Dependency provides(String packageName) {
        provides(packageName, '', 0)
    }

    List<Directory> directories = []

    Directory directory(String path) {
        Directory directory = directory(path, -1)
        directories << directory
        directory
    }

    Directory directory(String path, boolean addParents) {
        Directory directory = new Directory(path: path, addParents: addParents)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions) {
        Directory directory = new Directory(path: path, permissions: permissions)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions, boolean addParents) {
        Directory directory = new Directory(path: path, permissions: permissions, addParents: addParents)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions, String user, String permissionGroup) {
        Directory directory = new Directory(path: path, permissions: permissions, user: user,
                permissionGroup: permissionGroup)
        directories << directory
        directory
    }

    Directory directory(String path, int permissions, String user, String permissionGroup, boolean addParents) {
        Directory directory = new Directory(path: path, permissions: permissions, user: user,
                permissionGroup: permissionGroup, addParents: addParents)
        directories << directory
        directory
    }

    private static IllegalStateException multipleFilesDefined(String fileName) {
        new IllegalStateException("Cannot specify more than one $fileName File")
    }

    private static IllegalStateException conflictingDefinitions(String type) {
        new IllegalStateException("Cannot specify $type File and $type Commands")
    }
}
