package org.janelia.workstation.webdav.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
/**
 * Created by schauderd on 6/29/15.
 */


@Provider
public class FileNotFoundExceptionHandler implements ExceptionMapper<FileNotFoundException>
{
    @Override
    public Response toResponse(FileNotFoundException exception)
    {
        return Response.status(Status.NOT_FOUND).entity(exception.getMessage()).build();
    }
}