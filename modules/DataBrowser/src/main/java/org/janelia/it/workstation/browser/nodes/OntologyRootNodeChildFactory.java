package org.janelia.it.workstation.browser.nodes;

import java.util.List;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.model.domain.ontology.Ontology;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main child factory for the root node in the ontology explorer.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyRootNodeChildFactory extends ChildFactory<Ontology> {

    private static final Logger log = LoggerFactory.getLogger(OntologyRootNodeChildFactory.class);
    
    @Override
    protected boolean createKeys(List<Ontology> list) {
        try {
            Long currOntologyId = StateMgr.getStateMgr().getCurrentOntologyId();
            if (currOntologyId==null) return true;
            
            for(Ontology ontology : DomainMgr.getDomainMgr().getModel().getOntologies()) {
                if (ontology.getId().equals(currOntologyId)) {
                    log.info("Adding currently selected ontology node: {}", currOntologyId);
                    list.add(ontology);
                    return true;
                }
            }
            
            return true;
        } 
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(Ontology key) {
        try {
            return new OntologyNode(key);
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
        return null;
    }

    public void refresh() {
        refresh(true);
    }
}
