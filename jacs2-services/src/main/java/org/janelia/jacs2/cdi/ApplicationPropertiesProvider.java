package org.janelia.jacs2.cdi;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public class ApplicationPropertiesProvider {

    private static final String DEFAULT_APPLICATION_CONFIG_RESOURCES = "/jacs.properties";

    private Properties applicationProperties = new Properties();

    public ApplicationPropertiesProvider fromDefaultResource() {
        return fromResource(DEFAULT_APPLICATION_CONFIG_RESOURCES);
    }

    public ApplicationPropertiesProvider fromResource(String resourceName) {
        if (StringUtils.isBlank(resourceName)) {
            return this;
        }
        try (InputStream configStream = this.getClass().getResourceAsStream(resourceName)) {
            return fromInputStream(configStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ApplicationPropertiesProvider fromEnvVar(String envVarName) {
        if (StringUtils.isBlank(envVarName)) {
            return this;
        }
        String envVarValue = System.getenv(envVarName);
        if (StringUtils.isBlank(envVarValue)) {
            return this;
        }
        return fromFile(envVarValue);
    }

    public ApplicationPropertiesProvider fromFile(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return this;
        }
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            try (InputStream fileInputStream = new FileInputStream(file)) {
                return fromInputStream(fileInputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this;
    }

    private ApplicationPropertiesProvider fromInputStream(InputStream stream) {
        try {
            applicationProperties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public Properties build() {
        return applicationProperties;
    }

}
