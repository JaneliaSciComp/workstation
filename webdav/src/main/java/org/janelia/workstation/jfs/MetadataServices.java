package org.janelia.workstation.jfs;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.janelia.workstation.jfs.fileshare.FileShare;
import org.janelia.workstation.jfs.security.Permission;
import org.janelia.workstation.jfs.exception.FileNotFoundException;
import org.janelia.workstation.jfs.exception.FileUploadException;
import org.janelia.workstation.jfs.exception.PermissionsFailureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by schauderd on 10/5/15.
 */

@Path("/")
@Api(value = "Janelia Object File Services", description = "Services for managing files in Object Stores (Scality)")
public class MetadataServices {
    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @GET
    @Path("/search/{name:.+}")
    @ApiOperation(value = "Search for a filename in the object store",
            notes = "Searches for all files that match exactly the name from the appropriate file store (no wildcards yet) and return the results in JSON format.  Name Syntax is [Filestore]/[File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Search results are attached in JSON format"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have search permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to search this file store")
    })
    //TO DO: I'm hardcoding to a mapping now until I can enhance JOSS to use different file shares
    public StreamingOutput searchFile(@PathParam("name") String name) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        System.out.println("SEARCH " + name);

        FileShare mapping = Common.checkPermissions(name, headers, request);
        // check file share for search permissions
        if (!mapping.getPermissions().contains(Permission.SEARCH)) {
            throw new PermissionsFailureException("Not permitted to search this file share");
        }

        return mapping.searchFile(response, name);
    }

    @GET
    @Path("/info/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve metadata information (path, status, size) for a filename in the object store",
            notes = "Path returns the metadata in JSON format.  Path Syntax is [Filestore]/[File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Metadata information is attached in JSON format"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have info permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to get info for this file store")
    })
    public Object getInfo(@PathParam("path") String path) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        System.out.println("INFO " + path);

        FileShare mapping = Common.checkPermissions(path, headers, request);
        // check file share for info permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to get information for this file share");
        }

        return mapping.getInfo(response, path);
    }

    @PUT
    @Path("/register/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Takes a json map of metadata information about a file in the object store, checks whether it exists in Scality, and then stores the metadata in the fileservice's persistence store.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully registered object metadata in fileservice persistence store"),
            @ApiResponse(code = 400, message = "File metadata doesn't contain minimum parameters (fileservice, key)"),
            @ApiResponse(code = 404, message = "File Key not found in object store"),
            @ApiResponse(code = 500, message = "Internal Server Exception")
    })
    public void uploadMetadataInfo(Map<String, String> metadata) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        System.out.println("UPLOAD FILE META " + metadata);

        if (metadata==null || !metadata.containsKey("key") || !metadata.containsKey("fileservice")) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        FileShare mapping = Common.checkPermissions((String)metadata.get("fileservice"), headers, request);

        // check file share for info permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to register information for this file share");
        }

        mapping.uploadMetadata(request, metadata);
    }

    @POST
    @Path("/register/{path:.+}")
    @ApiOperation(value = "Looks up object info in object store and stores metadata information (path, status, size)",
            notes = "Path looks up the object and generates metadata in mongodb.  Path Syntax is [Filestore]/[File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully found file and generated data"),
            @ApiResponse(code = 404, message = "Either the file share doesn't have register permissions, or the file doesn't exist"),
            @ApiResponse(code = 500, message = "Unable to get info for this file store")
    })
    public void registerFile(@PathParam("path") String path) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        System.out.println("REGISTER " + path);

        FileShare mapping = Common.checkPermissions(path, headers, request);
        // check file share for info permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to register information for this file share");
        }

        mapping.registerFile(request, path);
    }

    @PUT
    @Path("/register/bulk")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Takes a json array of file paths and generates (path, status, size) for all the files",
            notes = "Path looks up the object keys listed in the and generates metadata in mongodb.  JSON payload is \"filestore\":<filestoreId>,\n\"objects\":array of paths (not including file store key)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully processed files and generated success report"),
            @ApiResponse(code = 404, message = "Either the file share doesn't have register permissions, or the file doesn't exist"),
            @ApiResponse(code = 500, message = "Problem processing this bulk file json or internal server error reading from this file store")
    })
    public Map<String,String> loadBulkMedataFiles(Map<String, Object> params) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        System.out.println("BULK REGISTER " + params);

        if (params==null || !params.containsKey("filestore") || !params.containsKey("objects")) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        FileShare mapping = Common.checkPermissions((String)params.get("filestore"), headers, request);
        // check file share for info permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to register information for this file share");
        }

        return mapping.registerBulkFiles(request, (String)params.get("filestore"), (List<String>)params.get("objects"));
    }

    @GET
    @Path("/reports/{store:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Looks up usage data for a data store by owner",
            notes = "Looks up usage data for a data store by owner")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully found file and generated data"),
    })
    public Map<String,String> generateOwnerUsageReports(@PathParam("store") String store) throws FileNotFoundException,PermissionsFailureException {
        System.out.println("REPORTS " + store);

        FileShare mapping = Common.checkPermissions(store, headers, request);
        // check file share for info permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to generate reports for this fileshare");
        }

        return mapping.generateUsageReports(store);
    }
}
