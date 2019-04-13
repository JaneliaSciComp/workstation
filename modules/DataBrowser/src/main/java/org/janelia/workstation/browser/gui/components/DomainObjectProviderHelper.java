package org.janelia.workstation.browser.gui.components;

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

import org.janelia.workstation.integration.spi.domain.ObjectOpenAcceptor;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.model.domain.DomainObject;

/**
 * Helps link up Domain Objects with services from providers.
 *
 * @author fosterl
 */
public class DomainObjectProviderHelper {

    /**
     * Is there some service / provider that can handle this domain object?
     * 
     * @param domainObject handling this data.
     * @return some provider has registered to handle this data=T.
     */
    public boolean isSupported(DomainObject domainObject) {
        return ServiceAcceptorHelper.findAcceptors(domainObject).size() > 0;
    }

    /**
     * Carry out whatever operations are provided for this domain object.
     * 
     * @param domainObject this data will be processed.
     * @return T=carried out, or user had menu items to carry out action.
     */
    public boolean service(DomainObject domainObject) {
        boolean handledHere = false;
        // Option to popup menu is carried out here, if multiple handlers exist.
        if (domainObject != null) {
            handledHere = true;
            Collection<ObjectOpenAcceptor> domainObjectAcceptors = ServiceAcceptorHelper.findAcceptors(domainObject);
            if (domainObjectAcceptors.size() == 1) {
                ObjectOpenAcceptor acceptor = domainObjectAcceptors.iterator().next();
                acceptor.acceptObject(domainObject);
            }
            else if (domainObjectAcceptors.size() > 1) {
                showMenu(domainObject, domainObjectAcceptors);
            }
        }
        return handledHere;
    }
    
    private void showMenu(DomainObject dObj, Collection<ObjectOpenAcceptor> acceptors) {
        Map<Integer,Action> orderingMap = new HashMap<>();
        List<Integer> orderingList = new ArrayList<>();
        JPopupMenu popupMenu = new JPopupMenu("Multiple Choices for " + dObj.getName());
        for (ObjectOpenAcceptor acceptor: acceptors) {
            orderingMap.put(acceptor.getOrder(), new ServiceAction(acceptor, dObj));
        }
        Collections.sort(orderingList);
        for (Integer orderingItem: orderingList) {
            popupMenu.add(orderingMap.get(orderingItem));
        }
        popupMenu.setVisible(true);
    }
    
    /**
     * Usable for placing into menu, to make the provider just work.
     */
    private class ServiceAction extends AbstractAction implements Comparable<ServiceAction> {
        private ObjectOpenAcceptor acceptor;
        private DomainObject dObj;
        public ServiceAction(ObjectOpenAcceptor acceptor, DomainObject dObj) {
            this.acceptor = acceptor;
            this.dObj = dObj;
            putValue(Action.NAME, acceptor.getActionLabel());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            acceptor.acceptObject(dObj);
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
