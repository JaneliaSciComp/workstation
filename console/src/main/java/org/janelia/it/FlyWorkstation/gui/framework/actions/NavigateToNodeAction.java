package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * This action expands or collapses the corresponding entity node in the ontology tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NavigateToNodeAction extends OntologyElementAction {

    @Override
    public void doAction() {
        final OntologyOutline ontologyOutline = SessionMgr.getBrowser().getOntologyOutline();
        
        OntologyElement element = getOntologyElement();
        if (element==null) {
            EntityData entityTermEd = ontologyOutline.getEntityDataByUniqueId(getUniqueId());
            element = ontologyOutline.getOntologyElement(entityTermEd);
        }
        
        ontologyOutline.navigateToOntologyElement(element);
    }
}
