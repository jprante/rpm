package org.xbib.maven.plugin.rpm;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.xbib.maven.plugin.rpm.mojo.PackageRpmMojo;

import java.io.File;

/**
 *
 */
public class RpmLinkTest extends RpmBaseObjectTest {

    private RpmLink rpmLink;

    @Before
    public void setUp() throws Exception {
        String testOutputPath = System.getProperty("project.build.testOutputDirectory");
        PackageRpmMojo mojo = new PackageRpmMojo();
        mojo.setDefaultFileMode(0644);
        mojo.setDefaultOwner("root");
        mojo.setDefaultGroup("root");
        mojo.setDefaultDestination(String.format("%svar%swww%stest", File.separator, File.separator, File.separator));
        mojo.setBuildPath(testOutputPath);
        MavenProject mavenProject = new MavenProject();
        mojo.setProject(mavenProject);
        Log log = new DefaultLog(new ConsoleLogger());
        mojo.setLog(log);
        RpmPackage rpmPackage = new RpmPackage();
        rpmPackage.setMojo(mojo);
        rpmLink = new RpmLink();
        rpmLink.setPackage(rpmPackage);
    }

    @Override
    public RpmBaseObject getRpmBaseObject() {
        return rpmLink;
    }
}
