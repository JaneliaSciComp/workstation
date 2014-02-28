package org.janelia.it.FlyWorkstation.nb_action;

import java.util.List;
import javax.swing.JComponent;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

/**
 * Implement this from another module to claim your class can accept drops
 * in DnD gesture.
 * 
 * @author fosterl
 */
public interface DropAcceptor extends Compatible<JComponent> {
    public final static String LOOKUP_PATH = "EntityPerspective/DropTarget";
    void drop(List<RootedEntity> entitiesToAdd);
}
