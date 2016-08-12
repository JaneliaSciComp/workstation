package org.janelia.it.workstation.gui.browser.nodes;

import java.util.List;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A child factory for ontology nodes (i.e. terms). Supports adding and removing 
 * children dynamically. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyChildFactory extends ChildFactory<OntologyTerm> {

    private final static Logger log = LoggerFactory.getLogger(OntologyChildFactory.class);
    
    private final Ontology ontology;
    private final OntologyTerm ontologyTerm;
    
    OntologyChildFactory(Ontology ontology, OntologyTerm ontologyTerm) {
        this.ontology = ontology;
        this.ontologyTerm = ontologyTerm;
    }

    public boolean hasNodeChildren() {
        return ontologyTerm.hasChildren();
    }

    @Override
    protected boolean createKeys(List<OntologyTerm> list) {
        if (ontologyTerm==null) return false;
        log.trace("Creating children keys for {}",ontologyTerm.getName());   
        if (ontologyTerm.getTerms()!=null) {
            for(OntologyTerm term : ontologyTerm.getTerms()) {
                if (term==null) continue;
                list.add(term);
            }
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(OntologyTerm key) {
        if (ontology==null) return null;
        log.debug("Creating node for {}",key.getName());
        try {
            return new OntologyTermNode(this, ontology, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
    
    public void refresh() {
        log.debug("Refreshing child factory for: {}",ontologyTerm.getName());
        refresh(true);
    }

    public void addChildren(List<OntologyTerm> childTerms) throws Exception {
        if (ontologyTerm==null) {
            log.warn("Cannot add child to unloaded treeNode");
            return;
        }   

        for(OntologyTerm childTerm : childTerms) {
            log.info("Adding child '{}' to '{}'",childTerm.getName(),ontologyTerm.getName());
        }
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.addOntologyTerms(ontology.getId(), ontologyTerm.getId(), childTerms);
    }
    
    public void addChildren(List<OntologyTerm> childTerms, int index) throws Exception {
        if (ontologyTerm==null) {
            log.warn("Cannot add child to unloaded treeNode");
            return;
        }   

        int i = 0;
        for(OntologyTerm childTerm : childTerms) {
            log.info("Adding child '{}' to '{}' at {}",childTerm.getName(),ontologyTerm.getName(),index+i);
            i++;
        }
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.addOntologyTerms(ontology.getId(), ontologyTerm.getId(), childTerms, index);
    }
    
    public void removeChild(final OntologyTerm childTerm) throws Exception {
        if (ontologyTerm==null) {
            log.warn("Cannot remove child from unloaded treeNode");
            return;
        }

        log.info("Removing child '{}' from '{}'", childTerm.getName(), ontologyTerm.getName());

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.removeOntologyTerm(ontology.getId(), ontologyTerm.getId(), childTerm.getId());
    }
}
