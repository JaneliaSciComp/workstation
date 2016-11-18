package org.janelia.jacs2.rest;

import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.TaskManager;
import org.janelia.jacs2.service.ServiceRegistry;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("services")
public class ServiceInfoResource {

    @Inject
    private TaskManager taskManager;
    @Inject
    private ServiceRegistry serviceRegistry;

    @GET
    @Produces("application/json")
    public PageResult<TaskInfo> getAllServices(@QueryParam("service-name") String serviceName,
                                                  @QueryParam("service-state") String serviceState) {
        System.out.println("!!!! NAME QUERY PARAM " + serviceName);
        System.out.println("!!!! STATE QUERY PARAM " + serviceState);
        return new PageResult<>(); // TODO implement me
    }

    @GET
    @Path("/{service-instance-id}")
    @Produces("application/json")
    public TaskInfo getServiceInfo(@PathParam("service-instance-id") long instanceId) {
        return taskManager.retrieveTaskInfo(instanceId);
    }

    @GET
    @Path("/{service-type}/metadata")
    @Produces("application/json")
    public Response getServiceMetadata(@PathParam("service-type") String serviceType) {
        ServiceMetaData smd = serviceRegistry.getServiceDescriptor(serviceType);
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
