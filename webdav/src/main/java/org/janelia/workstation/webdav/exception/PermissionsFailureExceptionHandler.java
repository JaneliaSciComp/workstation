package org.janelia.workstation.webdav.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
/**
 * Created by schauderd on 6/29/15.
 */


@Provider
public class PermissionsFailureExceptionHandler implements ExceptionMapper<PermissionsFailureException>
{
    @Override
    public Response toResponse(PermissionsFailureException exception)
    {
        return Response.status(Status.UNAUTHORIZED).entity(exception.getMessage()).build();
    }
}