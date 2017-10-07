package org.xbib.rpm.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;
import org.xbib.rpm.payload.CpioHeader;
import org.xbib.rpm.payload.Directive;
import org.xbib.rpm.payload.EmptyDir;
import org.xbib.rpm.payload.Ghost;
import org.xbib.rpm.payload.Link;
import org.xbib.rpm.trigger.TriggerIn;
import org.xbib.rpm.trigger.TriggerPostUn;
import org.xbib.rpm.trigger.TriggerPreIn;
import org.xbib.rpm.trigger.TriggerUn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Ant task for creating an RPM archive.
 */
public class RpmTask extends Task {

    private String name;

    private String epoch = "0";

    private String version;

    private String group;

    private String release = "1";

    private String host;

    private String summary = "";

    private String description = "";

    private String license = "";

    private String packager = System.getProperty("user.name", "");

    private String distribution = "";

    private String vendor = "";

    private String url = "";

    private String sourcePackage = null;

    private String provides;

    private String prefixes;

    private PackageType type = PackageType.BINARY;

    private Architecture architecture = Architecture.NOARCH;

    private Os os = Os.LINUX;

    private Path destination;

    private List<ArchiveFileSet> filesets = new ArrayList<>();

    private List<EmptyDir> emptyDirs = new ArrayList<>();

    private List<Ghost> ghosts = new ArrayList<>();

    private List<Link> links = new ArrayList<>();

    List<Depends> depends = new ArrayList<>();

    private List<Provides> moreProvides = new ArrayList<>();

    private List<Conflicts> conflicts = new ArrayList<>();

    private List<Obsoletes> obsoletes = new ArrayList<>();

    private List<TriggerPreIn> triggersPreIn = new ArrayList<>();

    private List<TriggerIn> triggersIn = new ArrayList<>();

    private List<TriggerUn> triggersUn = new ArrayList<>();

    private List<TriggerPostUn> triggersPostUn = new ArrayList<>();

    private List<BuiltIn> builtIns = new ArrayList<>();

    private Path preTransScript;

    private Path preInstallScript;

    private Path postInstallScript;

    private Path preUninstallScript;

    private Path postUninstallScript;

    private Path postTransScript;

    private InputStream privateKeyRing;

    private Long privateKeyId;

    private String privateKeyPassphrase;

    private Path changeLog;

    public RpmTask() {
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "";
        }
    }

    @Override
    public void execute() {
        if (name == null) {
            throw new BuildException("attribute 'name' is required");
        }
        if (version == null) {
            throw new BuildException("attribute 'version' is required");
        }
        if (group == null) {
            throw new BuildException("attribute 'group' is required");
        }
        Integer numEpoch;
        try {
            numEpoch = Integer.parseInt(epoch);
        } catch (Exception e) {
            throw new BuildException("epoch must be integer: " + epoch);
        }
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage(name, version, release, numEpoch);
        rpmBuilder.setType(type);
        rpmBuilder.setPlatform(architecture, os);
        rpmBuilder.setGroup(group);
        rpmBuilder.setBuildHost(host);
        rpmBuilder.setSummary(summary);
        rpmBuilder.setDescription(description);
        rpmBuilder.setLicense(license);
        rpmBuilder.setPackager(packager);
        rpmBuilder.setDistribution(distribution);
        rpmBuilder.setVendor(vendor);
        rpmBuilder.setUrl(url);
        if (provides != null) {
            rpmBuilder.setProvides(provides);
        }
        rpmBuilder.setPrefixes(prefixes == null ? null : prefixes.split(","));
        rpmBuilder.setPrivateKeyRing(privateKeyRing);
        rpmBuilder.setPrivateKeyId(privateKeyId);
        rpmBuilder.setPrivateKeyPassphrase(privateKeyPassphrase);
        if (sourcePackage != null) {
            rpmBuilder.addHeaderEntry(HeaderTag.SOURCERPM, sourcePackage);
        }
        for (BuiltIn builtIn : builtIns) {
            String text = builtIn.getText();
            if (text != null && !text.trim().equals("")) {
                rpmBuilder.addBuiltinDirectory(builtIn.getText());
            }
        }
        try {
            if (preTransScript != null) {
                rpmBuilder.setPreTrans(preTransScript);
            }
            if (preInstallScript != null) {
                rpmBuilder.setPreInstall(preInstallScript);
            }
            if (postInstallScript != null) {
                rpmBuilder.setPostInstall(postInstallScript);
            }
            if (preUninstallScript != null) {
                rpmBuilder.setPreUninstall(preUninstallScript);
            }
            if (postUninstallScript != null) {
                rpmBuilder.setPostUninstall(postUninstallScript);
            }
            if (postTransScript != null) {
                rpmBuilder.setPostTrans(postTransScript);
            }
            if (changeLog != null) {
                rpmBuilder.addChangelog(changeLog);
            }
            for (EmptyDir emptyDir : emptyDirs) {
                rpmBuilder.addDirectory(emptyDir.getPath(), emptyDir.getDirmode(), EnumSet.of(Directive.NONE),
                        emptyDir.getUsername(), emptyDir.getGroup(), true);
            }
            for (ArchiveFileSet fileset : filesets) {
                Path archive = fileset.getSrc(getProject()) != null ?
                        fileset.getSrc(getProject()).toPath() : null;
                String prefix = CpioHeader.normalizePath(fileset.getPrefix(getProject()));
                if (!prefix.endsWith("/")) {
                    prefix += "/";
                }
                DirectoryScanner directoryScanner = fileset.getDirectoryScanner(getProject());
                Integer filemode = fileset.getFileMode(getProject()) & 4095;
                Integer dirmode = fileset.getDirMode(getProject()) & 4095;
                String username = null;
                String group = null;
                EnumSet<Directive> directive = null;
                if (fileset instanceof TarFileSet) {
                    TarFileSet tarFileSet = (TarFileSet) fileset;
                    username = tarFileSet.getUserName();
                    group = tarFileSet.getGroup();
                    if (fileset instanceof RpmFileSet) {
                        RpmFileSet rpmFileSet = (RpmFileSet) fileset;
                        directive = rpmFileSet.getDirectives();
                    }
                }
                for (String entry : directoryScanner.getIncludedDirectories()) {
                    String dir = CpioHeader.normalizePath(prefix + entry);
                    if (!"".equals(entry)) {
                        rpmBuilder.addDirectory(dir, dirmode, directive, username, group, true);
                    }
                }
                for (String entry : directoryScanner.getIncludedFiles()) {
                    if (archive != null) {
                        URL url = new URL("jar:" + archive.toUri().toURL() + "!/" + entry);
                        rpmBuilder.addURL(prefix + entry, url, filemode, dirmode, directive, username, group);
                    } else {
                        Path path = directoryScanner.getBasedir().toPath().resolve(entry);
                        rpmBuilder.addFile(prefix + entry, path, filemode, dirmode, directive, username, group);
                    }
                }
            }
            for (Ghost ghost : ghosts) {
                rpmBuilder.addFile(ghost.getPath(), null, ghost.getFilemode(), ghost.getDirmode(),
                        ghost.getDirectives(), ghost.getUsername(), ghost.getGroup());
            }
            for (Link link : links) {
                rpmBuilder.addLink(link.getPath(), link.getTarget(), link.getPermissions());
            }
            for (Depends dependency : depends) {
                rpmBuilder.addDependency(dependency.getName(), dependency.getComparison(), dependency.getVersion());
            }
            for (Provides provision : moreProvides) {
                rpmBuilder.addProvides(provision.getName(), provision.getVersion());
            }
            for (Conflicts conflict : conflicts) {
                rpmBuilder.addConflicts(conflict.getName(), conflict.getComparison(), conflict.getVersion());
            }
            for (Obsoletes obsoletion : obsoletes) {
                rpmBuilder.addObsoletes(obsoletion.getName(), obsoletion.getComparison(), obsoletion.getVersion());
            }
            for (TriggerPreIn triggerPreIn : triggersPreIn) {
                rpmBuilder.addTrigger(triggerPreIn.getScript(), "", triggerPreIn.getDepends(),
                        triggerPreIn.getFlag());
            }
            for (TriggerIn triggerIn : triggersIn) {
                rpmBuilder.addTrigger(triggerIn.getScript(), "", triggerIn.getDepends(), triggerIn.getFlag());
            }
            for (TriggerUn triggerUn : triggersUn) {
                rpmBuilder.addTrigger(triggerUn.getScript(), "", triggerUn.getDepends(), triggerUn.getFlag());
            }
            for (TriggerPostUn triggerPostUn : triggersPostUn) {
                rpmBuilder.addTrigger(triggerPostUn.getScript(), "", triggerPostUn.getDepends(),
                        triggerPostUn.getFlag());
            }
            rpmBuilder.build(destination);
        } catch (IOException | RpmException e) {
            throw new BuildException("error while building package", e);
        }
    }

    public void restrict(String name) {
        depends.removeIf(dependency -> dependency.getName().equals(name));
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    public void setType(String type) {
        this.type = PackageType.valueOf(type);
    }

    public void setArchitecture(String architecture) {
        this.architecture = Architecture.valueOf(architecture);
    }

    public void setOs(String os) {
        this.os = Os.valueOf(os);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public void setPackager(String packager) {
        this.packager = packager;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setProvides(String provides) {
        this.provides = provides;
    }

    public void setPrefixes(String prefixes) {
        this.prefixes = prefixes;
    }

    public void setDestination(Path destination) {
        this.destination = destination;
    }

    public void addZipfileset(ZipFileSet fileset) {
        filesets.add(fileset);
    }

    public void addTarfileset(TarFileSet fileset) {
        filesets.add(fileset);
    }

    public void addRpmfileset(RpmFileSet fileset) {
        filesets.add(fileset);
    }

    public void addGhost(Ghost ghost) {
        ghosts.add(ghost);
    }

    public void addEmptyDir(EmptyDir emptyDir) {
        emptyDirs.add(emptyDir);
    }

    public void addLink(Link link) {
        links.add(link);
    }

    public void addDepends(Depends dependency) {
        depends.add(dependency);
    }

    public void addProvides(Provides provision) {
        moreProvides.add(provision);
    }

    public void addConflicts(Conflicts conflict) {
        conflicts.add(conflict);
    }

    public void addObsoletes(Obsoletes obsoletion) {
        obsoletes.add(obsoletion);
    }

    public void addTriggerPreIn(TriggerPreIn triggerPreIn) {
        triggersPreIn.add(triggerPreIn);
    }

    public void addTriggerIn(TriggerIn triggerIn) {
        triggersIn.add(triggerIn);
    }

    public void addTriggerUn(TriggerUn triggerUn) {
        triggersUn.add(triggerUn);
    }

    public void addTriggerPostUn(TriggerPostUn triggerPostUn) {
        triggersPostUn.add(triggerPostUn);
    }

    public void setPreTransScript(Path preTransScript) {
        this.preTransScript = preTransScript;
    }

    public void setPreInstallScript(Path preInstallScript) {
        this.preInstallScript = preInstallScript;
    }

    public void setPostInstallScript(Path postInstallScript) {
        this.postInstallScript = postInstallScript;
    }

    public void setPreUninstallScript(Path preUninstallScript) {
        this.preUninstallScript = preUninstallScript;
    }

    public void setPostUninstallScript(Path postUninstallScript) {
        this.postUninstallScript = postUninstallScript;
    }

    public void setPostTransScript(Path postTransScript) {
        this.postTransScript = postTransScript;
    }

    public void setSourcePackage(String sourcePackage) {
        this.sourcePackage = sourcePackage;
    }

    public void setPrivateKeyRing(Path privateKeyRing) throws IOException {
        this.privateKeyRing = Files.newInputStream(privateKeyRing);
    }

    public void setPrivateKeyRing(InputStream privateKeyRing) {
        this.privateKeyRing = privateKeyRing;
    }

    public void setPrivateKeyId(Long privateKeyId) {
        this.privateKeyId = privateKeyId;
    }

    public void setPrivateKeyId(String privateKeyId) {
        this.privateKeyId = Long.decode("0x" + privateKeyId);
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public void addBuiltin(BuiltIn builtIn) {
        builtIns.add(builtIn);
    }

    public void setChangeLog(Path changeLog) {
        this.changeLog = changeLog;
    }

}
