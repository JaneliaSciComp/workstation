package org.janelia.it.FlyWorkstation.gui.framework.outline;

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

    public ActionableEntity getEntityNode() {
        return (ActionableEntity)this.getUserObject();
    }

    public String getEntityName() {
        return ((ActionableEntity)this.getUserObject()).getEntity().getName();
    }

    public Long getEntityId() {
        return ((ActionableEntity)this.getUserObject()).getEntity().getId();
    }

    @Override
    public String toString() {
        return ((ActionableEntity)this.getUserObject()).getEntity().getName();
    }

    @Override
    public int compare(EntityMutableTreeNode entityMutableTreeNode, EntityMutableTreeNode entityMutableTreeNode1) {
        return entityMutableTreeNode.getEntityName().compareTo(entityMutableTreeNode1.getEntityName());
    }
}
