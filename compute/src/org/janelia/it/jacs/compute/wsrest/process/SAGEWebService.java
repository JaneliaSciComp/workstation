package org.janelia.it.jacs.compute.wsrest.process;

import java.rmi.RemoteException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.janelia.it.jacs.model.tasks.utility.LSMProcessingTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;

/**
 * Defines RESTful web service APIs for SAGE data processing.
 *
 * @author Eric Trautman
 */
@Path("/process")
@Api(value = "Janelia Workstation Pipelines")
public class SAGEWebService extends ResourceConfig {
    private final Logger logger = Logger.getLogger(this.getClass());

    @Context
    SecurityContext securityContext;

    public SAGEWebService() {
        register(JacksonFeature.class);
    }

    /**
     * Create and launch pipeline processing tasks for the samples associated with a list of lsm files.
     *
     * @param  owner id of the person or system submitting this request
     *
     * @param lsmProcessingParams input parameters encapsulated in a LSMProcessingTask
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    @Path("/owner/{owner}/dataSet/{dataSet}/lsmPipelines")
    public Response launchLsmPipelines(
            @PathParam("owner")String owner,
            @PathParam("dataSet")String dataSet,
            LSMProcessingTask lsmProcessingParams,
            @Context UriInfo uriInfo) {

        final String context = "launchLsmPipelines: ";
        logger.info(context +"entry, owner=" + owner +
                ", dataset=" + dataSet +
                ", lsms=" + lsmProcessingParams.getLsmNames().toString());

        Response response;
        LSMProcessingTask lsmProcessingTask;
        String dataOwner = null;
        try {
            final ComputeBeanRemote remoteComputeBean = EJBFactory.getRemoteComputeBean();

            dataOwner = extractOwner(dataSet, owner);
            checkOwner(dataOwner, remoteComputeBean);
            lsmProcessingParams.setOwner(dataOwner);
            lsmProcessingParams.setDataSetName(dataSet);
            lsmProcessingTask = (LSMProcessingTask) remoteComputeBean.saveOrUpdateTask(lsmProcessingParams);
            logger.info(context + "submitting task " + lsmProcessingParams + " as " + dataOwner);
            remoteComputeBean.submitJob(lsmProcessingTask.getJobName(), lsmProcessingTask.getObjectId());

            JsonTask result = new JsonTask(lsmProcessingTask);
            result.setTaskStatusUrl(getNormalizedBaseUrlString(uriInfo) + "task/" + lsmProcessingTask.getObjectId() + "/currentStatus");
            result.setTaskUrl(getNormalizedBaseUrlString(uriInfo) + "tasks/" + lsmProcessingTask.getObjectId());

            response = Response
                    .status(Response.Status.CREATED)
                    .entity(result)
                    .build();

        } catch (IllegalArgumentException e) {
            response = getErrorResponse(context, Response.Status.BAD_REQUEST, e.getMessage(), e);
        } catch (RemoteException | ComputeException e) {
            response = getErrorResponse(context,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "failed to run lsm processing for " + dataOwner + ":" + lsmProcessingParams,
                    e);
        }

        return response;
    }

    private void checkOwner(String dataOwner, ComputeBeanRemote remoteComputeBean) throws ComputeException {
        if (dataOwner == null) {
            throw new IllegalArgumentException("data set owner value is not defined");
        }
        else {
            final Subject user = remoteComputeBean.getSubjectByNameOrKey(dataOwner);
            if (user == null) {
                throw new IllegalArgumentException("invalid owner parameter '" + dataOwner + "' specified");
            }
        }
    }

    private String getNormalizedBaseUrlString(UriInfo uriInfo) {
        StringBuilder sb = new StringBuilder(uriInfo.getBaseUri().toString());
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private Response getErrorResponse(String context, Response.Status status, String errorMessage, Exception e)  {
        final RestfulWebServiceFailure failure = new RestfulWebServiceFailure(errorMessage, e);
        if (e != null) {
            logger.error(context + errorMessage, e);
        }
        return Response.status(status).entity(failure).build();
    }

    private String extractOwner(String dataSetName, String defaultName) {
        int firstUnderscorePos = dataSetName.indexOf('_');
        if (firstUnderscorePos > -1) {
            return dataSetName.substring(0, firstUnderscorePos);
        }
        else {
            logger.warn("No owner name found, as part of dataset name /" + dataSetName + "/.  Instead using " + defaultName);
            return defaultName;
        }
    }
}
