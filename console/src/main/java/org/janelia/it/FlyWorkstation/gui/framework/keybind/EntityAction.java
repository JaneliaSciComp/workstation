/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 1:35 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An abstract base class for actions dealing with entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityAction implements Action {

    private Entity entity;

    public EntityAction(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public abstract void doAction();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityAction)) return false;
        EntityAction that = (EntityAction) o;
        if (!entity.equals(that.entity)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
