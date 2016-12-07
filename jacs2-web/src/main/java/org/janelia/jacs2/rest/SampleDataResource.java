package org.janelia.jacs2.rest;

import org.janelia.jacs2.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.model.domain.sample.Sample;
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
    @Path("/{sample-id}/objective/{objective}/anatomicalArea")
    public Response getAnatomicalArea(@HeaderParam("authToken") String authToken, @PathParam("sample-id") Long sampleId, @PathParam("objective") String objectiveName) {
        Optional<AnatomicalArea> anatomicalArea = sampleDataService.getAnatomicalAreaBySampleIdAndObjective(extractSubjectFromAuthToken(authToken), sampleId, objectiveName);
        if (anatomicalArea.isPresent()) {
            return Response
                    .status(Response.Status.OK)
                    .entity(anatomicalArea.get())
                    .build();
        } else {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }
    }

    private String extractSubjectFromAuthToken(String authToken) {
        return null; // Not implemented yet.
    }
}
