package org.xbib.maven.plugin.rpm.mojo;

import org.apache.maven.plugin.logging.Log;
import org.xbib.maven.plugin.rpm.RpmScriptTemplateRenderer;
import org.xbib.rpm.exception.InvalidPathException;

import java.nio.file.Path;

/**
 * Plugin mojo implementation.
 */
public interface RpmMojo {
    /**
     * Get event hook template renderer.
     *
     * @return Event hook template renderer
     */
    RpmScriptTemplateRenderer getTemplateRenderer();

    /**
     * Set the primary artifact.
     *
     * @param artifactFile Primary artifact
     * @param classifier   Artifact classifier
     */
    void setPrimaryArtifact(Path artifactFile, String classifier);

    /**
     * Add a secondary artifact.
     *
     * @param artifactFile Secondary artifact file
     * @param artifactId   Artifact Id
     * @param version      Artifact version
     * @param classifier   Artifact classifier
     */
    void addSecondaryArtifact(Path artifactFile, String artifactId, String version, String classifier);

    /**
     * Get build output directory.
     *
     * @return Build output directory
     */
    String getBuildDirectory();

    /**
     * Get the project artifact id.
     *
     * @return Artifact id
     */
    String getProjectArtifactId();

    /**
     * Get the project version.
     *
     * @return Project version
     */
    String getProjectVersion();

    /**
     * Get the project url.
     *
     * @return Project url
     */
    String getProjectUrl();

    /**
     * Get project packaging type.
     *
     * @return Packaging type
     */
    String getProjectPackagingType();

    /**
     * Get collapsed project licensing.
     *
     * @return Project licenses, collapsed in to a single line, separated by commas.
     */
    String getCollapsedProjectLicense();

    /**
     * Get the build root path.
     *
     * @return Build root path
     * @throws InvalidPathException Build path is invalid and could not be retrieved
     */
    String getBuildPath() throws InvalidPathException;

    /**
     * Get default mode.
     *
     * @return Default mode
     */
    int getDefaultFileMode();

    /**
     * Get default owner.
     *
     * @return Default owner
     */
    String getDefaultOwner();

    /**
     * Get default group.
     *
     * @return Default group
     */
    String getDefaultGroup();

    /**
     * Get default destination.
     *
     * @return Default destination
     */
    String getDefaultDestination();

    /**
     * Get logger.
     *
     * @return Logger
     */
    Log getLog();
}
