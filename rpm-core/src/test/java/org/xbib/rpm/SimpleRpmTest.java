package org.xbib.rpm;

import org.junit.jupiter.api.Test;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;

import java.nio.file.Paths;

/**
 *
 */
public class SimpleRpmTest {

    @Test
    public void testRpmBuild() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("test", "0.0.1", "1");
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setSummary("Test RPM");
        rpmBuilder.setDescription("A test RPM with a few packaged files.");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("MIT");
        rpmBuilder.setGroup("Miscellaneous");
        rpmBuilder.setDistribution("MyDistribution");
        rpmBuilder.setVendor("My vendor repository http://example.org/repo");
        rpmBuilder.setPackager("Jane Doe");
        rpmBuilder.setUrl("http://example.org");
        rpmBuilder.setProvides("test");
        rpmBuilder.build(Paths.get("build"));
    }
}
