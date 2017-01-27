package org.janelia.jacs2.rest;

import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.ServerStats;
import org.janelia.jacs2.asyncservice.JacsServiceDataManager;

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
import java.util.List;

@ApplicationScoped
@Produces("application/json")
@Path("/v2/async-services")
public class AsyncServiceInfoResource {

    @Inject private JacsServiceDataManager jacsServiceDataManager;
    @Inject private JacsServiceEngine jacsServiceEngine;

    @POST
    @Consumes("application/json")
    public Response createAsyncServices(List<JacsServiceData> services) {
        List<JacsServiceData> newServices = jacsServiceDataManager.createMultipleServices(services);
        return Response
                .status(Response.Status.CREATED)
                .entity(newServices)
                .build();
    }

    @POST
    @Path("/{service-name}")
    @Consumes("application/json")
    public Response createAsyncService(@PathParam("service-name") String serviceName, JacsServiceData si) {
        si.setName(serviceName);
        JacsServiceData newJacsServiceData = jacsServiceDataManager.createSingleService(si);
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
        ServerStats stats = jacsServiceEngine.getServerStats();
        return Response
                .status(Response.Status.OK)
                .entity(stats)
                .build();
    }

    @PUT
    @Path("/processing-slots-count/{slots-count}")
    public Response setProcessingSlotsCount(@PathParam("slots-count") int nProcessingSlots) {
        jacsServiceEngine.setProcessingSlotsCount(nProcessingSlots);
        return Response
                .status(Response.Status.OK)
                .build();
    }
}
