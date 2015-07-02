package org.janelia.workstation.webdav;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.janelia.workstation.webdav.exception.FileUploadException;
import org.janelia.workstation.webdav.exception.PermissionsFailureException;
import org.janelia.workstation.webdav.exception.FileNotFoundException;
import org.janelia.workstation.webdav.propfind.Multistatus;
import org.janelia.workstation.webdav.propfind.Prop;
import org.janelia.workstation.webdav.propfind.PropfindResponse;
import org.janelia.workstation.webdav.propfind.Propstat;

@Path("{seg: .*}")
public class WebdavRequestHandler extends ResourceConfig {
    final Resource.Builder resourceBuilder;

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
                                    propFind();
                            return Response.status(207).entity(xmlResponse).build();
                        } catch (PermissionsFailureException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.UNAUTHORIZED).build();
                        } catch (FileNotFoundException e) {
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

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    private PropfindResponse generatePropMetadata(java.nio.file.Path file) {
        PropfindResponse fileMeta = new PropfindResponse();
        try {
            Propstat propstat = new Propstat();
            Prop prop = new Prop();
            prop.setCreationDate(Files.getAttribute(file, "creationTime").toString());
            prop.setGetContentType(Files.probeContentType(file));
            prop.setGetContentLength(Long.toString(Files.size(file)));
            prop.setGetLastModified(Files.getLastModifiedTime(file).toString());
            String baseURI = uriInfo.getBaseUri().toString();
            fileMeta.setHref("/Webdav" + file.toString());
            if (Files.isDirectory(file)) {
                prop.setResourceType("collection");
                fileMeta.setHref(fileMeta.getHref() + "/");
            }
            propstat.setProp(prop);
            propstat.setStatus("HTTP/1.1 200 OK");
            fileMeta.setPropstat(propstat);
        } catch (IOException e) {
            e.printStackTrace();
            // problem getting file metadata information
        }
        return fileMeta;
    }

    private void discoverFiles(Multistatus container, java.nio.file.Path file, int depth, int discoveryLevel) {
        if (depth<discoveryLevel) {
            if (Files.isDirectory(file)) {
                depth++;
                try (DirectoryStream<java.nio.file.Path> directoryStream = Files.newDirectoryStream(file)) {
                    for (java.nio.file.Path subpath : directoryStream) {
                        discoverFiles(container, subpath, depth, discoveryLevel);
                    }
                }
                catch (IOException ex) {
                    // handle issues with reading subdirectory metadata
                    ex.printStackTrace();
                }
            }
        }
        container.getResponse().add(generatePropMetadata(file));
    }


    private String propFind() throws FileNotFoundException {
        String filepath = "/" + uriInfo.getPath();
        System.out.println (filepath);
       /* try {
            FileShare mapping = checkPermissions(filepath);
        } catch (PermissionsFailureException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/

        // create Multistatus top level
        Multistatus propfindContainer = new Multistatus();

        java.nio.file.Path fileHandle = Paths.get(filepath);
        if (Files.exists(fileHandle)) {
            // check DEPTH header to check whether to get subdirectory information
            int discoveryLevel = 0;
            List<String> depth = headers.getRequestHeader("Depth");
            if (depth != null && depth.size() > 0) {
                String depthValue = depth.get(0).trim();
                System.out.println ("Depth is " + depthValue);
                if (depthValue.toLowerCase().equals("infinity")) {
                    discoveryLevel = 20; // prevents insanity
                } else {
                    discoveryLevel = Integer.parseInt(depthValue);
                }
            }
            discoverFiles(propfindContainer, fileHandle, 0, discoveryLevel);
        } else {
            throw new FileNotFoundException("File does not exist");
        }

        ObjectMapper xmlMapper = new XmlMapper();
        String xml = null;
        try {
            xml = xmlMapper.writeValueAsString(propfindContainer);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return xml;

    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public File getFile() throws PermissionsFailureException, FileNotFoundException {
        String filepath = "/" + uriInfo.getPath();
        System.out.println (filepath);
        /*FileShare mapping = checkPermissions(filepath);

        // check file share for read permissions
        if (!mapping.getPermissions().contains(Permission.READ)) {
            throw new PermissionsFailureException("Not permitted to read from this file share");
        }*/

        return new File(filepath);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void putFile(InputStream binaryStream) throws PermissionsFailureException,
            FileNotFoundException, FileUploadException {
        String filepath = "/" + uriInfo.getPath();
        System.out.println (filepath);
        FileShare mapping = checkPermissions(filepath);

        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.WRITE)) {
            throw new PermissionsFailureException("Not permitted to write from this file share");
        }

        try {
            Files.copy(binaryStream,
                    Paths.get(filepath),
                    new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileUploadException("Problem creating new file on file share");
        }
    }

    @DELETE
    public void deleteFile() throws PermissionsFailureException,
            FileNotFoundException, FileUploadException {
        String filepath = "/" + uriInfo.getPath();
        System.out.println (filepath);
        FileShare mapping = checkPermissions(filepath);

        // check file share for write permissions
        if (!mapping.getPermissions().contains(Permission.DELETE)) {
            throw new PermissionsFailureException("Not permitted to delete from this file share");
        }
        try {
            Files.delete(Paths.get(filepath));
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileUploadException("Problem removing file from file share");
        }

    }

    private FileShare checkPermissions(String filepath) throws PermissionsFailureException,FileNotFoundException {
        Token credentials = getCredentials();
        FileShare mapping = mapResource(filepath);

        // parse out request path
        // make sure user has access to this file share
        if (!mapping.getAuthorizer().checkAccess(credentials)) {
            throw new PermissionsFailureException("Not allowed to access this file share");
        }
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
