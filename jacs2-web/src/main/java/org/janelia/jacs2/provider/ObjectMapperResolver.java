package org.janelia.jacs2.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperResolver extends JacksonJaxbJsonProvider {

    @Inject
    public ObjectMapperResolver(ObjectMapper mapper) {
        setMapper(mapper);
    }

}
