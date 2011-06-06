package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/6/11
 * Time: 10:32 AM
 */
public class EntityMutableTreeNode extends DefaultMutableTreeNode {
    public EntityMutableTreeNode(Object o) {
        super(o);
    }

    public Long getEntityId() {
        return ((Entity)this.getUserObject()).getId();
    }

    @Override
    public String toString() {
        return ((Entity)this.getUserObject()).getName();
    }
}
