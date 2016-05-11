package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;

public class DomainFacadeImpl extends RESTClientImpl implements DomainFacade {

    private RESTClientManager manager;
    
    public DomainFacadeImpl() {
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) {
        Collection<Long> ids = new ArrayList<>();
        ids.add(id);
        List<DomainObject> objList = getDomainObjects(domainClass.getName(), ids);
        if (objList!=null && objList.size()>0) {
            return (T)objList.get(0);
        }
        return null;
    }

    @Override
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) {
        Response response = manager.getDomainObjectEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("name", name)
                .queryParam("domainClass", domainClass.getName())
                .path("name")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDomainObject from server using name: " + name)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return (List<T>)domainObjs;
    }
    
    @Override
    public DomainObject getDomainObject(Reference reference) {
        List<Reference> refList = new ArrayList<>();
        refList.add(reference);
        List<DomainObject> domainObjList = getDomainObjects(refList);
        if (domainObjList!=null && domainObjList.size()>0) {
            return domainObjList.get(0);
        }
        return null;
    }

    @Override
    public List<DomainObject> getDomainObjects(List<Reference> refList) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(refList);
        Response response = manager.getDomainObjectEndpoint()
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request getDomainObject from server: " + refList)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return domainObjs;
    }
    
    @Override
    public List<DomainObject> getDomainObjects(ReverseReference reference) {
        Response response = manager.getDomainObjectEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("referenceId", reference.getReferenceId())
                .queryParam("referenceAttr", reference.getReferenceAttr())
                .queryParam("count", reference.getCount())
                .queryParam("referenceClass", reference.getReferringClassName())
                .path("reverseLookup")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDomainObject from server using reverser reference: " + reference)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return domainObjs;
    }


    @Override
    public List<DomainObject> getDomainObjects(String className, Collection<Long> ids) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setObjectType(className);
        query.setObjectIds(new ArrayList<>(ids));

        Response response = manager.getDomainObjectEndpoint()
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request getDomainObjects from server: " + ids)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return domainObjs;
    }

    @Override
    public List<DomainObject> getAllDomainObjectsByClass(String className) {
        DomainQuery query = new DomainQuery();
        // Not using a subject key: these are universal collections.
        query.setObjectType(className);
        query.setSubjectKey(AccessManager.getSubjectKey());

        Response response = manager.getDomainObjectEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("domainClass", className)
                .path("class")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getAllDomainObjectsByClass from server: " + className)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return domainObjs;
    }

    public DomainObject update(DomainObject domainObject) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(domainObject);

        Response response = manager.getDomainObjectEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request to update domainObject from server: " + domainObject.getId())) {
            return null;
        }
        DomainObject domainObj = response.readEntity(DomainObject.class);
        return domainObj;
    }

    @Override
    public DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) {
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
        Response response = manager.getDomainObjectEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateProperty from server: " + propName + "," + propValue)) {
            return null;
        }
        DomainObject domainObj = response.readEntity(DomainObject.class);
        return domainObj;
    }


    @Override
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("subjectKey", AccessManager.getSubjectKey());
        params.put("targetClass", domainObject.getClass().getName());
        params.put("targetId", domainObject.getId());
        params.put("granteeKey", granteeKey);
        params.put("rights", rights);
        params.put("grant", grant);
        Response response = manager.getUserEndpoint()
                .path("permissions")
                .request("application/json")
                .put(Entity.json(params));
        if (checkBadResponse(response.getStatus(), "problem making request changePermissions to server: " + domainObject + "," + granteeKey + "," + rights + "," + grant)) {
            return null;
        }
        return this.getDomainObject(Reference.createFor(domainObject));
    }

    @Override
    public void remove(List<Reference> deleteObjectRefs) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(deleteObjectRefs);
        Response response = manager.getDomainObjectEndpoint()
                .path("remove")
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(response.getStatus(), "problem making request to remove objectList: " + deleteObjectRefs);
    }
    
}
