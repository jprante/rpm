package org.xbib.maven.plugin.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbib.maven.plugin.rpm.mojo.PackageRpmMojo;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.exception.UnknownArchitectureException;
import org.xbib.rpm.exception.UnknownOperatingSystemException;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RpmPackageTest {

    private String testOutputPath;

    private RpmPackage rpmPackage;

    private MavenProject project;

    @BeforeEach
    public void setUp() {
        testOutputPath = System.getProperty("project.build.testOutputDirectory");
        Build projectBuild = new Build();
        projectBuild.setDirectory(testOutputPath);
        rpmPackage = new RpmPackage();
        project = new MavenProject();
        project.setArtifactId("test-artifact");
        project.setName("test");
        project.setVersion("1.0");
        project.setBuild(projectBuild);
        PackageRpmMojo mojo = new PackageRpmMojo();
        mojo.setProject(project);
        mojo.setBuildPath(String.format("%s%sfiles", testOutputPath, File.separator));
        rpmPackage.setMojo(mojo);
    }

    @Test
    public void nameAccessors() {
        assertEquals("test-artifact", rpmPackage.getName());
        rpmPackage.setName("name");
        assertEquals("name", rpmPackage.getName());
    }

    @Test
    public void versionAccessors() {
        assertEquals("1.0", rpmPackage.getVersion());
        assertEquals("1.0", rpmPackage.getProjectVersion());
        rpmPackage.setVersion("2.0");
        assertEquals("2.0", rpmPackage.getVersion());
        assertEquals("2.0", rpmPackage.getProjectVersion());
        rpmPackage.setVersion("2.0-SNAPSHOT");
        assertEquals("2.0.SNAPSHOT", rpmPackage.getVersion());
        assertEquals("2.0-SNAPSHOT", rpmPackage.getProjectVersion());
        rpmPackage.setVersion(null);
        assertEquals("1.0", rpmPackage.getVersion());
        assertEquals("1.0", rpmPackage.getProjectVersion());
        rpmPackage.setVersion("");
        assertEquals("1.0", rpmPackage.getVersion());
        assertEquals("1.0", rpmPackage.getProjectVersion());
    }

    @Test
    public void releaseAccessors() {
        assertTrue(rpmPackage.getRelease().matches("\\d+"));
        rpmPackage.setRelease("release");
        assertEquals("release", rpmPackage.getRelease());
    }

    @Test
    public void finalNameAccessors() {
        rpmPackage.setName("name");
        rpmPackage.setVersion("1.0-SNAPSHOT");
        rpmPackage.setRelease("3");
        assertEquals("name-1.0.SNAPSHOT-3.noarch.rpm", rpmPackage.getFinalName());
        rpmPackage.setFinalName("finalname");
        assertEquals("finalname", rpmPackage.getFinalName());
    }

    @Test
    public void dependenciesAccessors() {
        List<RpmPackageAssociation> dependencies = new ArrayList<>();
        assertNotNull(rpmPackage.getDependencies());
        rpmPackage.setDependencies(dependencies);
        assertEquals(dependencies, rpmPackage.getDependencies());
    }

    @Test
    public void obsoletesAccessors() {
        List<RpmPackageAssociation> obsoletes = new ArrayList<>();
        assertNotNull(rpmPackage.getObsoletes());
        rpmPackage.setObsoletes(obsoletes);
        assertEquals(obsoletes, rpmPackage.getObsoletes());
    }

    @Test
    public void conflictsAccessors() {
        List<RpmPackageAssociation> conflicts = new ArrayList<>();
        assertNotNull(rpmPackage.getConflicts());
        rpmPackage.setConflicts(conflicts);
        assertEquals(conflicts, rpmPackage.getConflicts());
    }

    @Test
    public void urlAccessors() {
        assertNull(rpmPackage.getUrl());
        rpmPackage.setUrl("http://www.example.com/foo");
        assertEquals("http://www.example.com/foo", rpmPackage.getUrl());
    }

    @Test
    public void groupAccessors() {
        assertNull(rpmPackage.getGroup());
        rpmPackage.setGroup("group/subgroup");
        assertEquals("group/subgroup", rpmPackage.getGroup());
    }

    @Test
    public void licenseAccessors() {
        assertNull(rpmPackage.getLicense());
        rpmPackage.setLicense("license");
        assertEquals("license", rpmPackage.getLicense());
    }

    @Test
    public void summaryAccessors() {
        assertNull(rpmPackage.getSummary());
        rpmPackage.setSummary("summary");
        assertEquals("summary", rpmPackage.getSummary());
    }

    @Test
    public void descriptionAccessors() {
        assertNull(rpmPackage.getDescription());
        rpmPackage.setDescription("description");
        assertEquals("description", rpmPackage.getDescription());
    }

    @Test
    public void distributionAccessors() {
        assertNull(rpmPackage.getDistribution());
        rpmPackage.setDistribution("distribution");
        assertEquals("distribution", rpmPackage.getDistribution());
    }

    @Test
    public void architectureAccessors() throws UnknownArchitectureException {
        assertEquals(Architecture.NOARCH, rpmPackage.getArchitecture());
        rpmPackage.setArchitecture("SPARC");
        assertEquals(Architecture.SPARC, rpmPackage.getArchitecture());
    }

    @Test
    public void architectureInvalidException() {
        Assertions.assertThrows(UnknownArchitectureException.class, () ->
                rpmPackage.setArchitecture("NONEXISTENT"));
    }

    @Test
    public void architectureBlankException() {
        Assertions.assertThrows(UnknownArchitectureException.class, () ->
                rpmPackage.setArchitecture(""));
    }

    @Test
    public void architectureNullException() {
        Assertions.assertThrows(UnknownArchitectureException.class, () ->
                rpmPackage.setArchitecture(null));
    }

    @Test
    public void operatingSystemAccessors() throws UnknownOperatingSystemException {
        assertEquals(Os.LINUX, rpmPackage.getOperatingSystem());
        rpmPackage.setOperatingSystem("LINUX390");
        assertEquals(Os.LINUX390, rpmPackage.getOperatingSystem());
    }

    @Test
    public void operatingSystemInvalidException() {
        Assertions.assertThrows(UnknownOperatingSystemException.class, () ->
                rpmPackage.setOperatingSystem("NONEXISTENT"));
    }

    @Test
    public void operatingSystemBlankException() {
        Assertions.assertThrows(UnknownOperatingSystemException.class, () ->
                rpmPackage.setOperatingSystem(""));
    }

    @Test
    public void operatingSystemNullException() {
        Assertions.assertThrows(UnknownOperatingSystemException.class, () ->
                rpmPackage.setOperatingSystem(null));
    }

    @Test
    public void buildHostNameAccessors() throws Exception {
        assertNotNull(rpmPackage.getBuildHostName());
        rpmPackage.setBuildHostName("buildhost");
        assertEquals("buildhost", rpmPackage.getBuildHostName());
    }

    @Test
    public void packagerAccessors() {
        assertNull(rpmPackage.getPackager());
        rpmPackage.setPackager("packager");
        assertEquals("packager", rpmPackage.getPackager());
    }

    @Test
    public void attachAccessors() {
        assertTrue(rpmPackage.isAttach());
        rpmPackage.setAttach(false);
        assertFalse(rpmPackage.isAttach());
    }

    @Test
    public void classifierAccessors() {
        assertNull(rpmPackage.getClassifier());
        rpmPackage.setClassifier("classifier");
        assertEquals("classifier", rpmPackage.getClassifier());
    }

    @Test
    public void rulesAccessors() {
        List<RpmPackageRule> rules = new ArrayList<>();
        rules.add(new RpmPackageRule());
        rules.add(new RpmPackageRule());
        rpmPackage.setRules(rules);
        assertEquals(rules, rpmPackage.getRules());
        rpmPackage.setRules(null);
        assertNull(rpmPackage.getRules());
    }

    @Test
    public void eventHookAccessors() {
        Path scriptFile = Paths.get("samplescript.sh");
        assertNull(rpmPackage.getPreTransactionScriptPath());
        rpmPackage.setPreTransactionScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPreTransactionScriptPath());
        assertNull(rpmPackage.getPreTransactionProgram());
        rpmPackage.setPreTransactionProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPreTransactionProgram());
        assertNull(rpmPackage.getPreInstallScriptPath());
        rpmPackage.setPreInstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPreInstallScriptPath());
        assertNull(rpmPackage.getPreInstallProgram());
        rpmPackage.setPreInstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPreInstallProgram());
        assertNull(rpmPackage.getPostInstallScriptPath());
        rpmPackage.setPostInstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPostInstallScriptPath());
        assertNull(rpmPackage.getPostInstallProgram());
        rpmPackage.setPostInstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPostInstallProgram());
        assertNull(rpmPackage.getPreUninstallScriptPath());
        rpmPackage.setPreUninstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPreUninstallScriptPath());
        assertNull(rpmPackage.getPreUninstallProgram());
        rpmPackage.setPreUninstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPreUninstallProgram());
        assertNull(rpmPackage.getPostUninstallScriptPath());
        rpmPackage.setPostUninstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPostUninstallScriptPath());
        assertNull(rpmPackage.getPostUninstallProgram());
        rpmPackage.setPostUninstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPostUninstallProgram());
        assertNull(rpmPackage.getPostTransactionScriptPath());
        rpmPackage.setPostTransactionScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPostTransactionScriptPath());
        assertNull(rpmPackage.getPostTransactionProgram());
        rpmPackage.setPostTransactionProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPostTransactionProgram());
    }

    @Test
    public void triggerAccessors() {
        List<RpmTrigger> triggers = new ArrayList<>();
        assertNotNull(rpmPackage.getTriggers());
        rpmPackage.setTriggers(triggers);
        assertEquals(triggers, rpmPackage.getTriggers());
    }

    @Test
    public void signingKeyAccessors() {
        assertNull(rpmPackage.getSigningKey());
        rpmPackage.setSigningKey("key");
        assertEquals("key", rpmPackage.getSigningKey());
        assertNull(rpmPackage.getSigningKeyId());
        rpmPackage.setSigningKeyId(0L);
        assertEquals(Long.valueOf(0L), rpmPackage.getSigningKeyId());
        assertNull(rpmPackage.getSigningKeyPassPhrase());
        rpmPackage.setSigningKeyPassPhrase("passphrase");
        assertEquals("passphrase", rpmPackage.getSigningKeyPassPhrase());
    }

    @Test
    public void prefixesAccessors() {
        List<String> prefixes = new ArrayList<>();
        assertNotNull(rpmPackage.getPrefixes());
        rpmPackage.setPrefixes(null);
        assertNotNull(rpmPackage.getPrefixes());
        rpmPackage.setPrefixes(prefixes);
        assertEquals(prefixes, rpmPackage.getPrefixes());
    }

    @Test
    public void builtinsAccessors() {
        List<String> builtins = new ArrayList<>();
        assertNotNull(rpmPackage.getBuiltins());
        rpmPackage.setBuiltins(null);
        assertNotNull(rpmPackage.getBuiltins());
        rpmPackage.setBuiltins(builtins);
        assertEquals(builtins, rpmPackage.getBuiltins());
    }

    @Test
    public void build() throws IOException, RpmException {
        project.setArtifactId("build");
        List<RpmPackageAssociation> dependencies = new ArrayList<>();
        RpmPackageAssociation dependency = new RpmPackageAssociation();
        dependency.setName("dependency");
        dependencies.add(dependency);
        rpmPackage.setDependencies(dependencies);
        List<RpmPackageAssociation> obsoletes = new ArrayList<>();
        RpmPackageAssociation obsolete = new RpmPackageAssociation();
        obsolete.setName("obsolete");
        obsoletes.add(obsolete);
        rpmPackage.setObsoletes(obsoletes);
        List<RpmPackageAssociation> conflicts = new ArrayList<>();
        RpmPackageAssociation conflict = new RpmPackageAssociation();
        conflict.setName("conflict");
        conflicts.add(conflict);
        rpmPackage.setConflicts(conflicts);
        List<RpmPackageRule> rules = new ArrayList<>();
        RpmPackageRule rule = new RpmPackageRule();
        rules.add(rule);
        rpmPackage.setRules(rules);
        Path scriptFile = Paths.get(String.format("%s%s/rpm/RpmPackage.sh",
                testOutputPath, File.separator));
        rpmPackage.setPreTransactionScriptPath(scriptFile);
        rpmPackage.setPreTransactionProgram("/bin/sh");
        rpmPackage.setPreInstallScriptPath(scriptFile);
        rpmPackage.setPreInstallProgram("/bin/sh");
        rpmPackage.setPostInstallScriptPath(scriptFile);
        rpmPackage.setPostInstallProgram("/bin/sh");
        rpmPackage.setPreUninstallScriptPath(scriptFile);
        rpmPackage.setPreUninstallProgram("/bin/sh");
        rpmPackage.setPostUninstallScriptPath(scriptFile);
        rpmPackage.setPostUninstallProgram("/bin/sh");
        rpmPackage.setPostTransactionScriptPath(scriptFile);
        rpmPackage.setPostTransactionProgram("/bin/sh");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertTrue(new File(rpmFileName).exists());
    }

    @Test
    public void buildSecondaryAttachmentNameDifference() throws IOException, RpmException {
        rpmPackage.setName("buildSecondaryAttachment");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertTrue(new File(rpmFileName).exists());
    }

    @Test
    public void buildSecondaryAttachmentVersionDifference() throws IOException, RpmException {
        rpmPackage.setVersion("2.0");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertTrue(new File(rpmFileName).exists());
    }

    @Test
    public void buildSecondaryAttachmentNameAndVersionDifference() throws IOException, RpmException {
        rpmPackage.setName("buildSecondaryAttachmentNameAndVersionDifference");
        rpmPackage.setVersion("2.0");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertTrue(new File(rpmFileName).exists());
    }

    @Test
    public void buildWithoutAttachment() throws IOException, RpmException {
        project.setArtifactId("buildWithoutAttachment");
        rpmPackage.setAttach(false);
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertTrue(new File(rpmFileName).exists());
    }
}
