/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 1:31 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;

/**
 * This action expands or collapses the corresponding entity node in the ontology tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NavigateToNodeAction extends OntologyElementAction {

    @Override
    public void doAction() {
        ConsoleApp.getMainFrame().getOntologyOutline().navigateToOntologyElement(getOntologyElement());
    }
}
