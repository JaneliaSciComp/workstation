/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 1:31 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

/**
 * This action expands or collapses the corresponding entity node in the ontology tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NavigateToNodeAction extends OntologyElementAction {

    @Override
    public void doAction() {
        SessionMgr.getSessionMgr().getActiveBrowser().getOntologyOutline().navigateToOntologyElement(getOntologyElement());
    }
}
