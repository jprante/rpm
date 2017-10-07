package org.xbib.maven.plugin.rpm.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DirectoryScanner;
import org.xbib.maven.plugin.rpm.RpmPackage;
import org.xbib.maven.plugin.rpm.RpmScriptTemplateRenderer;
import org.xbib.rpm.exception.InvalidPathException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public abstract class AbstractRpmMojo extends AbstractMojo implements RpmMojo {
    /**
     * Set of master files (all files in build path).
     */
    protected Set<String> masterFiles = new HashSet<>();

    /**
     * Event hook template renderer.
     */
    private RpmScriptTemplateRenderer templateRenderer = null;

    /**
     * Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project = null;

    /**
     * Build path.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private String buildPath = null;

    /**
     * RPM package declarations from configuration.
     */
    @Parameter
    protected List<RpmPackage> packages = new ArrayList<>();

    /**
     * Default mode.
     */
    @Parameter
    private int defaultFileMode = 0644;

    /**
     * Default owner.
     */
    @Parameter
    private String defaultOwner = "root";

    /**
     * Default group.
     */
    @Parameter
    private String defaultGroup = "root";

    /**
     * Default installation destination.
     */
    @Parameter
    private String defaultDestination = File.separator;

    /**
     * List of file exclude patterns.
     */
    @Parameter
    protected List<String> excludes = new ArrayList<>();

    /**
     * Perform checking for extra files not included within any packages,
     * or excluded from all packages.
     */
    @Parameter
    private boolean performCheckingForExtraFiles = true;

    /**
     * Get Maven project.
     *
     * @return Maven project
     */
    protected MavenProject getProject() {
        return project;
    }

    /**
     * Set Maven project.
     *
     * @param project Maven project
     */
    public void setProject(MavenProject project) {
        this.project = project;
    }

    /**
     * Get event hook template renderer.
     *
     * @return Event hook template renderer
     */
    @Override
    public RpmScriptTemplateRenderer getTemplateRenderer() {
        if (templateRenderer == null) {
            templateRenderer = new RpmScriptTemplateRenderer();
            templateRenderer.addParameter("project", getProject());
            templateRenderer.addParameter("env", System.getenv());
            Properties systemProperties = System.getProperties();
            for (String propertyName : systemProperties.stringPropertyNames()) {
                templateRenderer.addParameter(propertyName, systemProperties.getProperty(propertyName));
            }
            Properties projectProperties = getProject().getProperties();
            for (String propertyName : projectProperties.stringPropertyNames()) {
                templateRenderer.addParameter(propertyName, projectProperties.getProperty(propertyName));
            }
        }
        return templateRenderer;
    }

    /**
     * Get the project artifact id.
     *
     * @return Artifact id
     */
    @Override
    public String getProjectArtifactId() {
        return getProject().getArtifactId();
    }

    /**
     * Get the project version.
     *
     * @return Project version
     */
    @Override
    public String getProjectVersion() {
        return getProject().getVersion();
    }

    /**
     * Get the project url.
     *
     * @return Project url
     */
    @Override
    public String getProjectUrl() {
        return getProject().getUrl();
    }

    /**
     * Get project packaging type.
     *
     * @return Packaging type
     */
    @Override
    public String getProjectPackagingType() {
        return getProject().getPackaging();
    }

    /**
     * Get collapsed project licensing.
     *
     * @return Project licenses, collapsed in to a single line, separated by commas.
     */
    @Override
    public String getCollapsedProjectLicense() {
        StringBuilder collapsedLicenseList = new StringBuilder();
        for (License license : getProject().getLicenses()) {
            if (collapsedLicenseList.toString().equals("")) {
                collapsedLicenseList = new StringBuilder(license.getName());
            } else {
                collapsedLicenseList.append(", ").append(license.getName());
            }
        }
        return (collapsedLicenseList.length() > 0 ? collapsedLicenseList.toString() : null);
    }

    /**
     * Get build output directory.
     *
     * @return Build output directory
     */
    @Override
    public String getBuildDirectory() {
        return project.getBuild().getDirectory();
    }

    /**
     * Set the primary artifact.
     *
     * @param artifactFile Primary artifact
     * @param classifier   Artifact classifier
     */
    @Override
    public void setPrimaryArtifact(Path artifactFile, String classifier) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler();
        handler.setExtension("rpm");
        Artifact artifact = new DefaultArtifact(getProject().getGroupId(),
                getProject().getArtifactId(),
                getProject().getVersion(),
                null,
                "rpm",
                classifier,
                handler
        );
        artifact.setFile(artifactFile.toFile());
        getProject().setArtifact(artifact);
    }

    /**
     * Add a secondary artifact.
     *
     * @param artifactFile Secondary artifact file
     * @param artifactId   Artifact Id
     * @param version      Artifact version
     * @param classifier   Artifact classifier
     */
    @Override
    public void addSecondaryArtifact(Path artifactFile, String artifactId, String version, String classifier) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler();
        handler.setExtension("rpm");
        Artifact artifact = new DefaultArtifact(
                getProject().getGroupId(),
                artifactId,
                version,
                null,
                "rpm",
                classifier,
                handler
        );
        artifact.setFile(artifactFile.toFile());
        getProject().addAttachedArtifact(artifact);
    }

    /**
     * Get the build root path.
     *
     * @return Build root path
     */
    @Override
    public String getBuildPath() throws InvalidPathException {
        try {
            return new File(buildPath).getCanonicalPath();
        } catch (IOException ex) {
            throw new InvalidPathException(buildPath, ex);
        }
    }

    /**
     * Set the build root path.
     *
     * @param buildPath Build root path
     */
    public void setBuildPath(String buildPath) {
        this.buildPath = buildPath;
    }

    /**
     * Set the RPM packages defined by the configuration.
     *
     * @param packages List of RPM packages
     */
    public void setPackages(List<RpmPackage> packages) {
        for (RpmPackage rpmPackage : packages) {
            rpmPackage.setMojo(this);
        }
        this.packages = packages;
    }

    /**
     * Get default mode.
     *
     * @return Default mode
     */
    @Override
    public int getDefaultFileMode() {
        return defaultFileMode;
    }

    /**
     * Set default mode.
     *
     * @param defaultFileMode Default mode
     */
    public void setDefaultFileMode(int defaultFileMode) {
        this.defaultFileMode = defaultFileMode;
    }

    /**
     * Get default owner.
     *
     * @return Default owner
     */
    @Override
    public String getDefaultOwner() {
        return defaultOwner;
    }

    /**
     * Set default owner.
     *
     * @param defaultOwner Default owner
     */
    public void setDefaultOwner(String defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    /**
     * Get default group.
     *
     * @return Default group
     */
    @Override
    public String getDefaultGroup() {
        return defaultGroup;
    }

    /**
     * Set default group.
     *
     * @param defaultGroup Default group
     */
    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    /**
     * Get default destination.
     *
     * @return Default destination
     */
    @Override
    public String getDefaultDestination() {
        return defaultDestination;
    }

    /**
     * Set default destination.
     *
     * @param defaultDestination Default destination
     */
    public void setDefaultDestination(String defaultDestination) {
        this.defaultDestination = defaultDestination;
    }

    /**
     * Set the list of file exclude patterns.
     *
     * @param excludes List of file exclude patterns
     */
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * Get ignore extra files.
     *
     * @return Ignore extra files
     */
    public boolean isPerformCheckingForExtraFiles() {
        return performCheckingForExtraFiles;
    }

    /**
     * Set ignore extra files.
     *
     * @param performCheckingForExtraFiles Ignore extra files
     */
    public void setPerformCheckingForExtraFiles(boolean performCheckingForExtraFiles) {
        this.performCheckingForExtraFiles = performCheckingForExtraFiles;
    }

    /**
     * Scan the build path for all files for inclusion in an RPM archive.
     * Excludes are applied also. This is because it doesn't matter
     * if a file ends up being included within an RPM as the master list
     * is only for us to know which files have been missed by a packaging
     * rule.
     */
    protected void scanMasterFiles() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setIncludes(null);
        ds.setExcludes(excludes.toArray(new String[0]));
        ds.setBasedir(buildPath);
        ds.setFollowSymlinks(false);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] fileMatches = ds.getIncludedFiles();
        masterFiles = new HashSet<>(fileMatches.length);
        Collections.addAll(masterFiles, fileMatches);
    }
}
