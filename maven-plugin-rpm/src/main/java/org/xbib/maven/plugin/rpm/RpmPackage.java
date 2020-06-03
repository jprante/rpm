package org.xbib.maven.plugin.rpm;

import org.apache.maven.plugin.logging.Log;
import org.xbib.maven.plugin.rpm.mojo.RpmMojo;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.exception.SigningKeyNotFoundException;
import org.xbib.rpm.exception.UnknownArchitectureException;
import org.xbib.rpm.exception.UnknownOperatingSystemException;
import org.xbib.rpm.format.Flags;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;
import org.xbib.rpm.payload.CompressionType;
import org.xbib.rpm.security.HashAlgo;
import org.xbib.rpm.trigger.Trigger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RPM package.
 */
public class RpmPackage {
    /**
     * Plugin mojo currently in use.
     */
    private RpmMojo mojo = null;

    /**
     * Package name.
     */
    private String name = null;

    /**
     * Package version.
     */
    private String version = null;

    /**
     * Project version.
     */
    private String projectVersion = null;

    /**
     * Package release.
     */
    private String release = null;

    /**
     * Final name of RPM artifact.
     */
    private String finalName = null;

    /**
     * Package url.
     */
    private String url = null;

    /**
     * Package group.
     */
    private String group = null;

    /**
     * Package license.
     */
    private String license = null;

    /**
     * Package summary.
     */
    private String summary = null;

    /**
     * Package description.
     */
    private String description = null;

    /**
     * Package distribution.
     */
    private String distribution = null;

    /**
     * Build architecture.
     * Defaults to detected architecture of build environment if non given
     */
    private Architecture architecture = null;

    /**
     * Build operating system.
     * Defaults to detected operating system of build environment if non given
     */
    private Os operatingSystem = null;

    /**
     * Build host name.
     * Defaults to hostname of build server provided by hostname service
     */
    private String buildHostName = null;

    /**
     * Packager of RPM.
     */
    private String packager = null;

    /**
     * Source RPM Name.
     */
    private String sourceRpm = null;

    /**
     * Attach the artifact.
     */
    private boolean attach = true;

    /**
     * Artifact classifier.
     */
    private String classifier = null;

    /**
     * Pre transaction event hook script file.
     */
    private Path preTransactionScriptPath = null;

    /**
     * Pre transaction event hook script program.
     */
    private String preTransactionProgram = null;

    /**
     * Pre install event hook script file.
     */
    private Path preInstallScriptPath = null;

    /**
     * Pre install event hook program.
     */
    private String preInstallProgram = null;

    /**
     * Post install event hook script file.
     */
    private Path postInstallScriptPath = null;

    /**
     * Post install event hook program.
     */
    private String postInstallProgram = null;

    /**
     * Pre uninstall event hook script file.
     */
    private Path preUninstallScriptPath = null;

    /**
     * Pre uninstall event hook script program.
     */
    private String preUninstallProgram = null;

    /**
     * Post uninstall event hook script file.
     */
    private Path postUninstallScriptPath = null;

    /**
     * Post uninstall event hook program.
     */
    private String postUninstallProgram = null;

    /**
     * Post transaction event hook script file.
     */
    private Path postTransactionScriptPath = null;

    /**
     * Post transaction event hook program.
     */
    private String postTransactionProgram = null;

    /**
     * List of triggers.
     */
    private List<RpmTrigger> triggers = new ArrayList<>();

    /**
     * Signing key.
     */
    private String signingKey = null;

    /**
     * Signing key pass phrase.
     */
    private String signingKeyPassPhrase = null;

    /**
     * Signing key id.
     */
    private Long signingKeyId = null;

    /**
     * Prefixes.
     */
    private List<String> prefixes = new ArrayList<>();

    /**
     * Builtins.
     */
    private List<String> builtins = new ArrayList<>();

    /**
     * Dependencies.
     */
    private List<RpmPackageAssociation> dependencies = new ArrayList<>();

    /**
     * Obsoletes.
     */
    private List<RpmPackageAssociation> obsoletes = new ArrayList<>();

    /**
     * Conflicts.
     */
    private List<RpmPackageAssociation> conflicts = new ArrayList<>();

    /**
     * Links.
     */
    private List<RpmLink> links = new ArrayList<>();

    /**
     * Package file matching rules.
     */
    private List<RpmPackageRule> rules = new ArrayList<>();

    /**
     * Get mojo in use by Maven.
     *
     * @return Current maven mojo
     */
    public RpmMojo getMojo() {
        return mojo;
    }

    /**
     * Set mojo in use by Maven.
     *
     * @param mojo Current maven mojo
     */
    public void setMojo(RpmMojo mojo) {
        this.mojo = mojo;
    }

    /**
     * Get package name.
     *
     * @return Package name
     */
    public String getName() {
        if (null == name) {
            name = getMojo().getProjectArtifactId();
        }

        return name;
    }

    /**
     * Set package name.
     *
     * @param name Package name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get package version.
     *
     * @return Package version
     */
    public String getVersion() {
        if (null == version) {
            version = sanitiseVersion(getMojo().getProjectVersion());
        }

        return version;
    }

    /**
     * Set package version.
     *
     * @param version Package version
     */
    public void setVersion(String version) {
        if (null != version && version.equals("")) {
            version = null;
        }
        projectVersion = version;
        this.version = sanitiseVersion(version);
    }

    /**
     * Get project version.
     *
     * @return Project version
     */
    public String getProjectVersion() {
        if (null == projectVersion) {
            projectVersion = getMojo().getProjectVersion();
        }

        return projectVersion;
    }

    /**
     * Sanitise the version number for use in packaging.
     *
     * @param version Un-sanitised version
     * @return Sanitised version number
     */
    private String sanitiseVersion(String version) {
        if (null != version && !version.replaceAll("[a-zA-Z0-9\\.]", "").equals("")) {
            version = version.replaceAll("-", ".");
            version = version.replaceAll("[^a-zA-Z0-9\\.]", "");
        }
        return version;
    }

    /**
     * Get package release.
     *
     * @return Package release
     */
    public String getRelease() {
        if (null == release) {
            release = Long.toString(System.currentTimeMillis() / 1000);
        }
        return release;
    }

    /**
     * Set package release.
     *
     * @param release Package release
     */
    public void setRelease(String release) {
        this.release = release;
    }

    /**
     * Get final name of the RPM artifact.
     * If a final name is not set, the final name will default to {name}-{version}-{release}.{architecture}.rpm
     *
     * @return Final name of RPM artifact
     */
    public String getFinalName() {
        if (finalName == null) {
            finalName = String.format("%s-%s-%s.%s.rpm",
                    getName(), getVersion(), getRelease(),
                    getArchitecture().toString().toLowerCase());
        }
        return finalName;
    }

    /**
     * Set final name of RPM artifact.
     *
     * @param finalName Final name of RPM artifact
     */
    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    /**
     * Get package dependencies.
     *
     * @return Package dependencies
     */
    public List<RpmPackageAssociation> getDependencies() {
        return dependencies;
    }

    /**
     * Set package dependencies.
     *
     * @param dependencies Package dependencies
     */
    public void setDependencies(List<RpmPackageAssociation> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Get package obsoletes.
     *
     * @return Package obsoletes
     */
    public List<RpmPackageAssociation> getObsoletes() {
        return obsoletes;
    }

    /**
     * Set package obsoletes.
     *
     * @param obsoletes Package obsoletes
     */
    public void setObsoletes(List<RpmPackageAssociation> obsoletes) {
        this.obsoletes = obsoletes;
    }

    /**
     * Get package conflicts.
     *
     * @return Package conflicts
     */
    public List<RpmPackageAssociation> getConflicts() {
        return conflicts;
    }

    /**
     * Set package conflicts.
     *
     * @param conflicts Package conflicts
     */
    public void setConflicts(List<RpmPackageAssociation> conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Get package links.
     *
     * @return Package links
     */
    public List<RpmLink> getLinks() {
        return links;
    }

    /**
     * Set package links.
     *
     * @param links Package links
     */
    public void setLinks(List<RpmLink> links) {
        if (null != links) {
            for (RpmLink link : links) {
                link.setPackage(this);
            }
        }
        this.links = links;
    }

    /**
     * Get package url.
     *
     * @return Package url
     */
    public String getUrl() {
        if (null == url) {
            url = getMojo().getProjectUrl();
        }
        return url;
    }

    /**
     * Set package url.
     *
     * @param url Package url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get package group.
     *
     * @return Package group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Set package group.
     *
     * @param group Package group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Get package license.
     *
     * @return Package license
     */
    public String getLicense() {
        if (null == license) {
            license = getMojo().getCollapsedProjectLicense();
        }
        return license;
    }

    /**
     * Set package license.
     *
     * @param license Package license
     */
    public void setLicense(String license) {
        this.license = license;
    }

    /**
     * Get package summary.
     *
     * @return Package summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Set package summary.
     *
     * @param summary Package summary
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Get package description.
     *
     * @return Package description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set package description.
     *
     * @param description Package description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get package distribution.
     *
     * @return Package distribution
     */
    public String getDistribution() {
        return distribution;
    }

    /**
     * Set package distribution.
     *
     * @param distribution Package distribution
     */
    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    /**
     * Get package architecture.
     *
     * @return Package architecture
     */
    public Architecture getArchitecture() {
        if (null == architecture) {
            architecture = Architecture.NOARCH;
        }
        return architecture;
    }

    /**
     * Set package architecture.
     *
     * @param architecture Package architecture
     * @throws UnknownArchitectureException The architecture supplied is not recognised.
     */
    public void setArchitecture(String architecture) throws UnknownArchitectureException {
        if (null == architecture || architecture.equals("")) {
            throw new UnknownArchitectureException(architecture);
        }
        architecture = architecture.toUpperCase();
        try {
            this.architecture = Architecture.valueOf(architecture);
        } catch (IllegalArgumentException ex) {
            throw new UnknownArchitectureException(architecture, ex);
        }
    }

    /**
     * Get package operating system.
     * Defaults to LINUX if not set.
     *
     * @return Package operating system
     */
    public Os getOperatingSystem() {
        if (null == operatingSystem) {
            operatingSystem = Os.LINUX;
        }
        return operatingSystem;
    }

    /**
     * Set package operating system.
     *
     * @param operatingSystem Package operating system
     * @throws UnknownOperatingSystemException The operating system supplied is not recognised.
     */
    public void setOperatingSystem(String operatingSystem) throws UnknownOperatingSystemException {
        if (null == operatingSystem || operatingSystem.equals("")) {
            throw new UnknownOperatingSystemException(operatingSystem);
        }
        operatingSystem = operatingSystem.toUpperCase();
        try {
            this.operatingSystem = Os.valueOf(operatingSystem);
        } catch (IllegalArgumentException ex) {
            throw new UnknownOperatingSystemException(operatingSystem, ex);
        }
    }

    /**
     * Get package build host name.
     * If one is not supplied, the default hostname of the machine running the build is used.
     *
     * @return Package build host name
     * @throws UnknownHostException The build host could not be retrieved automatically.
     */
    public String getBuildHostName() throws UnknownHostException {
        if (null == buildHostName) {
            buildHostName = InetAddress.getLocalHost().getHostName();
        }
        return buildHostName;
    }

    /**
     * Set package build host name.
     *
     * @param buildHostName Package build host name
     */
    public void setBuildHostName(String buildHostName) {
        this.buildHostName = buildHostName;
    }

    /**
     * Get packager.
     *
     * @return Packager
     */
    public String getPackager() {
        return packager;
    }

    /**
     * Set package packager.
     *
     * @param packager Package packager
     */
    public void setPackager(String packager) {
        this.packager = packager;
    }

    /**
     * Get source RPM name.
     *
     * @return sourceRpm
     */
    public String getSourceRpm() {
        return sourceRpm;
    }

    /**
     * Set source RPM name.
     *
     * @param sourceRpm Package sourceRpm
     */
    public void setSourceRpm(String sourceRpm) {
        this.sourceRpm = sourceRpm;
    }

    /**
     * Get artifact attachment.
     *
     * @return Artifact is attached
     */
    public boolean isAttach() {
        return attach;
    }

    /**
     * Set artifact attachment.
     *
     * @param attach Artifact attachment
     */
    public void setAttach(boolean attach) {
        this.attach = attach;
    }

    /**
     * Get the artifact classifier.
     *
     * @return Artifact classifier
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Set the artifact classifier.
     *
     * @param classifier Artifact classifier
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * Get pre transactions script path.
     *
     * @return Pre transaction script path
     */
    public Path getPreTransactionScriptPath() {
        return preTransactionScriptPath;
    }

    /**
     * Set pre transaction script path.
     *
     * @param preTransactionScriptPath Pre transaction script path
     */
    public void setPreTransactionScriptPath(Path preTransactionScriptPath) {
        this.preTransactionScriptPath = preTransactionScriptPath;
    }

    /**
     * Get pre transaction program.
     *
     * @return Pre transaction program
     */
    public String getPreTransactionProgram() {
        return preTransactionProgram;
    }

    /**
     * Set pre transaction program.
     *
     * @param preTransactionProgram Pre transaction program
     */
    public void setPreTransactionProgram(String preTransactionProgram) {
        this.preTransactionProgram = preTransactionProgram;
    }

    /**
     * Get pre install script path.
     *
     * @return Pre install script path
     */
    public Path getPreInstallScriptPath() {
        return preInstallScriptPath;
    }

    /**
     * Set pre install script path.
     *
     * @param preInstallScriptPath Pre install script path
     */
    public void setPreInstallScriptPath(Path preInstallScriptPath) {
        this.preInstallScriptPath = preInstallScriptPath;
    }

    /**
     * Get pre install program.
     *
     * @return Pre install program
     */
    public String getPreInstallProgram() {
        return preInstallProgram;
    }

    /**
     * Set pre install program.
     *
     * @param preInstallProgram Pre install program
     */
    public void setPreInstallProgram(String preInstallProgram) {
        this.preInstallProgram = preInstallProgram;
    }

    /**
     * Get post install script path.
     *
     * @return Post install script path
     */
    public Path getPostInstallScriptPath() {
        return postInstallScriptPath;
    }

    /**
     * Set post install script path.
     *
     * @param postInstallScriptPath Post install script path
     */
    public void setPostInstallScriptPath(Path postInstallScriptPath) {
        this.postInstallScriptPath = postInstallScriptPath;
    }

    /**
     * Get post install program.
     *
     * @return Post install program
     */
    public String getPostInstallProgram() {
        return postInstallProgram;
    }

    /**
     * Set post install program.
     *
     * @param postInstallProgram Post install program
     */
    public void setPostInstallProgram(String postInstallProgram) {
        this.postInstallProgram = postInstallProgram;
    }

    /**
     * Get pre uninstall script path.
     *
     * @return Pre uninstall script path
     */
    public Path getPreUninstallScriptPath() {
        return preUninstallScriptPath;
    }

    /**
     * Set pre uninstall script path.
     *
     * @param preUninstallScriptPath Pre uninstall script path
     */
    public void setPreUninstallScriptPath(Path preUninstallScriptPath) {
        this.preUninstallScriptPath = preUninstallScriptPath;
    }

    /**
     * Get pre uninstall program.
     *
     * @return Pre uninstall program
     */
    public String getPreUninstallProgram() {
        return preUninstallProgram;
    }

    /**
     * Set pre uninstall program.
     *
     * @param preUninstallProgram Pre uninstall program
     */
    public void setPreUninstallProgram(String preUninstallProgram) {
        this.preUninstallProgram = preUninstallProgram;
    }

    /**
     * Get post uninstall script path.
     *
     * @return Post uninstall script path
     */
    public Path getPostUninstallScriptPath() {
        return postUninstallScriptPath;
    }

    /**
     * Set post uninstall script path.
     *
     * @param postUninstallScriptPath Post uninstall script path
     */
    public void setPostUninstallScriptPath(Path postUninstallScriptPath) {
        this.postUninstallScriptPath = postUninstallScriptPath;
    }

    /**
     * Get post uninstall program.
     *
     * @return Post uninstall program
     */
    public String getPostUninstallProgram() {
        return postUninstallProgram;
    }

    /**
     * Set post uninstall program.
     *
     * @param postUninstallProgram Post uninstall program
     */
    public void setPostUninstallProgram(String postUninstallProgram) {
        this.postUninstallProgram = postUninstallProgram;
    }

    /**
     * Get post transaction script path.
     *
     * @return Post transaction script path
     */
    public Path getPostTransactionScriptPath() {
        return postTransactionScriptPath;
    }

    /**
     * Set post transaction script path.
     *
     * @param postTransactionScriptPath Post transaction script path
     */
    public void setPostTransactionScriptPath(Path postTransactionScriptPath) {
        this.postTransactionScriptPath = postTransactionScriptPath;
    }

    /**
     * Get post transaction program.
     *
     * @return Post transaction program
     */
    public String getPostTransactionProgram() {
        return postTransactionProgram;
    }

    /**
     * Set post transaction program.
     *
     * @param postTransactionProgram Post transaction program
     */
    public void setPostTransactionProgram(String postTransactionProgram) {
        this.postTransactionProgram = postTransactionProgram;
    }

    /**
     * Get triggers.
     *
     * @return Triggers
     */
    public List<RpmTrigger> getTriggers() {
        return triggers;
    }

    /**
     * Set triggers.
     *
     * @param triggers Triggers
     */
    public void setTriggers(List<RpmTrigger> triggers) {
        this.triggers = triggers;
    }

    /**
     * Get signing key.
     *
     * @return Signing key
     */
    public String getSigningKey() {
        return signingKey;
    }

    /**
     * Set signing key.
     *
     * @param signingKey Signing key
     */
    public void setSigningKey(String signingKey) {
        this.signingKey = signingKey;
    }

    /**
     * Get signing key id.
     *
     * @return Signing key id
     */
    public Long getSigningKeyId() {
        return signingKeyId;
    }

    /**
     * Set signing key id.
     *
     * @param signingKeyId Signing key id
     */
    public void setSigningKeyId(Long signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    /**
     * Get signing key pass phrase.
     *
     * @return Signing key pass phrase
     */
    public String getSigningKeyPassPhrase() {
        return signingKeyPassPhrase;
    }

    /**
     * Set signing key pass phrase.
     *
     * @param signingKeyPassPhrase Signing key pass phrase
     */
    public void setSigningKeyPassPhrase(String signingKeyPassPhrase) {
        this.signingKeyPassPhrase = signingKeyPassPhrase;
    }

    /**
     * Get list of prefixes.
     *
     * @return List of prefixes
     */
    public List<String> getPrefixes() {
        return prefixes;
    }

    /**
     * Set list of prefixes.
     *
     * @param prefixes List of Prefixes
     */
    public void setPrefixes(List<String> prefixes) {
        if (null == prefixes) {
            prefixes = new ArrayList<String>();
        }
        this.prefixes = prefixes;
    }

    /**
     * Get list of builtin directories.
     *
     * @return List of builtin directories
     */
    public List<String> getBuiltins() {
        return builtins;
    }

    /**
     * Set list of builtin directories.
     *
     * @param builtins List of Builtin Directories
     */
    public void setBuiltins(List<String> builtins) {
        if (null == builtins) {
            builtins = new ArrayList<>();
        }
        this.builtins = builtins;
    }

    /**
     * Get package rules.
     *
     * @return Package rules
     */
    public List<RpmPackageRule> getRules() {
        return rules;
    }

    /**
     * Set package rules.
     *
     * @param rules Package rules
     */
    public void setRules(List<RpmPackageRule> rules) {
        if (null != rules) {
            for (RpmPackageRule rpmPackageRule : rules) {
                rpmPackageRule.setPackage(this);
            }
        }
        this.rules = rules;
    }

    /**
     * Get the logger.
     *
     * @return logger
     */
    public Log getLog() {
        return getMojo().getLog();
    }

    /**
     * Build the package.
     *
     * @return Files included within the package
     * @throws IOException if writing the package fails
     * @throws RpmException if building the package fails
     */
    public Set<String> build() throws IOException, RpmException {
        Set<String> fileList = new HashSet<>();
        String buildDirectory = getMojo().getBuildDirectory();
        getLog().debug("Creating RPM archive");
        RpmBuilder builder = new RpmBuilder(HashAlgo.SHA256, CompressionType.GZIP);
        builder.setType(PackageType.BINARY);
        getLog().debug("Setting package information");
        builder.setPackage(getName(), getVersion(), getRelease());
        builder.setPlatform(getArchitecture(), getOperatingSystem());
        builder.setGroup(getGroup());
        builder.setLicense(getLicense());
        builder.setSummary(getSummary());
        builder.setDescription(getDescription());
        builder.setDistribution(getDistribution());
        builder.setBuildHost(getBuildHostName());
        builder.setPackager(getPackager());
        builder.setUrl(getUrl());
        builder.setPrefixes(getPrefixes());
        builder.setSourceRpm(getSourceRpm());
        for (String builtin : getBuiltins()) {
            builder.addBuiltinDirectory(builtin);
        }
        for (RpmPackageAssociation dependency : getDependencies()) {
            if (null != dependency.getName()) {
                if (dependency.isVersionRange()) {
                    if (null != dependency.getMinVersion()) {
                        builder.addDependency(dependency.getName(), Flags.GREATER | Flags.EQUAL, dependency.getMinVersion());
                    }
                    if (null != dependency.getMaxVersion()) {
                        builder.addDependency(dependency.getName(), Flags.LESS, dependency.getMaxVersion());
                    }
                } else {
                    if (null != dependency.getVersion()) {
                        builder.addDependency(dependency.getName(), Flags.EQUAL, dependency.getVersion());
                    } else {
                        builder.addDependency(dependency.getName(), 0, "");
                    }
                }
            }
        }
        for (RpmPackageAssociation obsolete : getObsoletes()) {
            if (null != obsolete.getName()) {
                if (obsolete.isVersionRange()) {
                    if (null != obsolete.getMinVersion()) {
                        builder.addObsoletes(obsolete.getName(), Flags.GREATER | Flags.EQUAL, obsolete.getMinVersion());
                    }

                    if (null != obsolete.getMaxVersion()) {
                        builder.addObsoletes(obsolete.getName(), Flags.LESS, obsolete.getMaxVersion());
                    }
                } else {
                    if (null != obsolete.getVersion()) {
                        builder.addObsoletes(obsolete.getName(), Flags.EQUAL, obsolete.getVersion());
                    } else {
                        builder.addObsoletes(obsolete.getName(), 0, "");
                    }
                }
            }
        }
        for (RpmPackageAssociation conflict : getConflicts()) {
            if (null != conflict.getName()) {
                if (conflict.isVersionRange()) {
                    if (null != conflict.getMinVersion()) {
                        builder.addConflicts(conflict.getName(), Flags.GREATER | Flags.EQUAL, conflict.getMinVersion());
                    }

                    if (null != conflict.getMaxVersion()) {
                        builder.addConflicts(conflict.getName(), Flags.LESS, conflict.getMaxVersion());
                    }
                } else {
                    if (null != conflict.getVersion()) {
                        builder.addConflicts(conflict.getName(), Flags.EQUAL, conflict.getVersion());
                    } else {
                        builder.addConflicts(conflict.getName(), 0, "");
                    }
                }
            }
        }
        for (RpmLink link : getLinks()) {
            builder.addLink(link.getPath(), link.getTarget(),
                    link.getPermissionsOrDefault(),
                    link.getOwnerOrDefault(),
                    link.getGroupOrDefault());
        }
        getLog().debug("Setting trigger scripts");
        RpmScriptTemplateRenderer scriptTemplateRenderer = getMojo().getTemplateRenderer();
        Path scriptPath = Paths.get(buildDirectory, getName() + "-" + getProjectVersion());
        Path scriptTemplate;
        scriptTemplate = getPreTransactionScriptPath();
        if (null != scriptTemplate) {
            Path scriptFile = Paths.get(String.format("%s-pretrans-hook", scriptPath.toString()));
            scriptTemplateRenderer.render(scriptTemplate, scriptFile);
            builder.setPreTrans(scriptFile);
            builder.setPreTransProgram(getPreTransactionProgram());
        }
        scriptTemplate = getPreInstallScriptPath();
        if (null != scriptTemplate) {
            Path scriptFile = Paths.get(String.format("%s-preinstall-hook", scriptPath));
            scriptTemplateRenderer.render(scriptTemplate, scriptFile);
            builder.setPreInstall(scriptFile);
            builder.setPreInstallProgram(getPreInstallProgram());
        }
        scriptTemplate = getPostInstallScriptPath();
        if (null != scriptTemplate) {
            Path scriptFile = Paths.get(String.format("%s-postinstall-hook", scriptPath));
            scriptTemplateRenderer.render(scriptTemplate, scriptFile);
            builder.setPostInstall(scriptFile);
            builder.setPostInstallProgram(getPostInstallProgram());
        }
        scriptTemplate = getPreUninstallScriptPath();
        if (null != scriptTemplate) {
            Path scriptFile = Paths.get(String.format("%s-preuninstall-hook", scriptPath));
            scriptTemplateRenderer.render(scriptTemplate, scriptFile);
            builder.setPreUninstall(scriptFile);
            builder.setPreUninstallProgram(getPreUninstallProgram());
        }
        scriptTemplate = getPostUninstallScriptPath();
        if (null != scriptTemplate) {
            Path scriptFile = Paths.get(String.format("%s-postuninstall-hook", scriptPath));
            scriptTemplateRenderer.render(scriptTemplate, scriptFile);
            builder.setPostUninstall(scriptFile);
            builder.setPostUninstallProgram(getPostUninstallProgram());
        }
        scriptTemplate = getPostTransactionScriptPath();
        if (null != scriptTemplate) {
            Path scriptFile = Paths.get(String.format("%s-posttrans-hook", scriptPath));
            scriptTemplateRenderer.render(scriptTemplate, scriptFile);
            builder.setPostTrans(scriptFile);
            builder.setPostTransProgram(getPostTransactionProgram());
        }
        for (RpmTrigger trigger : getTriggers()) {
            Map<String, Trigger.IntString> depends = new HashMap<>();
            for (RpmPackageAssociation dependency : trigger.getDependencies()) {
                int flags = 0;
                String version = "";
                if (null != dependency.getVersion()) {
                    version = dependency.getVersion();
                } else if (null != dependency.getMinVersion()) {
                    flags = Flags.GREATER | Flags.EQUAL;
                    version = dependency.getMinVersion();
                } else if (null != dependency.getMaxVersion()) {
                    flags = Flags.LESS;
                    version = dependency.getMaxVersion();
                }
                depends.put(dependency.getName(), new Trigger.IntString(flags, version));
            }
            scriptTemplate = trigger.getPreInstallScriptPath();
            if (null != scriptTemplate) {
                Path scriptFile = Paths.get(String.format("%s-preinstall-trigger", scriptPath));
                scriptTemplateRenderer.render(scriptTemplate, scriptFile);
                builder.addTrigger(scriptFile, trigger.getPreInstallProgram(), depends, Flags.SCRIPT_TRIGGERPREIN);
            }
            scriptTemplate = trigger.getPostInstallScriptPath();
            if (null != scriptTemplate) {
                Path scriptFile = Paths.get(String.format("%s-postinstall-trigger", scriptPath));
                scriptTemplateRenderer.render(scriptTemplate, scriptFile);
                builder.addTrigger(scriptFile, trigger.getPostInstallProgram(), depends, Flags.SCRIPT_TRIGGERIN);
            }
            scriptTemplate = trigger.getPreUninstallScriptPath();
            if (null != scriptTemplate) {
                Path scriptFile = Paths.get(String.format("%s-preuninstall-trigger", scriptPath));
                scriptTemplateRenderer.render(scriptTemplate, scriptFile);
                builder.addTrigger(scriptFile, trigger.getPreUninstallProgram(), depends, Flags.SCRIPT_TRIGGERUN);
            }
            scriptTemplate = trigger.getPostUninstallScriptPath();
            if (null != scriptTemplate) {
                Path scriptFile = Paths.get(String.format("%s-postuninstall-trigger", scriptPath));
                scriptTemplateRenderer.render(scriptTemplate, scriptFile);
                builder.addTrigger(scriptFile, trigger.getPostUninstallProgram(), depends, Flags.SCRIPT_TRIGGERPOSTUN);
            }
        }
        String keyFileName = getSigningKey();
        if (keyFileName != null) {
            Path keyFile = Paths.get(keyFileName);
            if (!Files.exists(keyFile)) {
                throw new SigningKeyNotFoundException(keyFileName);
            }
            String keyPassPhrase = getSigningKeyPassPhrase();
            if (null != keyPassPhrase && !keyPassPhrase.equals("")) {
                builder.setPrivateKeyPassphrase(keyPassPhrase);
            }
            builder.setPrivateKeyId(getSigningKeyId());
            try (InputStream inputStream = getClass().getResourceAsStream(keyFileName)) {
                builder.setPrivateKeyRing(inputStream);
            }
        }
        getLog().debug("Adding files matched from each rule");
        for (RpmPackageRule packageRule : getRules()) {
            Collections.addAll(fileList, packageRule.addFiles(builder));
        }
        Path rpmFileName = Paths.get(buildDirectory, getFinalName());
        getLog().info(String.format("Generating RPM file %s", rpmFileName));
        try (FileChannel fileChannel = FileChannel.open(rpmFileName,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            builder.build(fileChannel);
        }
        RpmMojo mojo = getMojo();
        if (mojo.getProjectPackagingType().equals("rpm")
                && mojo.getProjectArtifactId().equals(getName())
                && mojo.getProjectVersion().equals(getProjectVersion())) {
            getLog().info(String.format("Attaching %s as primary artifact", rpmFileName.toString()));
            mojo.setPrimaryArtifact(rpmFileName, getClassifier());
        } else if (isAttach()) {
            getLog().info(String.format("Attaching %s as secondary artifact", rpmFileName.toString()));
            mojo.addSecondaryArtifact(rpmFileName, getName(), getProjectVersion(), getClassifier());
        }
        return fileList;
    }

    /**
     * List files matched for the package.
     *
     * @return the file names included within the package
     * @throws RpmException if method fails
     */
    public Set<String> listFiles() throws RpmException {
        int counter = 1;
        Set<String> fileList = new HashSet<>();
        getMojo().getLog().info(String.format("    Package: %s", getFinalName()));
        for (RpmPackageRule packageRule : getRules()) {
            getMojo().getLog().info(String.format("        \\ Rule: %d", counter++));
            String[] packageRuleFileList = packageRule.listFiles();
            String scanPath = packageRule.getScanPath();
            for (String packageRulefileName : packageRuleFileList) {
                getMojo().getLog().info(String.format("            - %s/%s", scanPath, packageRulefileName));
            }
            Collections.addAll(fileList, packageRuleFileList);
        }
        getMojo().getLog().info("");
        return fileList;
    }
}
