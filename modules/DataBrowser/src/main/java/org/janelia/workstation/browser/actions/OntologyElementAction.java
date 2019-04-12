package org.janelia.workstation.browser.actions;

import org.janelia.workstation.browser.gui.components.OntologyExplorerTopComponent;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.core.actions.Action;

/**
 * The default action for executing the binding on an ontology element.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElementAction implements Action {

    private final OntologyTerm ontologyTerm;
    
    public OntologyElementAction(OntologyTerm ontologyTerm) {
        this.ontologyTerm = ontologyTerm;
    }

    @Override
    public String getName() {
        return ontologyTerm.getName();
    }

    public OntologyTerm getOntologyTerm() {
        return ontologyTerm;
    }

    public Long getOntologyTermId() {
        return ontologyTerm.getId();
    }
    
    @Override
    public void doAction() {
        OntologyExplorerTopComponent explorer = OntologyExplorerTopComponent.getInstance();
        explorer.executeBinding(ontologyTerm.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OntologyElementAction) {
            OntologyElementAction other = (OntologyElementAction)o;
            return getOntologyTermId().equals(other.getOntologyTermId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ontologyTerm.getId().hashCode();
    }

    @Override
    public String toString() {
        return "OntologyElementAction[" + ontologyTerm.getId() + ']';
    }
}
