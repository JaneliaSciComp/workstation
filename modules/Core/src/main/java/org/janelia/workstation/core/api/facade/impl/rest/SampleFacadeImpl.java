package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.workstation.core.api.facade.interfaces.SampleFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.model.domain.dto.SampleReprocessingRequest;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.LineRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleFacadeImpl extends RESTClientBase implements SampleFacade {

    private static final Logger log = LoggerFactory.getLogger(SampleFacadeImpl.class);

    private WebTarget service;
    
    public SampleFacadeImpl() {
        this(ConsoleProperties.getInstance().getProperty("domain.facade.rest.url"));
    }

    public SampleFacadeImpl(String serverUrl) {
        super(log);
        log.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    @Override
    public Collection<DataSet> getDataSets() throws Exception {
        Response response = service.path("data/dataset")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDataSets from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<DataSet>>() {});
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(dataSet);
        Response response = service.path("data/dataset")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createDataSet from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(DataSet.class);
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(dataSet);
        // TODO: this is a hack to allow admins to edit data sets. We need to implement a more comprehensive solution to this.
        if (AccessManager.getAccessManager().isAdmin()) {
            query.setSubjectKey(dataSet.getOwnerKey());
        }
        else {
            query.setSubjectKey(AccessManager.getSubjectKey());
        }
        Response response = service.path("data/dataset")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateDataSet to server: " + dataSet)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(DataSet.class);
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        Response response = service.path("data/dataset")
                .queryParam("dataSetId", dataSet.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeDataSet from server: " + dataSet)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) throws Exception {
        Response response = service.path("data/sample/lsms")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("sampleId", sampleId)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to get Lsm For Sample: " + sampleId)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity((new GenericType<List<LSMImage>>() {}));
    }

    @Override
    public List<LineRelease> getLineReleases() throws Exception {
        Response response = service.path("process/release")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to get line releases")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity((new GenericType<List<LineRelease>>() {}));
    }

    @Override
    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        DomainQuery query = new DomainQuery();

        LineRelease release = new LineRelease();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLagTimeMonths(lagTimeMonths);
        release.setDataSets(dataSets);

        query.setDomainObject(release);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = service.path("process/release")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createLineRelease to server: " + release)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(LineRelease.class);
    }

    @Override
    public LineRelease update(LineRelease release) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(release);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = service.path("process/release")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateLineRelease to server: " + release)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(LineRelease.class);
    }

    @Override
    public void remove(LineRelease release) throws Exception {
        Response response = service.path("process/release")
                .queryParam("releaseId", release.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeRelease from server: " + release)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public String dispatchSamples(SampleReprocessingRequest request) throws Exception {
        Response response = service.path("process/sample/reprocess")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .post(Entity.json(request));
        if (checkBadResponse(response.getStatus(), "problem making request to dispatch samples")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(String.class);
    }

    @Override
    public Long dispatchTask(JsonTask task, String processName) throws Exception {
        Response response = service.path("process/dispatch")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("processName", processName)
                .request("application/json")
                .post(Entity.json(task));
        if (checkBadResponse(response.getStatus(), "problem making request to dispatch samples")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Long.class);
    }
    
    @Override
    public Collection<DataSet> getColorDepthDataSets(String alignmentSpace) throws Exception {
        Response response = service.path("data/dataset/colordepth")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("alignmentSpace", alignmentSpace)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDataSets from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<DataSet>>() {});
    }

}
