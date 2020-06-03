package org.xbib.maven.plugin.rpm;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.DirectoryScanner;
import org.xbib.maven.plugin.rpm.mojo.RpmMojo;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.exception.InvalidDirectiveException;
import org.xbib.rpm.exception.InvalidPathException;
import org.xbib.rpm.exception.PathOutsideBuildPathException;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.payload.Directive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * RPM rule, represents a file to be included within the RPM.
 */
public class RpmPackageRule extends RpmBaseObject {
    /**
     * RPM package.
     */
    private RpmPackage rpmPackage = null;

    /**
     * Rule base path, relative to plugin buildPath.
     */
    private String base = File.separator;

    /**
     * Destination path of files within RPM.
     */
    private String destination = null;

    /**
     * List of file include rules.
     */
    private List<String> includes = new ArrayList<>();

    /**
     * List of file exclude rules.
     */
    private List<String> excludes = new ArrayList<>();

    /**
     * File directives.
     */
    private EnumSet<Directive> directive;

    /**
     * Get associated RPM package.
     *
     * @return RPM package
     */
    @Override
    public RpmPackage getPackage() {
        return this.rpmPackage;
    }

    /**
     * Set associated RPM package.
     *
     * @param rpmPackage RPM package
     */
    public void setPackage(RpmPackage rpmPackage) {
        this.rpmPackage = rpmPackage;
    }

    /**
     * Get base path, relative to the buildPath.
     *
     * @return Base path
     */
    public String getBase() {
        return this.base;
    }

    /**
     * Set base path, relative to the buildPath.
     *
     * @param base Base path
     */
    public void setBase(String base) {
        if (null == base || base.equals("")) {
            base = File.separator;
        }

        this.base = base;
    }

    /**
     * Get file destination.
     *
     * @return File destination
     */
    public String getDestination() {
        return this.destination;
    }

    /**
     * Set file destination.
     *
     * @param destination File destination
     */
    public void setDestination(String destination) {
        if (null != destination && destination.equals("")) {
            destination = null;
        }
        this.destination = destination;
    }

    /**
     * Get the file destination, or the default setting if not set.
     *
     * @return File destination
     */
    public String getDestinationOrDefault() {
        if (null == this.destination) {
            return this.getPackage().getMojo().getDefaultDestination();
        } else {
            return this.destination;
        }
    }

    /**
     * Get file inclusion rules.
     *
     * @return File inclusion rules
     */
    public List<String> getIncludes() {
        return this.includes;
    }

    /**
     * Set file inclusion rules.
     *
     * @param includes File inclusion rules
     */
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    /**
     * Get file exclusion rules.
     *
     * @return File exclusion rules
     */
    public List<String> getExcludes() {
        return this.excludes;
    }

    /**
     * Set file exclusion rules.
     *
     * @param excludes File exclusion rules
     */
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * Get file directives.
     *
     * @return File directives
     */
    public EnumSet<Directive> getDirectives() {
        return this.directive;
    }

    /**
     * Set file directives.
     *
     * @param directives File directives
     * @throws InvalidDirectiveException if there is an invalid RPM directive
     */
    public void setDirectives(List<String> directives) throws InvalidDirectiveException {
        this.directive = Directive.newDirective(directives);
    }

    /**
     * Get path used for scanning for files to be included by the rule.
     *
     * @return Scan path
     * @throws InvalidPathException if path is invalid
     */
    public String getScanPath() throws InvalidPathException {
        String scanPath = String.format("%s%s%s",
                this.rpmPackage.getMojo().getBuildPath(), File.separator, this.getBase());
        try {
            return Paths.get(scanPath).toRealPath().toString();
        } catch (IOException ex) {
            throw new InvalidPathException(scanPath, ex);
        }
    }

    /**
     * Get the Maven logger.
     *
     * @return Maven logger
     */
    public Log getLog() {
        return this.getPackage().getMojo().getLog();
    }

    /**
     * List all files found by the rule to the package.
     *
     * @return Matched file list
     * @throws RpmException if files can not be listed
     */
    public String[] listFiles() throws RpmException {
        RpmMojo mojo = rpmPackage.getMojo();
        String buildPath = mojo.getBuildPath();
        String scanPath = getScanPath();
        if (!String.format("%s%s", scanPath, File.separator).startsWith(String.format("%s%s", buildPath, File.separator))) {
            throw new PathOutsideBuildPathException(scanPath, buildPath);
        }
        DirectoryScanner ds = new DirectoryScanner();
        ds.setIncludes(getIncludes().toArray(new String[0]));
        ds.setExcludes(getExcludes().toArray(new String[0]));
        ds.setBasedir(scanPath);
        ds.setFollowSymlinks(false);
        ds.setCaseSensitive(true);
        getLog().debug("Scanning for files for package rule");
        ds.scan();
        return ds.getIncludedFiles();
    }

    /**
     * Add all files found by the rule to the package.
     *
     * @param builder RPM builder
     * @return Matched file list
     * @throws IOException if file could not be added
     * @throws RpmException if RPM archive could not be listed
     */
    public String[] addFiles(RpmBuilder builder) throws IOException, RpmException {
        String[] includedFiles = listFiles();
        String scanPath = getScanPath();
        getLog().debug(String.format("Adding %d files found to package.", includedFiles.length));
        for (String includedFile : includedFiles) {
            String destinationPath = this.getDestinationOrDefault() + File.separator + includedFile;
            String sourcePath = String.format("%s%s%s", scanPath, File.separator, includedFile);
            String owner = getOwnerOrDefault();
            String group = getGroupOrDefault();
            int fileMode = getPermissionsOrDefault();
            getLog().debug(String.format("Adding file: %s to path %s with owner '%s', " +
                            "group '%s', with file mode %o.",
                    sourcePath, destinationPath, owner, group, fileMode));
            builder.addFile(destinationPath, Paths.get(sourcePath), fileMode, -1,
                    getDirectives(), owner, group, true);
        }
        return includedFiles;
    }
}
