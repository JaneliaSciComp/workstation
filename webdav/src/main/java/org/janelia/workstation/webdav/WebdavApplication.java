package org.janelia.workstation.webdav;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.janelia.workstation.webdav.exception.PermissionsFailureException;
import org.janelia.workstation.webdav.exception.FileNotFoundException;

import io.swagger.jaxrs.listing.*;
import io.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("api")
public class WebdavApplication extends ResourceConfig {
    final Resource.Builder resourceBuilder;

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    public WebdavApplication() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.2");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setHost("schauderd-ws1:8880");
        beanConfig.setBasePath("/Webdav/api");
        beanConfig.setResourcePackage("org.janelia.workstation.webdav");
        beanConfig.setScan(true);

        resourceBuilder = Resource.builder();
        resourceBuilder.path("file/{seg: .*}");
        resourceBuilder.addMethod("PROPFIND")
                .handledBy(new Inflector<ContainerRequestContext, Response>() {
                    @Override
                    public Response apply(ContainerRequestContext containerRequestContext) {
                        // generate XML response for propfind

                        String filepath = "/" + Util.stripApiPath(uriInfo.getPath());
                        FileShare mapping;
                        String xmlResponse;
                        try {
                            mapping = Util.checkPermissions(filepath, headers, request);
                            if (!mapping.getPermissions().contains(Permission.PROPFIND)) {
                                return Response.status(Response.Status.UNAUTHORIZED).build();
                            }
                            xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                                    mapping.propFind(uriInfo, headers);
                            return Response.status(207).entity(xmlResponse).build();
                        }
                        catch (PermissionsFailureException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.UNAUTHORIZED).build();
                        }
                        catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.CONFLICT).build();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            return Response.status(Response.Status.CONFLICT).build();
                        }
                    }
                });
        resourceBuilder.addMethod("MKCOL")
                .handledBy(new Inflector<ContainerRequestContext, Response>() {
                    @Override
                    public Response apply(ContainerRequestContext containerRequestContext) {
                        String filepath = "/" + Util.stripApiPath(uriInfo.getPath());

                        FileShare mapping;
                        try {
                            mapping = Util.checkPermissions(filepath, headers, request);
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

        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

        packages("org.janelia.workstation.webdav");
    }


}
