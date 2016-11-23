package org.janelia.jacs2.rest;

import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.TaskManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Optional;

@ApplicationScoped
@Produces("application/json")
@Path("/v2/async-services")
public class AsyncServiceInfoResource {

    @Inject
    private TaskManager taskManager;

    @POST
    @Path("/{user}/{service-name}")
    @Consumes("application/json")
    public Response createAsyncService(@PathParam("user") String userName, @PathParam("service-name") String serviceName, TaskInfo si) {
        si.setOwner(userName);
        si.setName(serviceName);
        TaskInfo newTaskInfo = taskManager.submitTaskAsync(si, Optional.empty());
        UriBuilder locationURIBuilder = UriBuilder.fromResource(ServiceInfoResource.class);
        locationURIBuilder.path(Long.toString(newTaskInfo.getId()));
        return Response
                .status(Response.Status.CREATED)
                .entity(newTaskInfo)
                .contentLocation(locationURIBuilder.build())
                .build();
    }

    @GET
    @Path("/stats")
    public Response getServerStats() {
        ServerStats stats = taskManager.getServerStats();
        stats.setRunningTasks(1);
        return Response
                .status(Response.Status.OK)
                .entity(stats)
                .build();
    }
}
