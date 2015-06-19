package org.janelia.workstation.microjacsapi;

import static org.janelia.workstation.microjacsapi.utils.ResourceHelper.notFoundIfNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;

@Path("/domain/{collectionName:\\w+}")
@Produces(MediaType.APPLICATION_JSON)
public class DomainObjectResource {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectResource.class);
    
    private MongoManaged mm;
    private ObjectMapper mapper;
    
    public DomainObjectResource(MongoManaged mm, ObjectMapper mapper) {
        this.mm = mm;
        this.mapper = mapper;
    }

    @GET
    @Path("/{id:\\d+}")
    public DomainObject getById(@PathParam("collectionName") final String collectionName, @PathParam("id") final Long id) {

        final Jongo jongo = mm.getJongo();
        final Class<? extends DomainObject> objClass = MongoUtils.getObjectClass(collectionName);
        DomainObject obj = jongo.getCollection(collectionName).findOne("{_id:#}",id).as(objClass);
        notFoundIfNull(obj);
        return obj;
    }

    @GET
    @Path("/all")
    public Response getAll(@PathParam("collectionName") final String collectionName) {
        return streamObjects(collectionName, "", null);
    }

    @POST
    @Path("/search")
    public Response search(@PathParam("collectionName") final String collectionName, String data) {
        
        JsonNode rootNode = null;
        try {
             rootNode = mapper.readValue(data, JsonNode.class);
        }
        catch (Exception e) {
            log.error("Error parsing search parameters:\n"+data,e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        List<Object> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder("{");
        
        Iterator<Entry<String, JsonNode>> nodeIterator = rootNode.fields();
        int i = 0;
        while (nodeIterator.hasNext()) {
           Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodeIterator.next();
           String key = entry.getKey();
           
           if (i++>0) sb.append(",");
           sb.append(key.equals("id")?"_id":key);
           
           JsonNode value = entry.getValue();
           if (value.isArray()) {
               List<Object> valueList = new ArrayList<>();
               ArrayNode arrayNode = (ArrayNode)value;
               Iterator<JsonNode> arrayIterator = arrayNode.elements();
               while (arrayIterator.hasNext()) {
                   JsonNode element = arrayIterator.next();
                   if (element instanceof ValueNode) {
                       valueList.add(getValue(element));
                   }
                   else {
                       return Response.status(Status.BAD_REQUEST).build();
                   }
               }
               sb.append(":{$in:#}");
               values.add(valueList);
           }
           else if (value instanceof ValueNode) {
               sb.append(":#");
               values.add(getValue(value));
           }
        }
        
        sb.append("}");
        final String query = sb.toString();
        
        log.info("query:{}, values:{}",query,values);
        
        return streamObjects(collectionName, query, values.toArray());
    }
    
    private Response streamObjects(final String collectionName, final String query, final Object[] parameters) {
        final Jongo jongo = mm.getJongo();
        final Class<? extends DomainObject> objClass = MongoUtils.getObjectClass(collectionName);
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JsonGenerator jg = mapper.getFactory().createGenerator(os,JsonEncoding.UTF8);
                jg.writeStartArray();
                MongoCollection collection = jongo.getCollection(collectionName);
                Find find = parameters==null ? collection.find(query) : collection.find(query, parameters);
                MongoCursor<? extends DomainObject> cursor = find.as(objClass);
                for (DomainObject domainObject : cursor) {
                    jg.writeObject(domainObject);
                }
                jg.writeEndArray();
                jg.flush();
            }
        };
        return Response.ok(stream).build();
    }
    
    private Object getValue(JsonNode jsonNode) {
        // This shouldn't be so difficult. I just want the Java object that was parsed from the JSON.
        if (jsonNode.isInt()) {
            return jsonNode.asInt();
        }
        else if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        else if (jsonNode.isTextual()) {
            String s = jsonNode.asText();
            // TOOD: this is a hack needed because we represent Longs as text. We should have a unambiguous marshaling for Longs. 
            if (s.matches("^\\d+$")) {
                return new Long(s);
            }
            return s;
        }
        else if (jsonNode.isDouble() || jsonNode.isFloat()) {
            return jsonNode.asDouble();
        }
        else {
            throw new IllegalStateException("Cannot parse iuser input "+jsonNode);
        }
    }

    
}