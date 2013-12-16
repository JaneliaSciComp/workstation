package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/16/13
 * Time: 1:49 PM
 *
 * Can implement this with anything that can accept rooted entities, and hence remove implementation detail from
 * client of this interface.
 */
public interface RootedEntityReceiver {
    void setRootedEntities( Collection<RootedEntity> rootedEntities );
}
