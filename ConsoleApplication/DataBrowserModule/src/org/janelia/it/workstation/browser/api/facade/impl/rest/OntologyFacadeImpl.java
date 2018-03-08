package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.browser.api.http.RESTClientBase;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyFacadeImpl extends RESTClientBase implements OntologyFacade {

    private static final Logger log = LoggerFactory.getLogger(OntologyFacadeImpl.class);
    
    private static final String REMOTE_API_URL = ConsoleApp.getConsoleApp().getRemoteRestUrl();
    
    private WebTarget service;
    
    public OntologyFacadeImpl() {
        this(REMOTE_API_URL);
    }

    public OntologyFacadeImpl(String serverUrl) {
        super(log);
        log.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    @Override
    public Collection<Ontology> getOntologies() {
        Response response = service.path("data/ontology")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getOntologies from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Ontology>>() {});
    }

    @Override
    public Ontology create(Ontology ontology) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(ontology);
        Response response = service.path("data/ontology")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createOntology from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Ontology.class);
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        Response response = service.path("data/ontology")
                .queryParam("ontologyId", ontologyId)
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        checkBadResponse(response.getStatus(), "problem making request removeOntology to server: " + ontologyId);
    }

    @Override
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        List<Long> objectIds = new ArrayList<>();
        objectIds.add(ontologyId);
        objectIds.add(parentTermId);
        query.setObjectIds(objectIds);
        query.setObjectList(new ArrayList<>(terms));
        List<Integer> ordering = new ArrayList<>();
        ordering.add(index);
        query.setOrdering(ordering);
        Response response = service.path("data/ontology")
                .path("terms")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request addOntologyTerms to server: " + ontologyId + "," + parentTermId + "," + terms)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Ontology.class);
    }

    @Override
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        Response response = service.path("data/ontology")
                .path("terms")
                .queryParam("ontologyId", ontologyId)
                .queryParam("parentTermId", parentTermId)
                .queryParam("termId", termId)
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeOntologyTerms to server: " + ontologyId + "," + parentTermId + "," + termId)) {
            throw new WebApplicationException(response);
        }
        Ontology newOntology = response.readEntity(Ontology.class);
        return newOntology;
    }

    @Override
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        List<Long> objectIds = new ArrayList<>();
        objectIds.add(ontologyId);
        objectIds.add(parentTermId);
        query.setObjectIds(objectIds);
        List<Integer> orderList = new ArrayList<>();
        for (int i=0; i<order.length; i++) {
            orderList.add(new Integer(order[i]));
        }
        query.setOrdering(orderList);
        Response response = service.path("data/ontology")
                .path("terms")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request reorderOntologyTerms to server: " + ontologyId + "," + parentTermId + "," + order)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Ontology.class);
    }
    
    @Override
    public List<Annotation> getAnnotations(Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(new ArrayList<>(references));

        Response response = service.path("data/annotation")
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request getAnnotations from server: " + references)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Annotation>>() {});
    }
    
    @Override
    public Annotation createAnnotation(Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception {
        // TODO: implement
        throw new UnsupportedOperationException("This is not yet implemented in the web service");
    }
    
    @Override
    public Annotation create(Annotation annotation) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(annotation);
        Response response = service.path("data/annotation")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createAnnotation from server: " + annotation)) {
            throw new WebApplicationException(response);
        }
        Annotation newAnnotation = response.readEntity(Annotation.class);
        return newAnnotation;
    }

    @Override
    public Annotation update(Annotation annotation) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(annotation);
        Response response = service.path("data/annotation")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateAnnotation from server: " + annotation)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Annotation.class);
    }

    @Override
    public void remove(Annotation annotation) throws Exception {
        Response response = service.path("data/annotation")
                .queryParam("annotationId", annotation.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeAnnotation from server: " + annotation)) {
            throw new WebApplicationException(response);
        }
    }

}
