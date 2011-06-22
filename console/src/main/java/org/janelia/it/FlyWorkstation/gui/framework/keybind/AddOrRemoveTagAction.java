/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:40 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * This action adds or removes an entity tag from the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddOrRemoveTagAction extends EntityAction {

    public AddOrRemoveTagAction(Entity entity) {
        super(entity);
    }

    @Override
    public void doAction() {
        ConsoleApp.getMainFrame().getOntologyOutline().navigateToEntityNode(getEntity());
        ConsoleApp.getMainFrame().getViewerPanel().addOrRemoveTag(getName());
    }

}
