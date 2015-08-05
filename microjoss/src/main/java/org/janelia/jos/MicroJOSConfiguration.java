package org.janelia.jos;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.janelia.jos.mongo.MongoConfiguration;
import org.janelia.jos.scality.ScalityConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Janelia Object Store configuration.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MicroJOSConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("mongodb")
    private MongoConfiguration mongoConfiguration;

    @Valid
    @NotNull
    @JsonProperty("scality")
    private ScalityConfiguration scalityConfiguration;
    
    @Valid
    @NotNull
    @JsonProperty("httpClient")
    private HttpClientConfiguration httpClient = new HttpClientConfiguration();

    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerConfiguration;
    
    
    public MongoConfiguration getMongoConfiguration() {
        return mongoConfiguration;
    }

    public ScalityConfiguration getScalityConfiguration() {
        return scalityConfiguration;
    }

    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }
    
    public SwaggerBundleConfiguration getSwaggerConfiguration() {
        return swaggerConfiguration;
    }
}
