package org.janelia.it.workstation.gui.browser.api.facade.impl.mongo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SampleFacade;

public class SampleFacadeImpl implements SampleFacade {

    protected static final String MONGO_SERVER_URL = "dev-mongodb";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "";
    protected static final String MONGO_PASSWORD = "";

    private final DomainDAO dao;

    public SampleFacadeImpl() throws Exception {
        this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    }

    @Override
    public Collection<DataSet> getDataSets() {
        return dao.getDataSets(AccessManager.getSubjectKey());
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        return (DataSet) updateIndex(dao.save(AccessManager.getSubjectKey(), dataSet));
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        return (DataSet) updateIndex(dao.save(AccessManager.getSubjectKey(), dataSet));
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        removeFromIndex(dataSet.getId());
        dao.remove(AccessManager.getSubjectKey(), dataSet);
    }
    
    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) {
        return dao.getLsmsBySampleId(AccessManager.getSubjectKey(), sampleId);
    }

    @Override
    public List<LineRelease> getLineReleases() {
        return dao.getLineReleases(AccessManager.getSubjectKey());
    }

    @Override
    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        return dao.createLineRelease(name, releaseDate, lagTimeMonths, dataSets);
    }

    @Override
    public LineRelease update(LineRelease release) throws Exception {
        return (LineRelease) updateIndex(dao.save(AccessManager.getSubjectKey(), release));
    }

    @Override
    public void remove(LineRelease release) throws Exception {
        removeFromIndex(release.getId());
        dao.remove(AccessManager.getSubjectKey(), release);
    }
    
    private DomainObject updateIndex(DomainObject obj) throws Exception {
        ModelMgr.getModelMgr().updateIndex(obj);
        return obj;
    }

    private void removeFromIndex(Long domainObjId) throws Exception {
        ModelMgr.getModelMgr().removeFromIndex(domainObjId);
    }

}
