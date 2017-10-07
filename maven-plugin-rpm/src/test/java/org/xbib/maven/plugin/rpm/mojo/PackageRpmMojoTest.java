package org.xbib.maven.plugin.rpm.mojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.xbib.maven.plugin.rpm.RpmPackage;
import org.xbib.maven.plugin.rpm.RpmPackageRule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PackageRpmMojoTest {

    private PackageRpmMojo mojo;

    private MavenProject project;

    private RpmPackageRule packageRule;

    @Before
    public void setUp() {
        // Test output path
        String testOutputPath = System.getProperty("project.build.testOutputDirectory");
        Build projectBuild = new Build();
        projectBuild.setDirectory(testOutputPath);
        project = new MavenProject();
        project.setGroupId("org.xbib");
        project.setArtifactId("packagerpmmojo-artifact");
        project.setName("test");
        project.setUrl("http://www.example.com");
        project.setBuild(projectBuild);
        project.setPackaging("rpm");
        mojo = new PackageRpmMojo();
        mojo.setProject(project);
        List<RpmPackageRule> packageRules = new ArrayList<>();
        packageRule = new RpmPackageRule();
        packageRules.add(packageRule);
        RpmPackage rpmPackage = new RpmPackage();
        rpmPackage.setRules(packageRules);
        List<RpmPackage> packages = new ArrayList<>();
        packages.add(rpmPackage);
        mojo.setPackages(packages);
        mojo.setBuildPath(String.format("%s%sfiles", testOutputPath, File.separator));
    }

    @Test
    public void packageRpm() throws MojoExecutionException {
        project.setVersion("1.0-SNAPSHOT");
        List<String> includes = new ArrayList<>();
        includes.add("**");
        packageRule.setIncludes(includes);
        mojo.execute();
        assertEquals(true, project.getArtifact().getFile().exists());
    }

    @Test
    public void packageRpmNonRpmPackagingType() throws MojoExecutionException {
        project.setPackaging("jar");
        project.setVersion("1.1-SNAPSHOT");
        List<String> includes = new ArrayList<>();
        includes.add("**");
        packageRule.setIncludes(includes);
        mojo.execute();
        assertNull(project.getArtifact());
    }

    @Test(expected = MojoExecutionException.class)
    public void packageRpmMissedFiles() throws MojoExecutionException {
        project.setVersion("2.0-SNAPSHOT");
        List<String> includes = new ArrayList<>();
        packageRule.setIncludes(includes);
        mojo.execute();
    }

    @Test
    public void packageRpmMissedFilesWithoutChecks() throws MojoExecutionException {
        mojo.setPerformCheckingForExtraFiles(false);
        project.setVersion("3.0-SNAPSHOT");
        List<String> includes = new ArrayList<>();
        includes.add("**/*.php");
        packageRule.setIncludes(includes);
        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void packageRpmNoFilesPackaged() throws MojoExecutionException {
        mojo.setPerformCheckingForExtraFiles(false);
        project.setVersion("4.0-SNAPSHOT");
        List<String> includes = new ArrayList<>();
        packageRule.setIncludes(includes);
        mojo.execute();
    }

    @Test
    public void packageRpmNoFilesPackagedNoPackages() throws MojoExecutionException {
        mojo.setPackages(new ArrayList<>());
        mojo.setPerformCheckingForExtraFiles(false);
        project.setVersion("5.0-SNAPSHOT");
        List<String> includes = new ArrayList<>();
        packageRule.setIncludes(includes);
        mojo.execute();
    }
}
