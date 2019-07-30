package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.dto.SampleReprocessingRequest;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.LineRelease;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.facade.interfaces.SampleFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleFacadeImpl extends RESTClientBase implements SampleFacade {

    private static final Logger log = LoggerFactory.getLogger(SampleFacadeImpl.class);

    private WebTarget domainService;
    private WebTarget legacyDomainService;
    
    public SampleFacadeImpl() {
        this(ConsoleProperties.getInstance().getProperty("domain.facade.rest.url"), ConsoleProperties.getInstance().getProperty("domain.facade.rest.legacyUrl"));
    }

    private SampleFacadeImpl(String domainServiceURL, String legacyDomainServiceURL) {
        super(log);
        this.domainService = RestJsonClientManager.getInstance().getTarget(domainServiceURL, true);
        this.legacyDomainService = RestJsonClientManager.getInstance().getTarget(legacyDomainServiceURL, true);
    }
    
    @Override
    public Collection<DataSet> getDataSets() throws Exception {
        String currentPrincipal = AccessManager.getSubjectKey();
        WebTarget target = getDomainService("data/dataset")
                .queryParam("subjectKey", currentPrincipal);
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<DataSet>>() {});
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(dataSet);
        WebTarget target = getDomainService("data/dataset");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(DataSet.class);
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(dataSet);
        // TODO: this is a hack to allow admins to edit data sets. We need to implement a more comprehensive solution to this.
        if (AccessManager.getAccessManager().isAdmin()) {
            query.setSubjectKey(dataSet.getOwnerKey());
        } else {
            query.setSubjectKey(AccessManager.getSubjectKey());
        }
        WebTarget target = getDomainService("data/dataset");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(DataSet.class);
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        WebTarget target = getDomainService("data/dataset")
                .queryParam("dataSetId", dataSet.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) throws Exception {
        WebTarget target = getDomainService("data/sample/lsms")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("sampleId", sampleId);
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity((new GenericType<List<LSMImage>>() {}));
    }

    @Override
    public List<LineRelease> getLineReleases() throws Exception {
        String currentPrincipal = AccessManager.getSubjectKey();
        WebTarget target = getDomainService("process/release")
                .queryParam("subjectKey", currentPrincipal);
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<LineRelease>>() {})
                .stream()
                .sorted(new DomainObjectComparator(currentPrincipal))
                .collect(Collectors.toList());
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
        WebTarget target = getDomainService("process/release");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(LineRelease.class);
    }

    @Override
    public LineRelease update(LineRelease release) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(release);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = getDomainService("process/release");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(LineRelease.class);
    }

    @Override
    public void remove(LineRelease release) throws Exception {
        WebTarget target = getDomainService("process/release")
                .queryParam("releaseId", release.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

    @Override
    public String dispatchSamples(SampleReprocessingRequest request) throws Exception {
        WebTarget target = getLegacyDomainService("process/sample/reprocess")
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .post(Entity.json(request));
        checkBadResponse(target, response);
        return response.readEntity(String.class);
    }

    @Override
    public Long dispatchTask(JsonTask task, String processName) throws Exception {
        WebTarget target = getLegacyDomainService("process/dispatch")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("processName", processName);
        Response response = target
                .request("application/json")
                .post(Entity.json(task));
        checkBadResponse(target, response);
        return response.readEntity(Long.class);
    }

    @Override
    public Collection<ColorDepthLibrary> getColorDepthLibraries(String alignmentSpace) throws Exception {
        String currentPrincipal = AccessManager.getSubjectKey();
        WebTarget target = getDomainService("data/dataset/colordepth")
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        if (alignmentSpace!=null) {
            target = target.queryParam("alignmentSpace", alignmentSpace);
        }
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<ColorDepthLibrary>>() {});
    }

    private WebTarget getDomainService(String path) {
        return domainService.path(path);
    }

    private WebTarget getLegacyDomainService(String path) {
        return legacyDomainService.path(path);
    }
}
