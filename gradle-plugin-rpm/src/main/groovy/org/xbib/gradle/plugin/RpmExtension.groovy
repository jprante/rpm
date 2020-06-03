package org.xbib.gradle.plugin

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.xbib.rpm.Dependency
import org.xbib.rpm.Directory
import org.xbib.rpm.Link
import org.xbib.rpm.lead.Architecture
import org.xbib.rpm.lead.Os
import org.xbib.rpm.lead.PackageType

class RpmExtension {

    @Optional
    @Input
    boolean enabled = true

    @Optional
    @Input
    String packageName

    @Optional
    @Input
    String packageVersion

    @Optional
    @Input
    String packageRelease

    @Optional
    @Input
    Integer epoch = 0

    @Optional
    @Input
    String signingKeyPassphrase

    @Optional
    @Input
    String signingKeyRing

    @Optional
    @Input
    String signingKeyId

    @Optional
    @Input
    String signingKeyHashAlgo

    @Optional
    @Input
    String user

    @Optional
    @Input
    String group

    @Optional
    @Input
    String buildHost

    @Optional
    @Input
    String packageGroup

    @Optional
    @Input
    String packageDescription = ''

    @Optional
    @Input
    String summary

    @Optional
    @Input
    String license

    @Optional
    @Input
    String packager

    @Optional
    @Input
    String distribution

    @Optional
    @Input
    String vendor

    @Optional
    @Input
    String url

    @Optional
    @Input
    String sourcePackage

    @Optional
    @Input
    List<String> fileType

    @Optional
    @Input
    Boolean addParentDirs = false

    @Optional
    @Input
    Architecture arch = Architecture.X86_64

    @Optional
    @Input
    Os os = Os.LINUX

    @Optional
    @Input
    PackageType packageType = PackageType.BINARY

    @Input
    List<String> prefixes = new ArrayList<String>()

    @Optional
    @Input
    Integer uid

    @Optional
    @Input
    Integer gid

    @Optional
    @Input
    String maintainer

    @Optional
    @Input
    String uploaders

    @Optional
    @Input
    String priority

    @Optional
    @Input
    String preInstall

    @Optional
    @Input
    File preInstallFile

    @Input
    List<Object> preInstallCommands = []

    @Optional
    @Input
    String postInstall

    @Optional
    @Input
    File postInstallFile

    @Input
    List<Object> postInstallCommands = []

    @Optional
    @Input
    String preUninstall

    @Optional
    @Input
    File preUninstallFile

    @Input
    List<Object> preUninstallCommands = []

    @Optional
    @Input
    String postUninstall

    @Optional
    @Input
    File postUninstallFile

    @Input
    List<Object> postUninstallCommands = []

    @Optional
    @Input
    String preTrans

    @Optional
    @Input
    File preTransFile

    @Input
    List<Object> preTransCommands = []

    @Optional
    @Input
    String postTrans

    @Optional
    @Input
    File postTransFile

    @Input
    List<Object> postTransCommands = []

    @Input
    List<Object> commonCommands = []

    @Input
    List<Link> links = []

    Link link(String path, String target) {
        link(path, target, -1)
    }

    Link link(String path, String target, int permissions) {
        Link link = new Link(path, target, permissions)
        links.add(link)
        link
    }

    @Input
    List<Dependency> dependencies = []

    @Input
    List<Dependency> obsoletes = []

    @Input
    List<Dependency> conflicts = []

    @Input
    List<Dependency> recommends = []

    @Input
    List<Dependency> suggests = []

    @Input
    List<Dependency> enhances = []

    @Input
    List<Dependency> preDepends = []

    @Input
    List<Dependency> breaks = []

    @Input
    List<Dependency> replaces = []

    @Input
    List<Dependency> provides = []

    Dependency requires(String packageName) {
        requires(packageName, '')
    }

    Dependency requires(String packageName, String version){
        requires(packageName, version, 0)
    }

    Dependency requires(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        dependencies.add(dep)
        dep
    }

    Dependency obsoletes(String packageName) {
        obsoletes(packageName, '')
    }

    Dependency obsoletes(String packageName, String version) {
        obsoletes(packageName, version, 0)
    }

    Dependency obsoletes(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        obsoletes.add(dep)
        dep
    }

    Dependency conflicts(String packageName) {
        conflicts(packageName, '')
    }

    Dependency conflicts(String packageName, String version) {
        conflicts(packageName, version, 0)
    }

    Dependency conflicts(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        conflicts.add(dep)
        dep
    }

    Dependency recommends(String packageName) {
        recommends(packageName, '')
    }

    Dependency recommends(String packageName, String version) {
        recommends(packageName, version, 0)
    }

    Dependency recommends(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        recommends.add(dep)
        dep
    }

    Dependency suggests(String packageName) {
        suggests(packageName, '')
    }

    Dependency suggests(String packageName, String version) {
        suggests(packageName, version, 0)
    }

    Dependency suggests(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        suggests.add(dep)
        dep
    }

    Dependency enhances(String packageName) {
        enhances(packageName, '')
    }

    Dependency enhances(String packageName, String version) {
        enhances(packageName, version, 0)
    }

    Dependency enhances(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        enhances.add(dep)
        dep
    }

    Dependency preDepends(String packageName) {
        preDepends(packageName, '')
    }

    Dependency preDepends(String packageName, String version) {
        preDepends(packageName, version, 0)
    }

    Dependency preDepends(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        preDepends.add(dep)
        dep
    }

    Dependency breaks(String packageName) {
        breaks(packageName, '')
    }

    Dependency breaks(String packageName, String version) {
        breaks(packageName, version, 0)
    }

    Dependency breaks(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        breaks.add(dep)
        dep
    }

    Dependency replaces(String packageName) {
        replaces(packageName, '')
    }

    Dependency replaces(String packageName, String version) {
        replaces(packageName, version, 0)
    }

    Dependency replaces(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        replaces.add(dep)
        dep
    }

    Dependency provides(String packageName) {
        provides(packageName, '')
    }

    Dependency provides(String packageName, String version) {
        provides(packageName, version, 0)
    }

    Dependency provides(String packageName, String version, int flag) {
        def dep = new Dependency(packageName, version, flag)
        provides.add(dep)
        dep
    }

    @Input
    List<Directory> directories = []

    Directory directory(String path) {
        Directory directory = new Directory(path: path)
        directories << directory
        directory
    }

    @Optional
    @Input
    File changeLogFile

    @Optional
    @Input
    String changeLog
}
