package org.janelia.workstation.webdav;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.internal.util.Base64;
import org.janelia.workstation.webdav.exception.PermissionsFailureException;

@Path("{seg: .*}")
public class WebdavRequestHandler {

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    /**
     * @return File if found
     */
    @GET
   // @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public File getFile() throws PermissionsFailureException, FileNotFoundException {
        String username = getCredentials();
        String filepath = "/" + uriInfo.getPath();
        FileShare mapping = mapResource(filepath);

        // parse out request path
        // make sure user has access to this file share
        if (!mapping.getAuthorizer().checkAccess(username)) {
            throw new PermissionsFailureException("Not allowed to access this file share");
        }
        // check file share for read permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to read from this file share");
        }

        return new File(filepath);
    }

    @PUT
    public String putFile() {
        return "Got it!";
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deleteIt() {
        return "Got it!";
    }

    private String getCredentials() throws PermissionsFailureException {
        List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        // if no basic auth, return error
        if (authHeaders == null || authHeaders.size() < 1) {
            throw new PermissionsFailureException("Not using basic authentication to access webdav");
        }

        // assume Basic request header and scrape username from this;
        // TO DO : handle different authentication methods with a filter populating information for forward
        // propagation
        String encodedBasicAuth = authHeaders.get(0);
        return getBasicAuthUser(encodedBasicAuth);
    }

    private FileShare mapResource(String filepath) throws FileNotFoundException{
        // check out resources and find first matching
        Map<String,FileShare> resourceMap = WebdavContextManager.getResourcesByMapping();
        Iterator<String> mappings = resourceMap.keySet().iterator();
        FileShare mappedResource = null;
        while (mappings.hasNext()) {
            String mappingBase = (String)mappings.next();
            if (filepath.startsWith(mappingBase)) {
                mappedResource = resourceMap.get(mappingBase);
                break;
            }
        }
        if (mappedResource == null) {
            throw new FileNotFoundException("no file share mapped for the file requested.");
        }
        return mappedResource;
    }

    private String getBasicAuthUser(String basicRequestHeader) {
        String base64Credentials = basicRequestHeader.substring("Basic".length()).trim();
        String credentials = new String(Base64.decodeAsString(base64Credentials));
        return credentials.split(":",2)[0];
    }
}
