package org.xbib.maven.plugin.rpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class RpmScriptTemplateRendererTest {
    private String testOutputPath;

    @Before
    public void setUp() {
        this.testOutputPath = System.getProperty("project.build.testOutputDirectory");
    }

    @Test
    public void render() throws Exception {
        Path templateScriptFile = Paths.get(
                String.format("%s/rpm/RpmScriptTemplateRenderer-template",
                        this.testOutputPath));
        Path expectedScriptFile = Paths.get(
                String.format("%s/rpm/RpmScriptTemplateRenderer-template-expected",
                        this.testOutputPath));
        Path actualScriptFile = Paths.get(
                String.format("%s/rpm/RpmScriptTemplateRenderer-template-actual",
                        this.testOutputPath));
        RpmScriptTemplateRenderer renderer = new RpmScriptTemplateRenderer();
        renderer.addParameter("testdata1", true);
        renderer.addParameter("testdata2", "test");
        renderer.addParameter("testdata3", 123);
        //assertFalse(Files.exists(actualScriptFile));
        renderer.render(templateScriptFile, actualScriptFile);
        assertTrue(Files.exists(actualScriptFile));
        char[] buff = new char[1024];
        StringBuilder stringBuilder;
        int bytesRead;
        stringBuilder = new StringBuilder();
        try (Reader reader = Files.newBufferedReader(actualScriptFile)) {
            while (-1 != (bytesRead = reader.read(buff))) {
                stringBuilder.append(buff, 0, bytesRead);
            }
        }
        String actualTemplateContents = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        try (Reader reader = Files.newBufferedReader(expectedScriptFile)) {
            while (-1 != (bytesRead = reader.read(buff))) {
                stringBuilder.append(buff, 0, bytesRead);
            }
        }
        String expectedTemplateContents = stringBuilder.toString();
        assertEquals(expectedTemplateContents, actualTemplateContents);
    }
}
