package org.janelia.jacs2.rest;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.ServiceManager;

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

@Path("async-services")
public class AsyncServiceInfoResource {

    @Inject
    private ServiceManager serviceManager;

    @POST
    @Path("/{user}/{service-name}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createAsyncService(@PathParam("user") String userName, @PathParam("service-name") String serviceName, ServiceInfo si) {
        si.setOwner(userName);
        si.setName(serviceName);
        ServiceInfo newServiceInfo = serviceManager.startAsyncService(si, Optional.empty());
        UriBuilder locationURIBuilder = UriBuilder.fromResource(ServiceInfoResource.class);
        locationURIBuilder.path(Long.toString(newServiceInfo.getId()));
        return Response
                .status(Response.Status.CREATED)
                .entity(newServiceInfo)
                .contentLocation(locationURIBuilder.build())
                .build();
    }

    @GET
    @Path("stats")
    @Produces("application/json")
    public ServerStats getServerStats() {
        return serviceManager.getServerStats();
    }
}
