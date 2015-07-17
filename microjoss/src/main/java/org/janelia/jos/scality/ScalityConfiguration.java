package org.janelia.jos.scality;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScalityConfiguration {

    @NotEmpty
    private String url;
    @NotEmpty
    private String driver;

    public String getUrl() {
        return url;
    }

    @JsonProperty
    public void setUrl(String url) {
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    @JsonProperty
    public void setDriver(String driver) {
        this.driver = driver;
    }
}
