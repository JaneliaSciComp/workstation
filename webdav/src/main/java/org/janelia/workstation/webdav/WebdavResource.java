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
@Path("{seg: .*}")
@Api
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
    @ApiOperation(value = "Say Hello World",
            notes = "Anything Else?")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Something wrong in Server")})
    public StreamingOutput getFile() throws PermissionsFailureException, FileNotFoundException {
        String filepath = "/" + uriInfo.getPath();
        System.out.println ("GET " + filepath);
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
    public void putFile(InputStream binaryStream) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException {
        String filepath = "/" + uriInfo.getPath();
        System.out.println ("PUT " + filepath);
        FileShare mapping = Util.checkPermissions(filepath, headers, request);
        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to write from this file share");
        }

        mapping.putFile(binaryStream, filepath);
    }

    @DELETE
    public void deleteFile() throws PermissionsFailureException,
            FileNotFoundException, FileUploadException, IOException {
        String filepath = "/" + uriInfo.getPath();
        FileShare mapping = Util.checkPermissions(filepath, headers, request);

        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.DELETE)) {
            throw new PermissionsFailureException("Not permitted to delete from this file share");
        }

        mapping.deleteFile(filepath);
    }
}
