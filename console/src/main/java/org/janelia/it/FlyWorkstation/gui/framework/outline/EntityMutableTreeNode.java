package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/6/11
 * Time: 10:32 AM
 */
public class EntityMutableTreeNode extends DefaultMutableTreeNode implements Comparator<EntityMutableTreeNode>
{
    public EntityMutableTreeNode(Object o) {
        super(o);
    }

    public String getEntityName() {
        return ((Entity)this.getUserObject()).getName();
    }

    public Long getEntityId() {
        return ((Entity)this.getUserObject()).getId();
    }

    @Override
    public String toString() {
        return ((Entity)this.getUserObject()).getName();
    }

    @Override
    public int compare(EntityMutableTreeNode entityMutableTreeNode, EntityMutableTreeNode entityMutableTreeNode1) {
        return entityMutableTreeNode.getEntityName().compareTo(entityMutableTreeNode1.getEntityName());
    }
}
