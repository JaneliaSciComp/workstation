package org.janelia.jacs2.rest;

import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.ListResult;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.dataservice.sample.SampleDataService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

@ApplicationScoped
@Produces("application/json")
@Path("/v2/samples")
public class SampleDataResource {
    private static final int DEFAULT_PAGE_SIZE = 100;

    @Inject private SampleDataService sampleDataService;

    @GET
    public Response getAllSamples(
            @HeaderParam("authToken") String authToken,
            @QueryParam("sample-id") Long sampleId,
            @QueryParam("sample-name") String sampleName,
            @QueryParam("sample-owner") String sampleOwner,
            @QueryParam("age") String age,
            @QueryParam("effector") String effector,
            @QueryParam("dataset") String dataset,
            @QueryParam("line") String line,
            @QueryParam("slidecode") String slidecode,
            @QueryParam("gender") String gender,
            @QueryParam("status") String status,
            @QueryParam("tmog-from") Date tmogFrom,
            @QueryParam("tmog-to") Date tmogTo,
            @QueryParam("page") Integer pageNumber,
            @QueryParam("length") Integer pageLength) {
        Sample pattern = new Sample();
        pattern.setId(sampleId);
        pattern.setName(sampleName);
        pattern.setOwnerKey(sampleOwner);
        pattern.setAge(age);
        pattern.setEffector(effector);
        pattern.setDataSet(dataset);
        pattern.setLine(line);
        pattern.setSlideCode(slidecode);
        pattern.setGender(gender);
        pattern.setStatus(status);
        PageRequest pageRequest = new PageRequest();
        if (pageNumber != null) {
            pageRequest.setPageNumber(pageNumber);
        }
        if (pageLength != null) {
            pageRequest.setPageSize(pageLength);
        } else {
            pageRequest.setPageSize(DEFAULT_PAGE_SIZE);
        }
        PageResult<Sample> results = sampleDataService.searchSamples(extractSubjectFromAuthToken(authToken), pattern, new DataInterval<>(tmogFrom, tmogTo), pageRequest);
        return Response
                .status(Response.Status.OK)
                .entity(results)
                .build();
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
