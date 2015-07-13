package org.janelia.workstation.webdav;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.janelia.workstation.webdav.exception.FileUploadException;
import org.janelia.workstation.webdav.exception.PermissionsFailureException;
import org.janelia.workstation.webdav.exception.FileNotFoundException;

@Path("{seg: .*}")
public class WebdavRequestHandler extends ResourceConfig {
    final Resource.Builder resourceBuilder;

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    public WebdavRequestHandler() {
        resourceBuilder = Resource.builder();
        resourceBuilder.path("{seg: .*}");
        resourceBuilder.addMethod("PROPFIND")
                .handledBy(new Inflector<ContainerRequestContext, Response>() {
                    @Override
                    public Response apply(ContainerRequestContext containerRequestContext) {
                        // generate XML response for propfind
                        String filepath = "/" + uriInfo.getPath();
                        FileShare mapping;
                        String xmlResponse;
                        try {
                            mapping = checkPermissions(filepath);
                            if (!mapping.getPermissions().contains(Permission.PROPFIND)) {
                                return Response.status(Response.Status.UNAUTHORIZED).build();
                            }
                            xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                                    mapping.propFind(uriInfo, headers);
                            return Response.status(207).entity(xmlResponse).build();
                        } catch (PermissionsFailureException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.UNAUTHORIZED).build();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.CONFLICT).build();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.CONFLICT).build();
                        }
                    }
                });
        resourceBuilder.addMethod("MKCOL")
                .handledBy(new Inflector<ContainerRequestContext, Response>() {
                    @Override
                    public Response apply(ContainerRequestContext containerRequestContext) {
                        String filepath = "/" + uriInfo.getPath();
                        FileShare mapping;
                        try {
                            mapping = checkPermissions(filepath);
                            System.out.println (mapping.getPermissions());
                            if (!mapping.getPermissions().contains(Permission.MKCOL)) {
                                return Response.status(Response.Status.UNAUTHORIZED).build();
                            }
                            Files.createDirectory(Paths.get(filepath));
                        } catch (PermissionsFailureException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.UNAUTHORIZED).build();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.CONFLICT).build();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.CONFLICT).build();
                        }

                        return Response.status(Response.Status.CREATED).build();
                    }
                });
        final Resource resource = resourceBuilder.build();
        registerResources(resource);
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getFile() throws PermissionsFailureException, FileNotFoundException {
        String filepath = "/" + uriInfo.getPath();
        FileShare mapping = checkPermissions(filepath);

        // check file share for read permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to read from this file share");
        }

        // delegate to FileShare to get file
        return mapping.getFile(filepath);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void putFile(InputStream binaryStream) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException {
        String filepath = "/" + uriInfo.getPath();
        FileShare mapping = checkPermissions(filepath);

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
        FileShare mapping = checkPermissions(filepath);

        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.DELETE)) {
            throw new PermissionsFailureException("Not permitted to delete from this file share");
        }

        mapping.deleteFile(filepath);
    }

    private FileShare checkPermissions(String filepath) throws PermissionsFailureException,FileNotFoundException {
        Token credentials = getCredentials();
        FileShare mapping = mapResource(filepath);

        // make sure user has access to this file share
        if (!mapping.hasAccess(credentials)) {
            throw new PermissionsFailureException("Not allowed to access this file share");
        }

        // since the check passed, store the authorized FileShare in the session
        HttpSession session = request.getSession();
        session.setAttribute(mapping.getMapping(), mapping);
        return mapping;
    }

    private Token getCredentials() throws PermissionsFailureException {
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

    private FileShare mapResource(String filepath) throws FileNotFoundException {
        // if user already requested this resource, skip the mapping and permissions checks
        HttpSession session = request.getSession();
        Enumeration<String> mapNames = session.getAttributeNames();
        String bestMatch = null;
        while (mapNames.hasMoreElements()) {
            String attName = mapNames.nextElement();
            if (filepath.startsWith(attName)) {
                if (bestMatch==null || attName.length()>bestMatch.length())
                    bestMatch = attName;
            }
        }
        if (bestMatch!=null) {
            return (FileShare)session.getAttribute(bestMatch);
        }

        // check out resources and find first matching
        Map<String,FileShare> resourceMap = WebdavContextManager.getResourcesByMapping();
        Iterator<String> mappings = resourceMap.keySet().iterator();
        FileShare mappedResource = null;
        bestMatch = null;
        while (mappings.hasNext()) {
            String mappingBase = (String)mappings.next();
            if (filepath.startsWith(mappingBase)) {
                if (bestMatch==null || mappingBase.length()>bestMatch.length()) {
                    bestMatch = mappingBase;
                }
            }
        }
        if (bestMatch!=null) {
            mappedResource = resourceMap.get(bestMatch);
        }

        if (mappedResource == null) {
            throw new FileNotFoundException("no file share mapped for the file requested.");
        }

        return (FileShare)mappedResource.clone();
    }

    private BasicAuthToken getBasicAuthUser(String basicRequestHeader) {
        String base64Credentials = basicRequestHeader.substring("Basic".length()).trim();
        String credentials = new String(Base64.decodeAsString(base64Credentials));
        BasicAuthToken token = new BasicAuthToken();
        String[] creds = credentials.split(":",2);
        token.setUsername(creds[0]);
        token.setPassword(creds[1]);
        return token;
    }
}
