package org.xbib.maven.plugin.rpm;

import org.apache.maven.plugin.MojoExecutionException;
import org.xbib.maven.plugin.rpm.mojo.AbstractRpmMojo;

import java.util.List;
import java.util.Set;

/**
 *
 */
public class MockMojo extends AbstractRpmMojo {

    @Override
    public void execute() throws MojoExecutionException {
    }

    @Override
    public void scanMasterFiles() {
        super.scanMasterFiles();
    }

    /**
     * Get master file set.
     *
     * @return Master file set
     */
    public Set<String> getMasterFiles() {
        return this.masterFiles;
    }

    /**
     * Get excludes.
     *
     * @return Excludes
     */
    public List<String> getExcludes() {
        return this.excludes;
    }

    /**
     * Get packages.
     *
     * @return Packages
     */
    public List<RpmPackage> getPackages() {
        return this.packages;
    }
}
