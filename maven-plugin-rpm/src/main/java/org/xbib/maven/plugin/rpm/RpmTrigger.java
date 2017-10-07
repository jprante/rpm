package org.xbib.maven.plugin.rpm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * RPM trigger.
 */
public class RpmTrigger {
    /**
     * Pre install event hook script path.
     */
    private Path preInstallScriptPath = null;

    /**
     * Pre install event hook program.
     */
    private String preInstallProgram = null;

    /**
     * Post install event hook script path.
     */
    private Path postInstallScriptPath = null;

    /**
     * Post install event hook program.
     */
    private String postInstallProgram = null;

    /**
     * Pre uninstall event hook script path.
     */
    private Path preUninstallScriptPath = null;

    /**
     * Pre uninstall event hook script program.
     */
    private String preUninstallProgram = null;

    /**
     * Post uninstall event hook script path.
     */
    private Path postUninstallScriptPath = null;

    /**
     * Post uninstall event hook program.
     */
    private String postUninstallProgram = null;

    /**
     * Trigger package associations.
     */
    private List<RpmPackageAssociation> dependencies = new ArrayList<>();

    /**
     * Get pre install script path.
     *
     * @return Pre install script path
     */
    public Path getPreInstallScriptPath() {
        return this.preInstallScriptPath;
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
        return this.preInstallProgram;
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
        return this.postInstallScriptPath;
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
        return this.postInstallProgram;
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
        return this.preUninstallScriptPath;
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
        return this.preUninstallProgram;
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
        return this.postUninstallScriptPath;
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
        return this.postUninstallProgram;
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
     * Get trigger packages.
     *
     * @return Trigger packages
     */
    public List<RpmPackageAssociation> getDependencies() {
        return dependencies;
    }

    /**
     * Set trigger packages.
     *
     * @param dependencies Trigger packages
     */
    public void setDependencies(List<RpmPackageAssociation> dependencies) {
        this.dependencies = dependencies;
    }
}
