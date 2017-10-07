package org.xbib.maven.plugin.rpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
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

    @Before
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
        mojo.setBuildPath(String.format("%s%sbuild", testOutputPath, File.separator));
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
        assertEquals(null, rpmPackage.getUrl());
        rpmPackage.setUrl("http://www.example.com/foo");
        assertEquals("http://www.example.com/foo", rpmPackage.getUrl());
    }

    @Test
    public void groupAccessors() {
        assertEquals(null, rpmPackage.getGroup());
        rpmPackage.setGroup("group/subgroup");
        assertEquals("group/subgroup", rpmPackage.getGroup());
    }

    @Test
    public void licenseAccessors() {
        assertEquals(null, rpmPackage.getLicense());
        rpmPackage.setLicense("license");
        assertEquals("license", rpmPackage.getLicense());
    }

    @Test
    public void summaryAccessors() {
        assertEquals(null, rpmPackage.getSummary());
        rpmPackage.setSummary("summary");
        assertEquals("summary", rpmPackage.getSummary());
    }

    @Test
    public void descriptionAccessors() {
        assertEquals(null, rpmPackage.getDescription());
        rpmPackage.setDescription("description");
        assertEquals("description", rpmPackage.getDescription());
    }

    @Test
    public void distributionAccessors() {
        assertEquals(null, rpmPackage.getDistribution());
        rpmPackage.setDistribution("distribution");
        assertEquals("distribution", rpmPackage.getDistribution());
    }

    @Test
    public void architectureAccessors() throws UnknownArchitectureException {
        assertEquals(Architecture.NOARCH, rpmPackage.getArchitecture());
        rpmPackage.setArchitecture("SPARC");
        assertEquals(Architecture.SPARC, rpmPackage.getArchitecture());
    }

    @Test(expected = UnknownArchitectureException.class)
    public void architectureInvalidException() throws UnknownArchitectureException {
        rpmPackage.setArchitecture("NONEXISTENT");
    }

    @Test(expected = UnknownArchitectureException.class)
    public void architectureBlankException() throws UnknownArchitectureException {
        rpmPackage.setArchitecture("");
    }

    @Test(expected = UnknownArchitectureException.class)
    public void architectureNullException() throws UnknownArchitectureException {
        rpmPackage.setArchitecture(null);
    }

    @Test
    public void operatingSystemAccessors() throws UnknownOperatingSystemException {
        assertEquals(Os.LINUX, rpmPackage.getOperatingSystem());
        rpmPackage.setOperatingSystem("LINUX390");
        assertEquals(Os.LINUX390, rpmPackage.getOperatingSystem());
    }

    @Test(expected = UnknownOperatingSystemException.class)
    public void operatingSystemInvalidException() throws UnknownOperatingSystemException {
        rpmPackage.setOperatingSystem("NONEXISTENT");
    }

    @Test(expected = UnknownOperatingSystemException.class)
    public void operatingSystemBlankException() throws UnknownOperatingSystemException {
        rpmPackage.setOperatingSystem("");
    }

    @Test(expected = UnknownOperatingSystemException.class)
    public void operatingSystemNullException() throws UnknownOperatingSystemException {
        rpmPackage.setOperatingSystem(null);
    }

    @Test
    public void buildHostNameAccessors() throws Exception {
        assertNotNull(rpmPackage.getBuildHostName());
        rpmPackage.setBuildHostName("buildhost");
        assertEquals("buildhost", rpmPackage.getBuildHostName());
    }

    @Test
    public void packagerAccessors() {
        assertEquals(null, rpmPackage.getPackager());
        rpmPackage.setPackager("packager");
        assertEquals("packager", rpmPackage.getPackager());
    }

    @Test
    public void attachAccessors() {
        assertEquals(true, rpmPackage.isAttach());
        rpmPackage.setAttach(false);
        assertEquals(false, rpmPackage.isAttach());
    }

    @Test
    public void classifierAccessors() {
        assertEquals(null, rpmPackage.getClassifier());
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

        // pre transaction
        assertEquals(null, rpmPackage.getPreTransactionScriptPath());
        rpmPackage.setPreTransactionScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPreTransactionScriptPath());

        assertEquals(null, rpmPackage.getPreTransactionProgram());
        rpmPackage.setPreTransactionProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPreTransactionProgram());


        // pre install
        assertEquals(null, rpmPackage.getPreInstallScriptPath());
        rpmPackage.setPreInstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPreInstallScriptPath());

        assertEquals(null, rpmPackage.getPreInstallProgram());
        rpmPackage.setPreInstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPreInstallProgram());
        assertEquals(null, rpmPackage.getPostInstallScriptPath());
        rpmPackage.setPostInstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPostInstallScriptPath());
        assertEquals(null, rpmPackage.getPostInstallProgram());
        rpmPackage.setPostInstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPostInstallProgram());
        assertEquals(null, rpmPackage.getPreUninstallScriptPath());
        rpmPackage.setPreUninstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPreUninstallScriptPath());
        assertEquals(null, rpmPackage.getPreUninstallProgram());
        rpmPackage.setPreUninstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPreUninstallProgram());
        assertEquals(null, rpmPackage.getPostUninstallScriptPath());
        rpmPackage.setPostUninstallScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPostUninstallScriptPath());
        assertEquals(null, rpmPackage.getPostUninstallProgram());
        rpmPackage.setPostUninstallProgram("/bin/sh");
        assertEquals("/bin/sh", rpmPackage.getPostUninstallProgram());
        assertEquals(null, rpmPackage.getPostTransactionScriptPath());
        rpmPackage.setPostTransactionScriptPath(scriptFile);
        assertEquals(scriptFile, rpmPackage.getPostTransactionScriptPath());
        assertEquals(null, rpmPackage.getPostTransactionProgram());
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
        assertEquals(null, rpmPackage.getSigningKey());
        rpmPackage.setSigningKey("key");
        assertEquals("key", rpmPackage.getSigningKey());
        assertEquals(null, rpmPackage.getSigningKeyId());
        rpmPackage.setSigningKeyId(0L);
        assertEquals(new Long(0L), rpmPackage.getSigningKeyId());
        assertEquals(null, rpmPackage.getSigningKeyPassPhrase());
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
        assertEquals(true, new File(rpmFileName).exists());
    }

    @Test
    public void buildSecondaryAttachmentNameDifference() throws IOException, RpmException {
        rpmPackage.setName("buildSecondaryAttachment");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertEquals(true, new File(rpmFileName).exists());
    }

    @Test
    public void buildSecondaryAttachmentVersionDifference() throws IOException, RpmException {
        rpmPackage.setVersion("2.0");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertEquals(true, new File(rpmFileName).exists());
    }

    @Test
    public void buildSecondaryAttachmentNameAndVersionDifference() throws IOException, RpmException {
        rpmPackage.setName("buildSecondaryAttachmentNameAndVersionDifference");
        rpmPackage.setVersion("2.0");
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertEquals(true, new File(rpmFileName).exists());
    }

    @Test
    public void buildWithoutAttachment() throws IOException, RpmException {
        project.setArtifactId("buildWithoutAttachment");
        rpmPackage.setAttach(false);
        rpmPackage.build();
        String rpmFileName = String.format("%s%s%s", testOutputPath, File.separator, rpmPackage.getFinalName());
        assertEquals(true, new File(rpmFileName).exists());
    }
}
