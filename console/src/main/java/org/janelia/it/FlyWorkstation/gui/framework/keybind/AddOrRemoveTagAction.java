/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:40 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * This action adds or removes an entity tag from the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddOrRemoveTagAction implements Action {

    private Entity entity;

    public AddOrRemoveTagAction(Entity entity) {
        this.entity = entity;
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void doAction() {

        System.out.println("Tagging with "+getName());
        // TODO: set tag on the currently selected image


    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddOrRemoveTagAction that = (AddOrRemoveTagAction) o;
        if (entity != null ? !entity.getId().equals(that.entity.getId()) : that.entity != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return entity != null ? entity.hashCode() : 0;
    }
}
