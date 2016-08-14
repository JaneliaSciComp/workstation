package org.janelia.it.workstation.gui.browser.api.facade.impl.mongo;

import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.OntologyFacade;

public class OntologyFacadeImpl implements OntologyFacade {

    private final DomainDAO dao;

    public OntologyFacadeImpl() throws Exception {
        this.dao = DomainDAOManager.getInstance().getDao();
    }

    @Override
    public Collection<Ontology> getOntologies() {
        return dao.getOntologies(AccessManager.getSubjectKey());
    }

    @Override
    public Ontology create(Ontology ontology) throws Exception {
        return (Ontology)updateIndex(dao.save(AccessManager.getSubjectKey(), ontology));
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        Ontology ontology = dao.getDomainObject(AccessManager.getSubjectKey(), Ontology.class, ontologyId);
        removeFromIndex(ontology.getId());
        dao.remove(AccessManager.getSubjectKey(), ontology);
    }

    @Override
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        return (Ontology)updateIndex(dao.addTerms(AccessManager.getSubjectKey(), ontologyId, parentTermId, terms, index));
    }

    @Override
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        return (Ontology)updateIndex(dao.removeTerm(AccessManager.getSubjectKey(), ontologyId, parentTermId, termId));
    }

    @Override
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        return (Ontology)updateIndex(dao.reorderTerms(AccessManager.getSubjectKey(), ontologyId, parentTermId, order));
    }

    @Override
    public List<Annotation> getAnnotations(Collection<Reference> references) {
        return dao.getAnnotations(AccessManager.getSubjectKey(), references);
    }

    @Override
    public Annotation createAnnotation(Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception {
        return dao.createAnnotation(AccessManager.getSubjectKey(), target, ontologyTermReference, value);
    }
    
    @Override
    public Annotation create(Annotation annotation) throws Exception {
        return (Annotation)updateIndex (dao.save(AccessManager.getSubjectKey(), annotation));
    }

    @Override
    public Annotation update(Annotation annotation) throws Exception {
        return (Annotation)updateIndex(dao.save(AccessManager.getSubjectKey(), annotation));
    }

    @Override
    public void remove(Annotation annotation) throws Exception {
        removeFromIndex(annotation.getId());
        dao.remove(AccessManager.getSubjectKey(), annotation);
    }

    private DomainObject updateIndex(DomainObject obj) throws Exception {
        ModelMgr.getModelMgr().updateIndex(obj);
        return obj;
    }

    private void removeFromIndex(Long domainObjId) throws Exception {
        ModelMgr.getModelMgr().removeFromIndex(domainObjId);
    }
}
