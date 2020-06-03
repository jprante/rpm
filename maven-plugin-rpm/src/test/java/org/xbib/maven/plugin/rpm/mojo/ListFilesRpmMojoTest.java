package org.xbib.maven.plugin.rpm.mojo;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbib.maven.plugin.rpm.RpmPackage;
import org.xbib.maven.plugin.rpm.RpmPackageRule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ListFilesRpmMojoTest {

    private String testOutputPath;
    private ListFilesRpmMojo mojo;
    private RpmPackageRule packageRule;

    @BeforeEach
    public void setUp() {
        this.testOutputPath = System.getProperty("project.build.testOutputDirectory");
        Build projectBuild = new Build();
        projectBuild.setDirectory(this.testOutputPath);
        MavenProject project = new MavenProject();
        project.setGroupId("uk.co.codezen");
        project.setArtifactId("listfilesmojo-artifact");
        project.setName("test");
        project.setVersion("1.0-SNAPSHOT");
        project.setUrl("http://www.example.com");
        project.setBuild(projectBuild);
        this.mojo = new ListFilesRpmMojo();
        this.mojo.setProject(project);
        List<RpmPackageRule> packageRules = new ArrayList<>();
        this.packageRule = new RpmPackageRule();
        packageRules.add(this.packageRule);
        RpmPackage rpmPackage = new RpmPackage();
        rpmPackage.setRules(packageRules);
        List<RpmPackage> packages = new ArrayList<>();
        packages.add(rpmPackage);
        this.mojo.setPackages(packages);
        this.mojo.setBuildPath(String.format("%s%sfiles", this.testOutputPath, File.separator));
    }

    @Test
    public void packageRpm() throws MojoExecutionException {
        List<String> includes = new ArrayList<>();
        includes.add("**");
        packageRule.setIncludes(includes);
        this.mojo.execute();
    }

    @Test
    public void packageRpmMissedFiles() throws MojoExecutionException {
        List<String> includes = new ArrayList<>();
        packageRule.setIncludes(includes);
        this.mojo.execute();
    }
}
