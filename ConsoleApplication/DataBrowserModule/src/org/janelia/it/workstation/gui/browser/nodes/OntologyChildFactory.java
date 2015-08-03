package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyChildFactory extends ChildFactory<OntologyTerm> {

    private final static Logger log = LoggerFactory.getLogger(OntologyChildFactory.class);
    
    private final WeakReference<Ontology> ontologyRef;
    private final WeakReference<OntologyTerm> ontologyTermRef;
    
    public OntologyChildFactory(Ontology ontology, OntologyTerm ontologyTerm) {
        this.ontologyRef = new WeakReference<Ontology>(ontology);
        this.ontologyTermRef = new WeakReference<OntologyTerm>(ontologyTerm);
    }
    
    @Override
    protected boolean createKeys(List<OntologyTerm> list) {
        OntologyTerm ontologyTerm = ontologyTermRef.get();
        if (ontologyTerm==null) return false;
        log.trace("Creating children keys for {}",ontologyTerm.getName());   
        if (ontologyTerm.getTerms()!=null) {
            for(OntologyTerm term : ontologyTerm.getTerms()) {
                list.add(term);
            }
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(OntologyTerm key) {
        final Ontology ontology = ontologyRef.get();
        if (ontology==null) return null;
        try {
            return new OntologyTermNode(ontology, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
    
    public void refresh() {
        refresh(true);
    }
    
    public void addChild(final DomainObject domainObject) {
        final Ontology ontology = ontologyRef.get();
        if (ontology==null) return;
        final OntologyTerm ontologyTerm = ontologyTermRef.get();
        if (ontologyTerm==null) return;
        
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
//                log.warn("adding child {} to {}",domainObject.getId(),treeNode.getName());
//                DomainDAO dao = DomainExplorerTopComponent.getDao();
//                dao.addChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
            }
            @Override
            protected void hadSuccess() {
                log.info("refreshing view after adding child");
                refresh();
            }
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

    public void removeChild(final DomainObject domainObject) {
        final Ontology ontology = ontologyRef.get();
        if (ontology==null) return;
        final OntologyTerm ontologyTerm = ontologyTermRef.get();
        if (ontologyTerm==null) return;
        
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
//                log.warn("removing child {} from {}",domainObject.getId(),treeNode.getName());
//                DomainDAO dao = DomainExplorerTopComponent.getDao();
//                if (domainObject instanceof DeadReference) {
//                    dao.removeReference(SessionMgr.getSubjectKey(), treeNode, ((DeadReference)domainObject).getReference());
//                }
//                else {
//                    dao.removeChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
//                }
            }
            @Override
            protected void hadSuccess() {
                log.info("refreshing view after removing child");
                refresh();
            }
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }


}
