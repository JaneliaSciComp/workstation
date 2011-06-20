/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/16/11
 * Time: 12:37 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.keybind.Action;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.AddOrRemoveTagAction;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Associates an action with an entity. By default, the action will be to add/remove a tag corresponding to the
 * entity from the currently selected item.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ActionableEntity {

    private Entity entity;
    private Action action;

    public ActionableEntity(Entity entity) {
        this.entity = entity;
        setAction(new AddOrRemoveTagAction(entity));
    }

    public String getName() {
        return entity.getName();
    }

    public Entity getEntity() {
        return entity;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
