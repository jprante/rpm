package org.xbib.maven.plugin.rpm.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.maven.model.Build;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbib.maven.plugin.rpm.MockMojo;
import org.xbib.maven.plugin.rpm.RpmPackage;
import org.xbib.maven.plugin.rpm.RpmScriptTemplateRenderer;
import org.xbib.rpm.exception.InvalidPathException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class AbstractRpmMojoTest {

    private String testOutputPath;

    private MockMojo mojo;
    private MavenProject project;

    @BeforeEach
    public void setUp() {
        testOutputPath = System.getProperty("project.build.testOutputDirectory");
        Build projectBuild = new Build();
        projectBuild.setDirectory("target");
        project = new MavenProject();
        project.setGroupId("org.xbib");
        project.setArtifactId("test-artifact");
        project.setName("test");
        project.setVersion("1.0-SNAPSHOT");
        project.setUrl("http://www.example.com");
        project.setBuild(projectBuild);
        List<License> licenses = new ArrayList<>();
        License license1 = new License();
        license1.setName("GPL");
        licenses.add(license1);
        License license2 = new License();
        license2.setName("LGPL");
        licenses.add(license2);
        project.setLicenses(licenses);
        mojo = new MockMojo();
        mojo.setProject(project);
    }

    @Test
    public void projectAccessors() {
        mojo.setProject(null);
        assertNull(mojo.getProject());
        mojo.setProject(project);
        assertEquals(project, mojo.getProject());
        assertEquals("test-artifact", mojo.getProjectArtifactId());
        assertEquals("1.0-SNAPSHOT", mojo.getProjectVersion());
        assertEquals("http://www.example.com", mojo.getProjectUrl());
        assertEquals("GPL, LGPL", mojo.getCollapsedProjectLicense());
        assertEquals("target", mojo.getBuildDirectory());
    }

    @Test
    public void templateRenderer() throws IOException {
        RpmScriptTemplateRenderer renderer = mojo.getTemplateRenderer();
        assertNotNull(renderer);
        Path templateScriptFile =
                Paths.get(testOutputPath, "mojo/AbstractRpmMojo-template");
        Path expectedScriptFile =
                Paths.get(testOutputPath, "mojo/AbstractRpmMojo-template-expected");
        Path actualScriptFile =
                Paths.get(testOutputPath, "mojo/AbstractRpmMojo-template-actual");
        renderer.render(templateScriptFile, actualScriptFile);
        assertTrue(Files.exists(actualScriptFile));
        char[] buff = new char[1024];
        StringBuilder stringBuilder;
        int bytesRead;
        stringBuilder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(actualScriptFile)) {
            while (-1 != (bytesRead = reader.read(buff))) {
                stringBuilder.append(buff, 0, bytesRead);
            }
        }
        String actualTemplateContents = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(expectedScriptFile)) {
            while (-1 != (bytesRead = reader.read(buff))) {
                stringBuilder.append(buff, 0, bytesRead);
            }
        }
        String expectedTemplateContents = stringBuilder.toString();
        assertEquals(expectedTemplateContents, actualTemplateContents);
        assertEquals(renderer, mojo.getTemplateRenderer());
    }

    @Test
    public void projectArtifacts() {
        Path artifact = Paths.get(String.format("%s/artifact.rpm", testOutputPath));
        mojo.setPrimaryArtifact(artifact, "test");
        mojo.addSecondaryArtifact(artifact, "secondary-artifact", "1.0", "test");
        mojo.addSecondaryArtifact(artifact, "secondary-artifact", "1.0", null);
        assertNotNull(project.getArtifact());
        assertEquals(2, project.getAttachedArtifacts().size());
    }

    @Test
    public void buildPath() throws InvalidPathException {
        mojo.setBuildPath(testOutputPath);
        assertEquals(testOutputPath, mojo.getBuildPath());
    }

    @Test
    public void packages() {
        List<RpmPackage> packages = new ArrayList<>();
        packages.add(new RpmPackage());
        mojo.setPackages(packages);
        assertEquals(packages, mojo.getPackages());
    }

    @Test
    public void defaults() {
        assertEquals(0644, mojo.getDefaultFileMode());
        mojo.setDefaultFileMode(0755);
        assertEquals(0755, mojo.getDefaultFileMode());
        assertEquals("root", mojo.getDefaultOwner());
        mojo.setDefaultOwner("nobody");
        assertEquals("nobody", mojo.getDefaultOwner());
        assertEquals("root", mojo.getDefaultGroup());
        mojo.setDefaultGroup("nobody");
        assertEquals("nobody", mojo.getDefaultGroup());
        assertEquals(File.separator, mojo.getDefaultDestination());
        mojo.setDefaultDestination(String.format("%sdestination", File.separator));
        assertEquals(String.format("%sdestination", File.separator), mojo.getDefaultDestination());
    }

    @Test
    public void excludes() {
        List<String> excludes = new ArrayList<>();
        mojo.setExcludes(excludes);
        assertEquals(excludes, mojo.getExcludes());
    }

    @Test
    public void checkingForExtraFiles() {
        mojo.setPerformCheckingForExtraFiles(true);
        assertTrue(mojo.isPerformCheckingForExtraFiles());
        mojo.setPerformCheckingForExtraFiles(false);
        assertFalse(mojo.isPerformCheckingForExtraFiles());
    }

    @Test
    public void scanMasterFiles() {
        mojo.setBuildPath(String.format("%s%sfiles", testOutputPath, File.separator));
        mojo.scanMasterFiles();
        Set<String> masterFiles = mojo.getMasterFiles();
        assertEquals(63, masterFiles.size());
    }
}
