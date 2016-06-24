package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SampleFacade;

public class SampleFacadeImpl extends RESTClientImpl implements SampleFacade {

    private RESTClientManager manager;
    
    public SampleFacadeImpl() {
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public Collection<DataSet> getDataSets() throws Exception {
        Response response = manager.getDataSetEndpoint()
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
        Response response = manager.getDataSetEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createDataSet from server")) {
            throw new WebApplicationException(response);
        }
        DataSet newDataSet = response.readEntity(DataSet.class);
        return newDataSet;
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(dataSet);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getDataSetEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateDataSet to server: " + dataSet)) {
            throw new WebApplicationException(response);
        }
        DataSet newDataSet = response.readEntity(DataSet.class);
        return newDataSet;
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        Response response = manager.getDataSetEndpoint()
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
        Response response = manager.getSampleEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("sampleId", sampleId)
                .path("lsms")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to get Lsm For Sample: " + sampleId)) {
            throw new WebApplicationException(response);
        }
        List<LSMImage> lsms = response.readEntity((new GenericType<List<LSMImage>>() {}));
        return lsms;
    }
    
    @Override
    public List<LineRelease> getLineReleases() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public LineRelease update(LineRelease release) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(LineRelease release) throws Exception {
        throw new UnsupportedOperationException();
    }
}
