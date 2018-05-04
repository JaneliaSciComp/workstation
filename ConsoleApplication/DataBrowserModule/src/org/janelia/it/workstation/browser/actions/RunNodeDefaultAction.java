package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import javax.swing.Action;

import org.janelia.it.workstation.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.OntologyTermNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action expands or collapses the corresponding entity node in the ontology tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunNodeDefaultAction extends OntologyElementAction {

    private static final Logger log = LoggerFactory.getLogger(RunNodeDefaultAction.class);
    
    @Override
    public void doAction() {
        
        Long[] path = getPath();
        OntologyExplorerTopComponent explorer = OntologyExplorerTopComponent.getInstance();
        OntologyTermNode node = explorer.select(path);
        if (node==null) {
            throw new IllegalStateException("Could not find ontology term with path: "+NodeUtils.createPathString(path));
        }
        
        Action a = node.getPreferredAction();
        log.debug("Executing default node action for: {}",getUniqueId());

        if (a != null) {
            log.trace("Executing preferred action for: {}",getUniqueId());
            a.actionPerformed(new ActionEvent(node, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    @Override
    public String toString() {
        return "RunNodeDefaultAction{" + getUniqueId() + '}';
    }
}
