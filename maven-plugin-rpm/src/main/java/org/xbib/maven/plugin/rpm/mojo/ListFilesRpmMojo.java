package org.xbib.maven.plugin.rpm.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.xbib.maven.plugin.rpm.RpmPackage;
import org.xbib.rpm.exception.RpmException;

import java.util.Set;

/**
 *
 */
@Mojo(name = "listfiles", defaultPhase = LifecyclePhase.PACKAGE)
public class ListFilesRpmMojo extends AbstractRpmMojo {

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException There was a problem running the Mojo.
     *                                Further details are available in the message and cause properties.
     */
    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Declared packages:");
        scanMasterFiles();
        for (RpmPackage rpmPackage : this.packages) {
            Set<String> includedFiles;
            try {
                includedFiles = rpmPackage.listFiles();
            } catch (RpmException e) {
                throw new MojoExecutionException(e.getMessage());
            }
            masterFiles.removeAll(includedFiles);
        }
        if (masterFiles.size() > 0) {
            getLog().info("Unmatched files:");
            for (String unmatchedFile : this.masterFiles) {
                getLog().info(String.format("    - %s", unmatchedFile));
            }
        }
    }
}
