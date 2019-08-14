package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.facade.interfaces.OntologyFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyFacadeImpl extends RESTClientBase implements OntologyFacade {

    private static final Logger log = LoggerFactory.getLogger(OntologyFacadeImpl.class);

    private WebTarget service;
    
    public OntologyFacadeImpl(String serverUrl) {
        super(log);
        log.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }

    @Override
    public List<Ontology> getOntologies() {
        String currentPrincipal = AccessManager.getSubjectKey();
        WebTarget target = service.path("data/ontology")
                .queryParam("subjectKey", currentPrincipal);
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<Ontology>>() {})
                .stream()
                .sorted(new DomainObjectComparator(currentPrincipal))
                .collect(Collectors.toList());
    }

    @Override
    public Ontology create(Ontology ontology) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(ontology);
        WebTarget target = service.path("data/ontology");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Ontology.class);
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        WebTarget target = service.path("data/ontology")
                .queryParam("ontologyId", ontologyId)
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
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
        WebTarget target = service.path("data/ontology/terms");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Ontology.class);
    }

    @Override
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        WebTarget target = service.path("data/ontology/terms")
                .queryParam("ontologyId", ontologyId)
                .queryParam("parentTermId", parentTermId)
                .queryParam("termId", termId)
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
        return response.readEntity(Ontology.class);
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
        for (int i : order) {
            orderList.add(i);
        }
        query.setOrdering(orderList);
        WebTarget target = service.path("data/ontology/terms");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Ontology.class);
    }
    
    @Override
    public List<Annotation> getAnnotations(Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(new ArrayList<>(references));
        WebTarget target = service.path("data/annotation/details");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
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
        WebTarget target = service.path("data/annotation");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Annotation.class);
    }

    @Override
    public Annotation update(Annotation annotation) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(annotation);
        WebTarget target = service.path("data/annotation");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Annotation.class);
    }

    @Override
    public void remove(Annotation annotation) throws Exception {
        WebTarget target = service.path("data/annotation")
                .queryParam("annotationId", annotation.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

}
