package org.janelia.it.workstation.gui.framework.actions;

import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * An abstract base class for actions dealing with ontology elements.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyElementAction implements Action {

    private OntologyElement element;
    private String uniqueId;

    public void init(OntologyElement element) {
        this.element = element;
    }
    
    public void init(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public OntologyElement getOntologyElement() {
        return element;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getName() {
        return uniqueId;
    }

    @Override
    public abstract void doAction();

    @Override
    public boolean equals(Object o) {
        if (o instanceof OntologyElementAction) {
            OntologyElementAction other = (OntologyElementAction)o;
            return uniqueId.equals(other.getUniqueId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uniqueId.hashCode();
    }
}
