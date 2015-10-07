package org.janelia.workstation.webdav;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.janelia.workstation.webdav.exception.FileNotFoundException;
import org.janelia.workstation.webdav.exception.FileUploadException;
import org.janelia.workstation.webdav.exception.PermissionsFailureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by schauderd on 10/5/15.
 */

@Path("/")
@Api(value = "Janelia Object File Services", description = "Services for managing files in Object Stores (Scality)")
public class MetadataResource {
    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @POST
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
        System.out.println("SEARCH " + uriInfo.getPath());
        FileShare mapping = Util.checkPermissions(uriInfo.getPath().substring("search".length()), headers, request);
        System.out.println ("Accessing data from " + mapping.getMapping() + " file store");
        // check file share for search permissions
        if (!mapping.getPermissions().contains(Permission.SEARCH)) {
            throw new PermissionsFailureException("Not permitted to search this file share");
        }

        return mapping.searchFile(response, name);
    }

    @POST
    @Path("/info/{path:.+}")
    @ApiOperation(value = "Retrieve metadata information (path, status, size) for a filename in the object store",
            notes = "Path returns the metadata in JSON format.  Path Syntax is [Filestore]/[File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Metdata information is attached in JSON format"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have info permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to get info for this file store")
    })
    public StreamingOutput getInfo(@PathParam("path") String path) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        FileShare mapping = Util.checkPermissions(uriInfo.getPath().substring("info".length()), headers, request);

        // check file share for info permissions
        if (!mapping.getPermissions().contains(Permission.INFO)) {
            throw new PermissionsFailureException("Not permitted to get information for this file share");
        }

        return mapping.getInfo(response, path);
    }
}
