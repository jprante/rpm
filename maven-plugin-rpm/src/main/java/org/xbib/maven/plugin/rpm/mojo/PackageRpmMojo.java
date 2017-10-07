package org.xbib.maven.plugin.rpm.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.xbib.maven.plugin.rpm.RpmPackage;

import java.util.Set;

/**
 * Build an RPM using Maven, allowing for operating system agnostic RPM builds.
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageRpmMojo extends AbstractRpmMojo {

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException There was a problem running the Mojo.
     *                                Further details are available in the message and cause properties.
     */
    public void execute() throws MojoExecutionException {
        scanMasterFiles();
        long totalFilesPackaged = 0;
        for (RpmPackage rpmPackage : this.packages) {
            Set<String> includedFiles;
            try {
                includedFiles = rpmPackage.build();
            } catch (Exception ex) {
                getLog().error(String.format("Unable to build package %s", rpmPackage.getName()), ex);
                throw new MojoExecutionException(String.format("Unable to build package %s", rpmPackage.getName()), ex);
            }
            masterFiles.removeAll(includedFiles);
            totalFilesPackaged += includedFiles.size();
        }
        if (isPerformCheckingForExtraFiles() && masterFiles.size() > 0) {
            getLog().error(String.format("%d file(s) listed below were found in the build path that have not been " +
                    "included in any package or explicitly excluded. Maybe you need to exclude them?", masterFiles.size()));
            for (String missedFile : this.masterFiles) {
                getLog().error(String.format(" - %s", missedFile));
            }
            throw new MojoExecutionException(String.format("%d file(s) were found in the build path that have not been " +
                            "included or explicitly excluded. Maybe you need to exclude them?",
                    masterFiles.size()));
        }
        if (0 < packages.size() && 0 == totalFilesPackaged) {
            // No files were actually packaged. Perhaps something got missed.
            getLog().error("No files were included when packaging RPM artifacts. " +
                    "Did you specify the correct output path?");
            throw new MojoExecutionException("No files were included when packaging RPM artifacts. " +
                    "Did you specify the correct output path?");
        }
    }
}
