package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.ontology.Ontology;

/**
 * An ontology was selected by the user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologySelectionEvent {

    private final Long ontologyId;

    public OntologySelectionEvent(Long ontologyId) {
        this.ontologyId = ontologyId;
    }
   
    public Long getOntologyId() {
        return ontologyId;
    }
    
    @Override
    public String toString() {
        return "OntologySelectionEvent[" + "id=" + ontologyId + ']';
    }
}
