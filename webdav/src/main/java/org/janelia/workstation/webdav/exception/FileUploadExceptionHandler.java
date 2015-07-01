package org.janelia.workstation.webdav.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
/**
 * Created by schauderd on 6/29/15.
 */


@Provider
public class FileUploadExceptionHandler implements ExceptionMapper<FileUploadException>
{
    @Override
    public Response toResponse(FileUploadException exception)
    {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }
}