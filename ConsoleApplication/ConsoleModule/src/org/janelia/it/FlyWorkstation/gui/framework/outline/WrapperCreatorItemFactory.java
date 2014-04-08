/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.nb_action.EntityWrapperCreator;
import org.janelia.it.FlyWorkstation.nb_action.ServiceAcceptorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience delegate to make Menu Items for creating entity wrappers.
 * 
 * @author fosterl
 */
public class WrapperCreatorItemFactory {
    private Logger log = LoggerFactory.getLogger( WrapperCreatorItemFactory.class );    

    /**
     * This allows an abstraction layer between making the action, and the thing
     * that is being carried out.
     * 
     * @param rootedEntity build another item around this; may be null.
     * @return the menu item suitable for add to menu.
     */
    public JMenuItem makeEntityWrapperCreatorItem(final RootedEntity rootedEntity) {
        JMenuItem wrapEntityItem = null;
        final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
        EntityWrapperCreator wrapperCreator
                = helper.findHandler(rootedEntity, EntityWrapperCreator.class, EntityWrapperCreator.LOOKUP_PATH);
        if (wrapperCreator != null) {
            wrapEntityItem = new JMenuItem(wrapperCreator.getActionLabel());
            wrapEntityItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        EntityWrapperCreator wrapperCreator
                                = helper.findHandler(rootedEntity, EntityWrapperCreator.class, EntityWrapperCreator.LOOKUP_PATH);
                        if (wrapperCreator == null) {
                            log.warn("No service provider for this entity.");
                        } else {
                            wrapperCreator.wrapEntity(rootedEntity);
                        }
                    } catch (Exception ex) {
                        ModelMgr.getModelMgr().handleException(ex);
                    }

                }
            });

        }
        return wrapEntityItem;
    }

}
