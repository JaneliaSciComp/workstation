package org.janelia.it.workstation.gui.browser.actions;

import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.framework.actions.Action;

/**
 * An abstract base class for actions dealing with ontology elements.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyElementAction implements Action {

    private Long[] path;
    private String uniqueId;
    
    public void init(Long[] path) {
        this.path = path;
        this.uniqueId = NodeUtils.createPathString(path);
    }

    public Long[] getPath() {
        return path;
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
