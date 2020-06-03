package org.xbib.maven.plugin.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbib.maven.plugin.rpm.mojo.PackageRpmMojo;
import org.xbib.rpm.exception.InvalidDirectiveException;
import org.xbib.rpm.exception.InvalidPathException;
import org.xbib.rpm.exception.PathOutsideBuildPathException;
import org.xbib.rpm.exception.RpmException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RpmPackageRuleTest extends RpmBaseObjectTest {

    private String testOutputPath;

    private Log log;

    private RpmPackageRule rpmFileRule;

    private RpmPackage rpmPackage;

    @BeforeEach
    public void setUp() {
        testOutputPath = System.getProperty("project.build.testOutputDirectory");
        PackageRpmMojo mojo = new PackageRpmMojo();
        mojo.setDefaultFileMode(0644);
        mojo.setDefaultOwner("root");
        mojo.setDefaultGroup("root");
        mojo.setDefaultDestination(String.format("%svar%swww%stest", File.separator, File.separator, File.separator));
        mojo.setBuildPath(testOutputPath);
        MavenProject mavenProject = new MavenProject();
        mojo.setProject(mavenProject);
        log = new DefaultLog(new ConsoleLogger());
        mojo.setLog(log);
        rpmPackage = new RpmPackage();
        rpmPackage.setMojo(mojo);
        rpmFileRule = new RpmPackageRule();
        rpmFileRule.setPackage(rpmPackage);
        rpmFileRule.setBase("files");
    }

    @Override
    public RpmBaseObject getRpmBaseObject() {
        return rpmFileRule;
    }

    @Test
    public void directiveAccessors() throws InvalidDirectiveException {
        List<String> directives = new ArrayList<>();
        directives.add("noreplace");
        rpmFileRule.setDirectives(directives);
        assertNotNull(rpmFileRule.getDirectives());
    }

    @Test
    public void packageAccessors() {
        assertEquals(rpmPackage, rpmFileRule.getPackage());
    }

    @Test
    public void baseAccessors() {
        rpmFileRule.setBase("");
        assertEquals(File.separator, rpmFileRule.getBase());
        rpmFileRule.setBase(null);
        assertEquals(File.separator, rpmFileRule.getBase());
        rpmFileRule.setBase(String.format("%sfoo", File.separator));
        assertEquals(String.format("%sfoo", File.separator), rpmFileRule.getBase());
    }

    @Test
    public void destinationAccessors() {
        rpmFileRule.setDestination("");
        assertNull(rpmFileRule.getDestination());
        rpmFileRule.setDestination(null);
        assertNull(rpmFileRule.getDestination());
        assertEquals(String.format("%svar%swww%stest", File.separator, File.separator, File.separator),
                rpmFileRule.getDestinationOrDefault());
        rpmFileRule.setDestination(String.format("%sfoo", File.separator));
        assertEquals(String.format("%sfoo", File.separator), rpmFileRule.getDestination());
        assertEquals(String.format("%sfoo", File.separator), rpmFileRule.getDestinationOrDefault());
    }

    @Test
    public void includeAccessors() {
        List<String> includes = new ArrayList<>();
        rpmFileRule.setIncludes(includes);
        assertEquals(includes, rpmFileRule.getIncludes());
    }

    @Test
    public void excludeAccessors() {
        List<String> excludes = new ArrayList<>();
        rpmFileRule.setExcludes(excludes);
        assertEquals(excludes, rpmFileRule.getExcludes());
    }

    @Test
    public void logAccessor() {
        assertEquals(log, rpmFileRule.getLog());
    }

    @Test
    public void scanPathAccessor() throws InvalidPathException {
        String scanPath = String.format("%s%sfiles", new File(testOutputPath).getAbsolutePath(), File.separator);
        assertEquals(scanPath, rpmFileRule.getScanPath());
    }

    @Test
    public void testListFiles() throws RpmException {
        List<String> includes = new ArrayList<>();
        includes.add("**");
        List<String> excludes = new ArrayList<>();
        excludes.add("composer.*");
        rpmFileRule.setIncludes(includes);
        rpmFileRule.setExcludes(excludes);
        String[] files = rpmFileRule.listFiles();
        assertEquals(62, files.length);
    }

    @Test
    public void testListFilesOutsideBuildPath() throws RpmException {
        Assertions.assertThrows(PathOutsideBuildPathException.class, () -> {
            rpmFileRule.setBase(String.format("..%s", File.separator));
            rpmFileRule.listFiles();
        });
    }

    @Test
    public void testAddFiles() throws IOException, RpmException {
        MockBuilder builder = new MockBuilder();
        List<String> includes = new ArrayList<>();
        includes.add("**");
        List<String> excludes = new ArrayList<>();
        excludes.add("composer.*");
        rpmFileRule.setIncludes(includes);
        rpmFileRule.setExcludes(excludes);
        String[] files = rpmFileRule.addFiles(builder);
        assertEquals(62, files.length);
        assertEquals(92, builder.getContents().size());
    }
}
