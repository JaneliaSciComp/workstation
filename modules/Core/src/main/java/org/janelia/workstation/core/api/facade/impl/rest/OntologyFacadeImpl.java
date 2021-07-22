package org.janelia.workstation.core.api.facade.impl.rest;

import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.dto.DomainQuery;
import org.janelia.model.domain.dto.annotation.CreateAnnotationParams;
import org.janelia.model.domain.dto.annotation.RemoveAnnotationParams;
import org.janelia.model.domain.dto.annotation.UpdateAnnotationParams;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.facade.interfaces.OntologyFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    public Ontology create(Ontology ontology) {
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
    public void removeOntology(Long ontologyId) {
        WebTarget target = service.path("data/ontology")
                .queryParam("ontologyId", ontologyId)
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

    @Override
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) {
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
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) {
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
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) {
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
    public List<Annotation> getAnnotations(Collection<Reference> references) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(new ArrayList<>(references));
        WebTarget target = service.path("data/annotations/query");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<Annotation>>() {});
    }

    @Override
    public Annotation createAnnotation(Reference targetObject, OntologyTermReference ontologyTermReference, String value) {
        CreateAnnotationParams params = new CreateAnnotationParams();
        params.setSubjectKey(AccessManager.getSubjectKey());
        params.setOntologyTermReference(ontologyTermReference);
        params.setTarget(targetObject);
        params.setValue(value);
        WebTarget target = service.path("data/annotations");
        Response response = target
                .request("application/json")
                .put(Entity.json(params));
        checkBadResponse(target, response);
        return response.readEntity(Annotation.class);
    }

    @Override
    public Annotation updateAnnotation(Annotation annotation, String newValue) {
        UpdateAnnotationParams params = new UpdateAnnotationParams();
        params.setSubjectKey(AccessManager.getSubjectKey());
        params.setAnnotationId(annotation.getId());
        params.setValue(newValue);
        WebTarget target = service.path("data/annotations/value");
        Response response = target
                .request("application/json")
                .post(Entity.json(params));
        checkBadResponse(target, response);
        return response.readEntity(Annotation.class);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        RemoveAnnotationParams params = new RemoveAnnotationParams();
        params.setSubjectKey(AccessManager.getSubjectKey());
        params.setAnnotationId(annotation.getId());
        WebTarget target = service.path("data/annotations/remove")
                .queryParam("annotationId", annotation.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey());
        Response response = target
                .request("application/json")
                .put(Entity.json(params));
        checkBadResponse(target, response);
    }

}
