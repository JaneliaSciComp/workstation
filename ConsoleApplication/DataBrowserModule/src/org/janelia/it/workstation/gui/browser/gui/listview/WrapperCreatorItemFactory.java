/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.browser.gui.listview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JMenuItem;
import org.janelia.it.jacs.model.domain.DomainObject;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.nb_action.DomainObjectCreator;
import org.janelia.it.workstation.nb_action.ServiceAcceptorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience delegate to make Menu Items for creating things which contain
 * domain objects.
 * 
 * @author fosterl
 */
public class WrapperCreatorItemFactory {
    
    private static final Logger log = LoggerFactory.getLogger( WrapperCreatorItemFactory.class );    

    /**
     * This allows an abstraction layer between making the action, and the thing
     * that is being carried out.
     * 
     * @param domainObject build another domain object around this; may be null.
     * @return the menu item suitable for add to menu.
     */
    public List<JMenuItem> makeWrapperCreatorItem(final DomainObject domainObject) {
        List<JMenuItem> rtnVal = new ArrayList<>();
        final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
        Collection<DomainObjectCreator> wrapperCreators
                = helper.findHandler(domainObject, DomainObjectCreator.class, DomainObjectCreator.LOOKUP_PATH);
        for ( DomainObjectCreator wrapperCreator: wrapperCreators ) {
            JMenuItem wrapEntityItem = new JMenuItem(wrapperCreator.getActionLabel());
            wrapEntityItem.addActionListener(new WrapObjectActionListener(wrapperCreator, domainObject));
            rtnVal.add( wrapEntityItem );
        }
        return rtnVal;
    }
    
    class WrapObjectActionListener implements ActionListener {
        private DomainObjectCreator wrapperCreator;
        private DomainObject domainObject;
        public WrapObjectActionListener( DomainObjectCreator wrapperCreator, DomainObject rootedEntity ) {
            this.wrapperCreator = wrapperCreator;
            this.domainObject = rootedEntity;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (wrapperCreator == null) {
                    log.warn("No service provider for this entity.");
                } else {
                    wrapperCreator.useDomainObject(domainObject);
                }
            } catch (Exception ex) {
                ModelMgr.getModelMgr().handleException(ex);
            }

        }
    }

}
