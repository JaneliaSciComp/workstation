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
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

/**
 * Created by schauderd on 10/5/15.
 */

@Path("file/{path: .*}")
@Api(value = "Janelia Object File Services", description = "Services for managing files in Object Stores (Scality)")
public class FileServices {
    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Get a file from an object store",
            notes = "Get a file from the appropriate file store and return it as a stream.  Path Syntax is [Filestore]/[Path_To_File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The file was successfully retrieved"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have read permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to retrieve the file successfully from the file store")
    })
    public StreamingOutput getFile(@PathParam("path") String path) throws PermissionsFailureException, FileNotFoundException {
        System.out.println ("GET "  + path);

        FileShare mapping = Common.checkPermissions(path, headers, request);
        // check file share for read permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to read from this file share");
        }

        // delegate to FileShare to get file
        return mapping.getFile(response, path);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Upload a file into the object store",
            notes = "Upload a file into the appropriate file store.  Path Syntax is [Filestore]/[Path_To_File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The file was uploaded successfully"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have write permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to write the file into the file store")
    })
    public void putFile(InputStream binaryStream, @PathParam("path") String path) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException {
        System.out.println ("PUT "  + path);

        FileShare mapping = Common.checkPermissions(path, headers, request);
        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to write from this file share");
        }

        mapping.putFile(request, response, binaryStream, path);
    }

    @DELETE
    @ApiOperation(value = "Delete a file in the object store",
            notes = "Delete a file in the file store.  Path Syntax is [Filestore]/[Path_To_File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The file was deleted successfully"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have delete permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to remove the file into the file store")
    })
    public void deleteFile(@PathParam("path") String path) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        System.out.println ("DELETE "  + path);

        FileShare mapping = Common.checkPermissions(path, headers, request);

        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.DELETE)) {
            throw new PermissionsFailureException("Not permitted to delete from this file share");
        }

        mapping.deleteFile(path);
    }
}
