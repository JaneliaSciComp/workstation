/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.browser.components;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.nb_action.DomainObjectAcceptor;
import org.janelia.it.workstation.nb_action.ServiceAcceptorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps link up Domain Objects with services from providers.
 *
 * @author fosterl
 */
public class DomainObjectProviderHelper {
    private Logger logger = LoggerFactory.getLogger(DomainObjectProviderHelper.class);
    private ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
    
    /**
     * Is there some service / provider that can handle this domain object?
     * 
     * @param dObj handling this data.
     * @return some provider has registered to handle this data=T.
     */
    public boolean isSupported(DomainObject dObj) {
        return findHandlerCollection(dObj).size() > 0;
    }

    /**
     * Carry out whatever operations are provided for this domain object.
     * 
     * @param dObj this data will be processed.
     * @return T=carried out, or user had menu items to carry out action.
     */
    public boolean service(DomainObject dObj) {
        boolean handledHere = false;
        // Option to popup menu is carried out here, if multiple handlers
        // exist.
        if (dObj != null) {
            handledHere = true;
            Collection<DomainObjectAcceptor> domainObjectAcceptors = findHandlerCollection(dObj);
            if (domainObjectAcceptors.size() == 1) {
                DomainObjectAcceptor acceptor = domainObjectAcceptors.iterator().next();
                acceptor.acceptDomainObject(dObj);
            }
            else if (domainObjectAcceptors.size() > 1) {
                showMenu(dObj, domainObjectAcceptors);
            }
        }
        return handledHere;
    }
    
    private void showMenu(DomainObject dObj, Collection<DomainObjectAcceptor> acceptors) {
        Map<Integer,Action> orderingMap = new HashMap<>();
        List<Integer> orderingList = new ArrayList<>();
        JPopupMenu popupMenu = new JPopupMenu("Multiple Choices for " + dObj.getName());
        for (DomainObjectAcceptor acceptor: acceptors) {
            orderingMap.put(acceptor.getOrder(), new ServiceAction(acceptor, dObj));
        }
        Collections.sort(orderingList);
        for (Integer orderingItem: orderingList) {
            popupMenu.add(orderingMap.get(orderingItem));
        }
        popupMenu.setVisible(true);
    }
    
    protected Collection<DomainObjectAcceptor> findHandlerCollection(DomainObject dObj) {
        return helper.findHandler(dObj, DomainObjectAcceptor.class, DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH);
    }

    /**
     * Usable for placing into menu, to make the provider just work.
     */
    private class ServiceAction extends AbstractAction implements Comparable<ServiceAction> {
        private DomainObjectAcceptor acceptor;
        private DomainObject dObj;
        public ServiceAction(DomainObjectAcceptor acceptor, DomainObject dObj) {
            this.acceptor = acceptor;
            this.dObj = dObj;
            putValue(Action.NAME, acceptor.getActionLabel());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            acceptor.acceptDomainObject(dObj);
        }

        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         * @see java.lang.Comparable<T>
         */
        @Override
        public int compareTo(ServiceAction o) {
            return acceptor.getOrder() - o.acceptor.getOrder();
        }
                
    }
}
