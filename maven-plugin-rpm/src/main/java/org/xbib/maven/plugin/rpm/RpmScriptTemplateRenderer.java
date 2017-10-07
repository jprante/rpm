package org.xbib.maven.plugin.rpm;

import org.mvel2.templates.TemplateRuntime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * RPM script template renderer.
 */
public class RpmScriptTemplateRenderer {
    /**
     * Template parameter map.
     */
    private Map<String, Object> parameterMap = new HashMap<>();

    /**
     * Add parameter to parameter map.
     *
     * @param name  Parameter Name
     * @param value Parameter value
     */
    public void addParameter(String name, Object value) {
        this.parameterMap.put(name, value);
    }

    /**
     * Render a script template file.
     *
     * @param templateFile Template file
     * @param renderedFile Rendered output file
     * @throws IOException if rendering fails
     */
    public void render(Path templateFile, Path renderedFile) throws IOException {
        char[] buffer = new char[1024];
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(templateFile)) {
            int bytesRead;
            while (-1 != (bytesRead = reader.read(buffer))) {
                stringBuilder.append(buffer, 0, bytesRead);
            }
        }
        String renderedTemplate = (String) TemplateRuntime.eval(stringBuilder.toString(), this.parameterMap);
        try (BufferedWriter writer = Files.newBufferedWriter(renderedFile)) {
            writer.write(renderedTemplate);
        }
    }
}
