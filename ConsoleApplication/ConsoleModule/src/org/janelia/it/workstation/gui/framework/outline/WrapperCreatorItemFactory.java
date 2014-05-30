/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.outline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.JMenuItem;

import org.janelia.it.workstation.nb_action.EntityWrapperCreator;
import org.janelia.it.workstation.nb_action.ServiceAcceptorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience delegate to make Menu Items for creating entity wrappers.
 * 
 * @author fosterl
 */
public class WrapperCreatorItemFactory {
    
    private static final Logger log = LoggerFactory.getLogger( WrapperCreatorItemFactory.class );    

    /**
     * This allows an abstraction layer between making the action, and the thing
     * that is being carried out.
     * 
     * @param rootedEntity build another item around this; may be null.
     * @return the menu item suitable for add to menu.
     */
    public JMenuItem makeEntityWrapperCreatorItem(final org.janelia.it.workstation.model.entity.RootedEntity rootedEntity) {
        JMenuItem wrapEntityItem = null;
        final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
        Collection<EntityWrapperCreator> wrapperCreators
                = helper.findHandler(rootedEntity, EntityWrapperCreator.class, EntityWrapperCreator.LOOKUP_PATH);
        for ( EntityWrapperCreator wrapperCreator: wrapperCreators ) {
            wrapEntityItem = new JMenuItem(wrapperCreator.getActionLabel());
            wrapEntityItem.addActionListener(new WrapEntityActionListener(wrapperCreator, rootedEntity));
        }
        return wrapEntityItem;
    }
    
    class WrapEntityActionListener implements ActionListener {
        private EntityWrapperCreator wrapperCreator;
        private org.janelia.it.workstation.model.entity.RootedEntity rootedEntity;
        public WrapEntityActionListener( EntityWrapperCreator wrapperCreator, org.janelia.it.workstation.model.entity.RootedEntity rootedEntity ) {
            this.wrapperCreator = wrapperCreator;
            this.rootedEntity = rootedEntity;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (wrapperCreator == null) {
                    log.warn("No service provider for this entity.");
                } else {
                    wrapperCreator.wrapEntity(rootedEntity);
                }
            } catch (Exception ex) {
                org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().handleException(ex);
            }

        }
    }

}
