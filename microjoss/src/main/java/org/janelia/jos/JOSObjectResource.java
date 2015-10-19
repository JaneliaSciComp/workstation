package org.janelia.jos;

import static org.janelia.utils.ResourceHelper.notFoundIfNull;
import io.dropwizard.auth.Auth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.http.HttpHeaders;
import org.janelia.jos.auth.SimplePrincipal;
import org.janelia.jos.model.JOSObject;
import org.janelia.jos.mongo.MongoManaged;
import org.janelia.jos.scality.ScalityDAO;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.ResponseHeader;

/**
 * The main resource for dealing with objects in the Janelia Object Store. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Path("/jos")
@Api(value = "/jos", description = "JOS object operations")
public class JOSObjectResource {

    private static final Logger log = LoggerFactory.getLogger(JOSObjectResource.class);

    private static final long MEGABYTE = 1000*1000;
    
    private static final String JOSS_NAME = "X-Joss-Name";
    private static final String JOSS_OWNER = "X-Joss-Owner";
    private static final String JOSS_SIZE = "X-Joss-Size";
    
    private MongoManaged mm;
    private ObjectMapper mapper;
    private ScalityDAO scality;
    
    public JOSObjectResource(MongoManaged mm, ObjectMapper mapper, ScalityDAO scality) {
        this.mm = mm;
        this.mapper = mapper;
        this.scality = scality;
    }
    
    @PUT
    @Path("/object/{path:.+}")
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_FORM_URLENCODED})
    @ApiOperation(value = "Upload an object", 
                  notes = "Put an object into the object store and begin tracking it.")
    @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Successfully saved object to object store")
    })
    public Response putObject(
            @Context HttpServletRequest request,
            @ApiParam(value="Path to object", required=true) @PathParam("path") String path,
            @ApiParam(value="Compress with bzip2 before storing?") @QueryParam("compress") Boolean compress,
            @Auth SimplePrincipal principal,
            InputStream is) throws Throwable {

      //  authorize(principal);
        
        boolean bzipped = compress==null?false:compress;
        
        JOSObject obj = getObject(path, principal, true);
        long contentLength = scality.put(is, path, bzipped);
        
        if (contentLength==0) {
            log.error("Received zero bytes for PUT to {}",path);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        
        if (obj==null) obj = new JOSObject();
         
        File file = new File(path);
        String name = file.getName();
        String extension = name.substring(name.lastIndexOf('.')+1);
        
        obj.setName(name);
        obj.setPath(path);
        obj.setParentPath(file.getParent());
        obj.setFileType(extension);
        obj.setOwner(principal.getUsername());
        obj.setNumBytes(contentLength);
        obj.setBzipped(bzipped);
        
        getObjectCollection().save(obj);
        
        return Response.noContent().build();
    }
    
    @GET
    @Path("/object/{path:.+}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @ApiOperation(value = "Get an object", 
                  notes = "Streams the given object. If the object is marked bzipped, then it will be decompressed as it is streamed. "
                          + "You can also force decompression of objects which were already compressed before being uploaded.")
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Object found", responseHeaders={ 
              @ResponseHeader(name="X-Joss-Name", description="Object Name"),
              @ResponseHeader(name="X-Joss-Owner", description="Object Owner"),
              @ResponseHeader(name="X-Joss-Size", description="Object Size (bytes)")
      }),
      @ApiResponse(code = 404, message = "Object not found")
    })
    public Response getFile(
            @ApiParam(value="Path to object", required=true) @PathParam("path") final String path,
            @ApiParam(value="Force decompression of the stream with bzip2?") @QueryParam("decompress") Boolean decompress,
            @Auth SimplePrincipal principal) throws Exception {

      //  authorize(principal);
        final JOSObject obj = getObject(path, principal, false);
        final boolean bzipped = decompress==null?obj.isBzipped():decompress;
        final InputStream input = scality.get(path, obj.getNumBytes(), bzipped);
        
        StreamingOutput output = new StreamingOutput() {
            public void write(OutputStream output) throws IOException, WebApplicationException {

                CountingOutputStream counted = new CountingOutputStream(output);
                IOUtils.copy(input, counted);
                counted.flush();
                
                if (counted.getByteCount()!=obj.getNumBytes()) {
                    log.warn("Streamed {} bytes, but object has {}",counted.getByteCount(),obj.getNumBytes());
                }
            }
        };
        
        ResponseBuilder rb = Response.ok(output);
        rb.header(JOSS_NAME, obj.getName());
        rb.header(JOSS_OWNER, obj.getOwner());
        if (obj.getNumBytes()!=null) {
            rb.header(JOSS_SIZE, obj.getNumBytes().toString());
            rb.header(HttpHeaders.CONTENT_LENGTH, obj.getNumBytes().toString());
        }
        return rb.build();
    }

    @HEAD
    @Path("/object/{path:.+}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @ApiOperation(value = "Get object headers", 
                  notes = "Returns information about the object as HTTP headers.")
    @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Object found", responseHeaders={ 
              @ResponseHeader(name="X-Joss-Name", description="Object Name"),
              @ResponseHeader(name="X-Joss-Owner", description="Object Owner"),
              @ResponseHeader(name="X-Joss-Size", description="Object Size (bytes)")
      }),
      @ApiResponse(code = 404, message = "Object not found")
    })
    public Response getHeaders(
            @ApiParam(value="Path to object", required=true) @PathParam("path") final String path,
            @Auth SimplePrincipal principal) throws Exception {

      //  authorize(principal);

        final JOSObject obj = getObject(path, principal, false);
        
        ResponseBuilder rb = Response.noContent();
        rb.header(JOSS_NAME, obj.getName());
        rb.header(JOSS_OWNER, obj.getOwner());
        if (obj.getNumBytes()!=null) {
            rb.header(JOSS_SIZE, obj.getNumBytes().toString());
        }
        return rb.build();
    }
    
    @DELETE
    @Path("/object/{path:.+}")
    @ApiOperation(value = "Delete an object", 
                  notes = "Deletes the object specified by the given path")
    @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Successfully marked the object for deletion"),
      @ApiResponse(code = 404, message = "Object not found")
    })
    public Response deleteFile(
                @ApiParam(value="Path to object", required=true) @PathParam("path") final String path,
                @ApiParam(value="Synchronously delete the file from the object store. "
                        + "If false, the object is marked for deletion and deleted asynchronously.") @QueryParam("sync") final boolean sync,
                @Auth SimplePrincipal principal) throws Exception {

      //  authorize(principal);
        
        JOSObject obj = getObject(path, principal, false);
        
        WriteResult wr = getObjectCollection().update("{path:#}",path).with("{$set:{deleted:#}}", true);
        if (wr.getN()<1) {
            log.error("Could not update object to set deleted flag: {}",path);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

        if (sync) {
            if (scality.delete(path)) {
                getObjectCollection().remove("{path:#}",path);
            }
        }
        else {
            log.info("Marked object for deletion: {}", path);
        }
        
        return Response.noContent().build();
    }

    @PUT
    @Path("/metadata/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upload object metadata for a list of paths",
            notes = "Checks to see if objects already exist in the backing store and if so, creates metadata to track it.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successfully saved list of object metadata to object store")
    })
    public Response registerBulkObject(
            @Context HttpServletRequest request,
            String pathRaw,
            @Auth SimplePrincipal principal) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        List<String> paths = mapper.readValue(pathRaw,new TypeReference<List<String>>(){});
        for (String path : paths) {
            try {
                JOSObject existingObj = getObject(path, principal, true);

                Map<String, String> headerMap = scality.head(path);

                if (headerMap == null) {
                    log.info("Cannot register object which does not exist in Scality: {}", path);
                    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                }

                JOSObject obj = existingObj == null ? new JOSObject() : existingObj;

                File file = new File(path);
                String name = file.getName();
                String extension = name.substring(name.lastIndexOf('.') + 1);

                String contentLengthStr = headerMap.get(HttpHeaders.CONTENT_LENGTH);
                if (contentLengthStr != null) {
                    long contentLength = Long.parseLong(contentLengthStr);
                    obj.setNumBytes(contentLength);
                } else {
                    log.warn("Scality did not return content length for object: {}", path);
                    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                }

                obj.setName(name);
                obj.setPath(path);
                obj.setParentPath(file.getParent());
                obj.setFileType(extension);
                obj.setOwner(principal.getUsername());
                obj.setBzipped(false);

                getObjectCollection().save(obj);
            } catch (Exception e) {
                // for now skip to next so we can bulk load
                e.printStackTrace();
                continue;
            }
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/metadata/{path:.+}")
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_FORM_URLENCODED})
    @ApiOperation(value = "Upload object metadata", 
                  notes = "Checks to see if this object already exists in the backing store and if so, creates metadata to track it.")
    @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Successfully saved object metadata to object store")
    })
    public Response registerObject(
            @Context HttpServletRequest request,
            @ApiParam(value="Path to object", required=true) @PathParam("path") String path,
            @Auth SimplePrincipal principal) throws Throwable {

      //  authorize(principal);

        JOSObject existingObj = getObject(path, principal, true);
        
        Map<String,String> headerMap = scality.head(path);

        if (headerMap==null) {
            log.info("Cannot register object which does not exist in Scality: {}",path);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        
        JOSObject obj = existingObj==null ? new JOSObject() : existingObj;
        
        File file = new File(path);
        String name = file.getName();
        String extension = name.substring(name.lastIndexOf('.')+1);
        
        String contentLengthStr = headerMap.get(HttpHeaders.CONTENT_LENGTH);
        if (contentLengthStr!=null) {
            long contentLength = Long.parseLong(contentLengthStr);
            obj.setNumBytes(contentLength);
        }
        else {
            log.warn("Scality did not return content length for object: {}",path);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        
        obj.setName(name);
        obj.setPath(path);
        obj.setParentPath(file.getParent());
        obj.setFileType(extension);
        obj.setOwner(principal.getUsername());
        obj.setBzipped(false);
        
        getObjectCollection().save(obj);
        
        return Response.noContent().build();
    }
    
    @GET
    @Path("/metadata/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get metadata about an object", 
                  notes = "Returns all known metadata about the object specified by the given path", 
                  response = JOSObject.class)
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Returning found object"),
      @ApiResponse(code = 404, message = "Object not found") 
    })
    public JOSObject getMetadata(
            @ApiParam(value="Path to object", required=true) @PathParam("path") final String path,
            @Auth SimplePrincipal principal) {

      //  authorize(principal);
        
        final String finalPath = getFormattedPath(path);
        log.debug("Getting info for "+finalPath);
        
        JOSObject obj = getObjectCollection().findOne("{path:#}",finalPath).as(JOSObject.class);
        notFoundIfNull(obj);
        return obj;
    }

    @GET
    @Path("/search/name/{name:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Listing by object name",
                  notes = "Returns a listing of all known objects with the given name", 
                  response = JOSObject.class,
                  responseContainer = "List")
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Streaming list of results")
    })
    public StreamingOutput getListByName(
            @ApiParam(value="Object name", required=true) @PathParam("name") final String name,
            @Auth SimplePrincipal principal) {
        
     //   authorize(principal);
        
        log.debug("Getting listing for "+name);
        
        return new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                MongoCursor<JOSObject> cursor = getObjectCollection().find("{name:#}",name).as(JOSObject.class);
                writeCursor(os, cursor);
            }
        };
    }

    @GET
    @Path("/search/parent/{parentPath:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Listing by parent",
                  notes = "Returns a listing of all known objects with the given parent path", 
                  response = JOSObject.class,
                  responseContainer = "List")
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Streaming list of results")
    })
    public StreamingOutput getListByParent(
            @ApiParam(value="Parent path", required=true) @PathParam("parentPath") String parentPath,
            @Auth SimplePrincipal principal) {
        
      //  authorize(principal);
        
        final String finalPath = getFormattedPath(parentPath);
        log.debug("Getting listing for " + finalPath);
        
        return new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                MongoCursor<JOSObject> cursor = getObjectCollection().find("{parentPath:#}",finalPath).as(JOSObject.class);
                writeCursor(os, cursor);
            }
        };
    }

    @GET
    @Path("/search/prefix/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Listing by prefix",
                  notes = "Returns a listing of all known objects with the given path prefix", 
                  response = JOSObject.class,
                  responseContainer = "List")
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Streaming list of results")
    })
    public StreamingOutput getListByPrefix(
            @ApiParam(value="Path prefix", required=true) @PathParam("path") String parentPath,
            @Auth SimplePrincipal principal) {
        
      //  authorize(principal);
        
        String regex = getFormattedPath(parentPath)+".*";
        log.debug("Getting listing for " + regex);
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        return new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                MongoCursor<JOSObject> cursor = getObjectCollection().find("{path:#}",pattern).as(JOSObject.class);
                writeCursor(os, cursor);
            }
        };
    }

    @GET
    @Path("/report/usage/owner")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Space usage by owner",
                  notes = "Returns a report of space usage by owner")
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Report returned")
    })
    public Map<String,String> getUsageByOwner(
            @Auth SimplePrincipal principal) {
        
      //  authorize(principal);

        List<DBObject> results = getObjectCollection().aggregate("{\"$group\" : {_id:\"$owner\", totalMb:{$sum:{$divide:[\"$numBytes\","+MEGABYTE+"]}}}}").as(DBObject.class);
        
        Map<String,String> usage = new HashMap<>();
        
        DecimalFormat df = new DecimalFormat("0.0000 MB"); 
        
        for (DBObject result : results) {
        	Double sum = (Double)result.get("totalMb");
        	usage.put(result.get("_id").toString(), df.format(sum));
        }
        
        return usage;
    }
    
    /*
     * HELPER METHODS
     */

    private void writeCursor(OutputStream os, MongoCursor<JOSObject> cursor) throws IOException {
        JsonGenerator jg = mapper.getFactory().createGenerator(os,JsonEncoding.UTF8);
        jg.writeStartArray();
        for (JOSObject obj : cursor) {
            jg.writeObject(obj);
        }
        jg.writeEndArray();
        jg.flush();
    }
    
    private String getFormattedPath(String path) {
        String formattedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
        return formattedPath;
    }
    
    private MongoCollection getObjectCollection() {
        return mm.getJongo().getCollection("object");
    }

    private void authorize(SimplePrincipal principal) {
        if (principal==null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
    }
    
    private JOSObject getObject(String path, SimplePrincipal principal, boolean nullOk) throws Exception {

        String owner = principal.getUsername();
        
        log.trace("Logged in as [{}]",owner);
        
        JOSObject obj = getObjectCollection().findOne("{path:#}",path).as(JOSObject.class);
        if (obj==null) {
            if (nullOk) {
                return null;
            }
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        if (!owner.equals(obj.getOwner())) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        if (obj.isDeleted()) {
            throw new WebApplicationException(Status.GONE);
        }
        
        return obj;
    }
    
}