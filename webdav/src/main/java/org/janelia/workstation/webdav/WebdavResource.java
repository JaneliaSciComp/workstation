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
public class WebdavResource {
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
    public StreamingOutput getFile() throws PermissionsFailureException, FileNotFoundException {
        String filepath = "/" + Util.stripApiPath(uriInfo.getPath());
        System.out.println ("GET "  + filepath);

        FileShare mapping = Util.checkPermissions(filepath, headers, request);
        // check file share for read permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to read from this file share");
        }

        // delegate to FileShare to get file
        return mapping.getFile(response, filepath);
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
    public void putFile(InputStream binaryStream) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException {
        String filepath = "/" + Util.stripApiPath(uriInfo.getPath());

        System.out.println ("PUT "  + filepath);
        FileShare mapping = Util.checkPermissions(filepath, headers, request);
        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to write from this file share");
        }

        mapping.putFile(binaryStream, filepath);
    }

    @DELETE
    @ApiOperation(value = "Delete a file in the object store",
            notes = "Delete a file in the file store.  Path Syntax is [Filestore]/[Path_To_File_Name]. Example: {path}=DATA1/somefile.ext")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The file was deleted successfully"),
            @ApiResponse(code = 401, message = "Either the file share doesn't have delete permissions, or the user is not authorized to access this share"),
            @ApiResponse(code = 500, message = "Unable to remove the file into the file store")
    })
    public void deleteFile() throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        String filepath = "/" + Util.stripApiPath(uriInfo.getPath());
        FileShare mapping = Util.checkPermissions(filepath, headers, request);

        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.DELETE)) {
            throw new PermissionsFailureException("Not permitted to delete from this file share");
        }

        mapping.deleteFile(filepath);
    }
}
