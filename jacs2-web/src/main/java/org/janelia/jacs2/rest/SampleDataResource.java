package org.janelia.jacs2.rest;

import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.jacs2.model.page.ListResult;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Produces("application/json")
@Path("/v2/samples")
public class SampleDataResource {

    @Inject
    private SampleDataService sampleDataService;

    @GET
    public PageResult<Sample> getAllSamples(@QueryParam("page") Integer pageNumber,
                                            @QueryParam("length") Integer pageLength) {
        return new PageResult<>(); // TODO implement me
    }

    @GET
    @Path("/{sample-id}")
    public Sample getSample(@HeaderParam("authToken") String authToken, @PathParam("sample-id") Long sampleId) {
        return sampleDataService.getSampleById(extractSubjectFromAuthToken(authToken), sampleId);
    }

    @GET
    @Path("/{sample-id}/anatomicalAreas")
    public Response getAnatomicalArea(@HeaderParam("authToken") String authToken, @PathParam("sample-id") Long sampleId, @QueryParam("objective") String objectiveName) {
        List<AnatomicalArea> anatomicalAreas = sampleDataService.getAnatomicalAreasBySampleIdAndObjective(extractSubjectFromAuthToken(authToken), sampleId, objectiveName);
        return Response
                .status(Response.Status.OK)
                .entity(new ListResult<>(anatomicalAreas))
                .build();
    }

    private String extractSubjectFromAuthToken(String authToken) {
        return null; // Not implemented yet.
    }
}
