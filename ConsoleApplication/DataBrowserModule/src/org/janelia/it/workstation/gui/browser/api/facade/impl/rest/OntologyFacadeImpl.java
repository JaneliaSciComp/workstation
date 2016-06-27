package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.OntologyFacade;

public class OntologyFacadeImpl extends RESTClientImpl implements OntologyFacade {

    private RESTClientManager manager;
    
    public OntologyFacadeImpl() {
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public Collection<Ontology> getOntologies() {
        Response response = manager.getOntologyEndpoint()
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
        Response response = manager.getOntologyEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createOntology from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Ontology.class);
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        Response response = manager.getOntologyEndpoint()
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
        Response response = manager.getOntologyEndpoint()
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
        Response response = manager.getOntologyEndpoint()
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
        Response response = manager.getOntologyEndpoint()
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

        Response response = manager.getAnnotationEndpoint()
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
        Response response = manager.getAnnotationEndpoint()
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
        Response response = manager.getAnnotationEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateAnnotation from server: " + annotation)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Annotation.class);
    }

    @Override
    public void remove(Annotation annotation) throws Exception {
        Response response = manager.getAnnotationEndpoint()
                .queryParam("annotationId", annotation.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeAnnotation from server: " + annotation)) {
            throw new WebApplicationException(response);
        }
    }

}
