package org.janelia.it.jacs.integration.framework.domain;

import java.util.List;
import javax.swing.JComponent;

import org.janelia.model.domain.DomainObject;

/**
 * Implement this from another module to claim your class can accept drops
 * in DnD gesture.
 * 
 * @author fosterl
 */
public interface DropAcceptor extends Compatible<JComponent> {
    public final static String LOOKUP_PATH = "EntityPerspective/DropTarget";
    void drop(List<DomainObject> objectToAdd, String objective);
}
