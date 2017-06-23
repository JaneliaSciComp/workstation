package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.dto.SampleDispatchRequest;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.it.workstation.browser.api.http.pool.RsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleFacadeImpl extends RESTClientImpl implements SampleFacade {

    private static final Logger log = LoggerFactory.getLogger(SampleFacadeImpl.class);
    private static final String DEFAULT_REMOTE_REST_URL = ConsoleApp.getConsoleApp().getRemoteRestUrl();
    private static final RestJsonClientManager mgr = RestJsonClientManager.getInstance();
    
    private String serverUrl = DEFAULT_REMOTE_REST_URL;
    
    public SampleFacadeImpl() {
        super(log);
    }
    
    @Override
    public Collection<DataSet> getDataSets() throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("data").path("dataset");
            Response response = target
                    .queryParam("subjectKey", AccessManager.getSubjectKey())
                    .request("application/json")
                    .get();
            checkBadResponse(target, response);
            return response.readEntity(new GenericType<List<DataSet>>() {});
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("data").path("dataset");
            DomainQuery query = new DomainQuery();
            query.setSubjectKey(AccessManager.getSubjectKey());
            query.setDomainObject(dataSet);
            Response response = target
                    .request("application/json")
                    .put(Entity.json(query));
            checkBadResponse(target, response);
            return response.readEntity(DataSet.class);
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("data").path("dataset");
            DomainQuery query = new DomainQuery();
            query.setSubjectKey(AccessManager.getSubjectKey());
            query.setDomainObject(dataSet);
            Response response = target
                    .request("application/json")
                    .post(Entity.json(query));
            checkBadResponse(target, response);
            return response.readEntity(DataSet.class);
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("data").path("dataset");
            Response response = target
                    .queryParam("subjectKey", AccessManager.getSubjectKey())
                    .queryParam("dataSetId", dataSet.getId())
                    .request("application/json")
                    .delete();
            checkBadResponse(target, response);
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("data").path("sample").path("lsms");
            Response response = target
                    .queryParam("subjectKey", AccessManager.getSubjectKey())
                    .queryParam("sampleId", sampleId)
                    .request("application/json")
                    .get();
            checkBadResponse(target, response);
            return response.readEntity((new GenericType<List<LSMImage>>() {}));
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public List<LineRelease> getLineReleases() throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("process").path("release");
            Response response = target
                    .queryParam("subjectKey", AccessManager.getSubjectKey())
                    .request("application/json")
                    .get();
            checkBadResponse(target, response);
            return response.readEntity((new GenericType<List<LineRelease>>() {}));
        }
        finally {
            mgr.returnClient(client);
        }
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
        
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("process").path("release");
            Response response = target
                    .request("application/json")
                    .post(Entity.json(query));
            checkBadResponse(target, response);
            return response.readEntity(LineRelease.class);
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public LineRelease update(LineRelease release) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(release);
        query.setSubjectKey(AccessManager.getSubjectKey());

        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("process").path("release");
            Response response = target
                    .request("application/json")
                    .post(Entity.json(query));
            checkBadResponse(target, response);
            return response.readEntity(LineRelease.class);
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public void remove(LineRelease release) throws Exception {
        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("process").path("release");
            Response response = target
                    .queryParam("subjectKey", AccessManager.getSubjectKey())
                    .queryParam("releaseId", release.getId())
                    .request("application/json")
                    .delete();
            checkBadResponse(target, response);
        }
        finally {
            mgr.returnClient(client);
        }
    }

    @Override
    public String dispatchSamples(List<Reference> sampleRefs, String reprocessPurpose, boolean reuse) throws Exception {
        SampleDispatchRequest dispatchRequest = new SampleDispatchRequest();
        dispatchRequest.setProcessLabel(reprocessPurpose);
        dispatchRequest.setSampleReferences(sampleRefs);
        dispatchRequest.setReuse(reuse);

        RsClient client = mgr.borrowClient();
        try {
            WebTarget target = client.target(serverUrl).path("data").path("sample").path("dispatch");
            Response response = target
                    .queryParam("subjectKey", AccessManager.getSubjectKey())
                    .request("application/json")
                    .post(Entity.json(dispatchRequest));
            checkBadResponse(target, response);
            return response.readEntity(String.class);
        }
        finally {
            mgr.returnClient(client);
        }
    }
}
