package org.janelia.jacs2.rest;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.JacsServiceDataManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
    private JacsServiceDataManager jacsServiceDataManager;

    @POST
    @Path("/{user}/{service-name}")
    @Consumes("application/json")
    public Response createAsyncService(@PathParam("user") String userName, @PathParam("service-name") String serviceName, JacsServiceData si) {
        si.setOwner(userName);
        si.setName(serviceName);
        JacsServiceData newJacsServiceData = jacsServiceDataManager.submitServiceAsync(si, Optional.empty());
        UriBuilder locationURIBuilder = UriBuilder.fromResource(ServiceInfoResource.class);
        locationURIBuilder.path(newJacsServiceData.getId().toString());
        return Response
                .status(Response.Status.CREATED)
                .entity(newJacsServiceData)
                .contentLocation(locationURIBuilder.build())
                .build();
    }

    @GET
    @Path("/stats")
    public Response getServerStats() {
        ServerStats stats = jacsServiceDataManager.getServerStats();
        return Response
                .status(Response.Status.OK)
                .entity(stats)
                .build();
    }

    @PUT
    @Path("/processing-slots-count/{slots-count}")
    public Response setProcessingSlotsCount(@PathParam("slots-count") int nProcessingSlots) {
        jacsServiceDataManager.setProcessingSlotsCount(nProcessingSlots);
        return Response
                .status(Response.Status.OK)
                .build();
    }
}
