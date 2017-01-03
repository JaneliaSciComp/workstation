package org.janelia.jacs2.rest;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.JacsServiceDataManager;
import org.janelia.jacs2.service.ServiceRegistry;
import org.janelia.jacs2.service.impl.JacsService;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Date;

@ApplicationScoped
@Produces("application/json")
@Path("/v2/services")
public class ServiceInfoResource {

    @Inject
    private Logger logger;
    @Inject
    private JacsServiceDataManager jacsServiceDataManager;
    @Inject
    private ServiceRegistry serviceRegistry;

    @GET
    public Response getAllServices(@QueryParam("service-name") String serviceName,
                                   @QueryParam("service-id") Long serviceId,
                                   @QueryParam("parent-id") Long parentServiceId,
                                   @QueryParam("root-id") Long rootServiceId,
                                   @QueryParam("service-owner") String serviceOwner,
                                   @QueryParam("service-state") String serviceState,
                                   @QueryParam("service-from") Date from,
                                   @QueryParam("service-to") Date to,
                                   @QueryParam("page") Integer pageNumber,
                                   @QueryParam("length") Integer pageSize) {
        JacsServiceData pattern = new JacsServiceData();
        pattern.setId(serviceId);
        pattern.setParentServiceId(parentServiceId);
        pattern.setRootServiceId(rootServiceId);
        pattern.setName(serviceName);
        pattern.setOwner(serviceOwner);
        try {
            if (StringUtils.isNotBlank(serviceState)) {
                pattern.setState(JacsServiceState.valueOf(serviceState));
            } else {
                pattern.setState(null);
            }
        } catch (Exception e) {
            logger.error("Invalid state filter {}", serviceState, e);
        }
        PageRequest pageRequest = new PageRequest();
        if (pageNumber != null) {
            pageRequest.setPageNumber(pageNumber);
        }
        if (pageSize != null) {
            pageRequest.setPageSize(pageSize);
        }
        PageResult<JacsServiceData> results = jacsServiceDataManager.searchServices(pattern, from, to, pageRequest);
        return Response
                .status(Response.Status.OK)
                .entity(results)
                .build();
    }

    @GET
    @Path("/{service-instance-id}")
    public Response getServiceInfo(@PathParam("service-instance-id") long instanceId) {
        JacsServiceData serviceData = jacsServiceDataManager.retrieveServiceById(instanceId);
        if (serviceData == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(serviceData)
                    .build();
        } else {
            return Response
                    .status(Response.Status.OK)
                    .entity(serviceData)
                    .build();
        }
    }

    @GET
    @Path("/{service-name}/metadata")
    public Response getServiceMetadata(@PathParam("service-name") String serviceName) {
        ServiceMetaData smd = serviceRegistry.getServiceDescriptor(serviceName);
        if (smd == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }
        return Response
                .status(Response.Status.OK)
                .entity(smd)
                .build();
    }

}
