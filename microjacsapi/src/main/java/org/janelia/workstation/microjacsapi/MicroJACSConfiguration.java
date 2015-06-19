package org.janelia.workstation.microjacsapi;

import io.dropwizard.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MicroJACSConfiguration extends Configuration {

    @JsonProperty @NotEmpty
    public String mongoHost = "localhost";

    @Min(1)
    @Max(65535)
    @JsonProperty
    public int mongoPort = 27017;

    @JsonProperty @NotEmpty
    public String mongoDatabase = "default";

    @JsonProperty
    public String mongoUsername;

    @JsonProperty
    public String mongoPassword;
    
    
}
