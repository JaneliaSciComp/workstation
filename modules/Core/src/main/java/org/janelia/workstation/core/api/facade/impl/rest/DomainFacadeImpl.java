package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ReverseReference;
import org.janelia.model.domain.report.DatabaseSummary;
import org.janelia.model.domain.report.DiskUsageSummary;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.facade.interfaces.DomainFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainFacadeImpl extends RESTClientBase implements DomainFacade {

    private static final Logger log = LoggerFactory.getLogger(DomainFacadeImpl.class);

    private WebTarget service;

    public DomainFacadeImpl(String serverUrl) {
        super(log);
        log.info("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    @Override
    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) throws Exception {
        Collection<Long> ids = new ArrayList<>();
        ids.add(id);
        List<T> objList = getDomainObjects(domainClass.getName(), ids);
        if (objList!=null && objList.size()>0) {
            return objList.get(0);
        }
        return null;
    }

    @Override
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) throws Exception {
        WebTarget target = service.path("data/domainobject/name")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("name", name)
                .queryParam("domainClass", domainClass.getName());
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<T>>() {});
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainObject> T getDomainObject(Reference reference) throws Exception {
        List<Reference> refList = new ArrayList<>();
        refList.add(reference);
        List<DomainObject> domainObjList = getDomainObjects(refList);
        if (domainObjList!=null && domainObjList.size()>0) {
            return (T)domainObjList.get(0);
        }
        return null;
    }

    @Override
    public List<DomainObject> getDomainObjects(List<Reference> refList) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(refList);
        WebTarget target = service.path("data/domainobject/details");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<DomainObject>>() {});
    }
    
    @Override
    public List<DomainObject> getDomainObjects(ReverseReference reference) throws Exception {
        WebTarget target = service.path("data/domainobject/reverseLookup")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("referenceId", reference.getReferenceId())
                .queryParam("referenceAttr", reference.getReferenceAttr())
                .queryParam("count", reference.getCount())
                .queryParam("referenceClass", reference.getReferringClassName());
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<DomainObject>>() {});
    }


    @Override
    public <T extends DomainObject> List<T> getDomainObjects(String className, Collection<Long> ids) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setObjectType(className);
        query.setObjectIds(new ArrayList<>(ids));
        WebTarget target = service.path("data/domainobject/details");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<T>>() {});
    }

    @Override
    public List<DomainObject> getAllDomainObjectsByClass(String className) throws Exception {
        DomainQuery query = new DomainQuery();
        // Not using a subject key: these are universal collections.
        query.setObjectType(className);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = service.path("data/domainobject/class");
        Response response = target
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("domainClass", className)
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<DomainObject>>() {});
    }

    public DomainObject save(DomainObject domainObject) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(domainObject);
        WebTarget target = service.path("data/domainobject");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(DomainObject.class);
    }

    @Override
    public DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setObjectType(domainObject.getClass().getName());
        List<Long> objectIdList = new ArrayList<>();
        objectIdList.add(domainObject.getId());
        query.setObjectIds(objectIdList);
        query.setPropertyName(propName);
        
        // TODO: This needs to take an object supporting the basic types (String, Boolean, Integer, etc):
        if (!(propValue instanceof String)) throw new UnsupportedOperationException("This method needs to be fixed to support non-strings");
        
        query.setPropertyValue(propValue.toString());
        WebTarget target = service.path("data/domainobject");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(DomainObject.class);
    }

    @Override
    public DomainObject setPermissions(DomainObject domainObject, String granteeKey, String rights) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("subjectKey", AccessManager.getSubjectKey());
        params.put("targetClass", domainObject.getClass().getName());
        params.put("targetId", domainObject.getId());
        params.put("granteeKey", granteeKey);
        params.put("rights", rights);
        WebTarget target = service.path("data/user/permissions");
        Response response = target
                .request("application/json")
                .put(Entity.json(params));
        checkBadResponse(target, response);
        return getDomainObject(Reference.createFor(domainObject));
    }

    @Override
    public void remove(List<Reference> deleteObjectRefs) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(deleteObjectRefs);
        WebTarget target = service.path("data/domainobject/remove");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
    }

    @Override
    public void removeObjectStorage(List<String> storagePaths) {
        String remoteStorageUrl = ConsoleProperties.getInstance().getProperty("jadestorage.rest.url");
        WebTarget storageService = RestJsonClientManager.getInstance().getTarget(remoteStorageUrl, true);
        for (String storagePath : storagePaths) {
            WebTarget target = storageService.path("storage_content/storage_path_redirect")
                    .path(storagePath);
            Response response = target
                    .request("application/json")
                    .delete();

            // check and log but don't fail
            int responseStatus = response.getStatus();
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (responseStatus<200 || responseStatus>=300) {
                log.error("Request for {} returned {} {}", target.getUri(), responseStatus, status);
            }
        }
    }

    @Override
    public DatabaseSummary getDatabaseSummary() throws Exception {
        WebTarget target = service.path("data/summary/database")
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(DatabaseSummary.class);
    }
    
    @Override
    public DiskUsageSummary getDiskUsageSummary() throws Exception {
        WebTarget target = service.path("data/summary/disk")
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(DiskUsageSummary.class);
    }
}
