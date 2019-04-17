package org.janelia.workstation.browser.gui.components;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.integration.spi.domain.ObjectOpenAcceptor;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps link up Domain Objects with services from providers.
 *
 * @author fosterl
 */
public class DomainObjectAcceptorHelper {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectAcceptorHelper.class);

    /**
     * Is there some service / provider that can handle this domain object?
     *
     * @param domainObject handling this data.
     * @return some provider has registered to handle this data=T.
     */
    public static  boolean isSupported(DomainObject domainObject) {
        return !ServiceAcceptorHelper.findAcceptors(domainObject).isEmpty();
    }

    /**
     * Carry out whatever operations are provided for this domain object.
     *
     * @param domainObject this data will be processed.
     * @return T=carried out, or user had menu items to carry out action.
     */
    public static boolean service(DomainObject domainObject) {
        boolean handledHere = false;
        // Option to popup menu is carried out here, if multiple handlers exist.
        if (domainObject != null) {
            handledHere = true;
            Collection<ObjectOpenAcceptor> domainObjectAcceptors = ServiceAcceptorHelper.findAcceptors(domainObject);
            if (domainObjectAcceptors.size() == 1) {
                ObjectOpenAcceptor acceptor = domainObjectAcceptors.iterator().next();
                ServiceAction action = new ServiceAction(acceptor, domainObject);
                action.actionPerformed(null);
            } else if (domainObjectAcceptors.size() > 1) {
                showMenu(domainObject, domainObjectAcceptors);
            }
        }
        return handledHere;
    }

    private static void showMenu(DomainObject dObj, Collection<ObjectOpenAcceptor> acceptors) {
        Map<Integer, Action> orderingMap = new HashMap<>();
        List<Integer> orderingList = new ArrayList<>();
        JPopupMenu popupMenu = new JPopupMenu("Multiple Choices for " + dObj.getName());
        for (ObjectOpenAcceptor acceptor : acceptors) {
            orderingMap.put(acceptor.getOrder(), new ServiceAction(acceptor, dObj));
        }
        Collections.sort(orderingList);
        for (Integer orderingItem : orderingList) {
            popupMenu.add(orderingMap.get(orderingItem));
        }
        popupMenu.setVisible(true);
    }

    /**
     * Makes the item for showing the object in its own viewer iff the object
     * type is correct.
     */
    public static Collection<AbstractAction> getOpenForContextActions(final Object obj) {

        Collection<ObjectOpenAcceptor> domainObjectAcceptors = ServiceAcceptorHelper.findAcceptors(obj);

        boolean lastItemWasSeparator = false;
        int expectedCount = 0;
        TreeMap<Integer, AbstractAction> orderedMap = new TreeMap<>();
        List<AbstractAction> actionItemList = new ArrayList<>();

        for (final ObjectOpenAcceptor domainObjectAcceptor : domainObjectAcceptors) {

            final Integer order = domainObjectAcceptor.getOrder();
            if (domainObjectAcceptor.isPrecededBySeparator() && (!lastItemWasSeparator)) {
                orderedMap.put(order - 1, null);
                expectedCount++;
            }

            ServiceAction action = new ServiceAction(domainObjectAcceptor, obj);
            action.setEnabled(domainObjectAcceptor.isEnabled(obj));

            orderedMap.put(order, action);
            expectedCount++;

            actionItemList.add(action); // Bail alternative if ordering fails.

            if (domainObjectAcceptor.isSucceededBySeparator()) {
                orderedMap.put(order + 1, null);
                expectedCount++;
                lastItemWasSeparator = true;
            }
            else {
                lastItemWasSeparator = false;
            }
        }

        // This is the bail strategy for order key clashes.
        if (orderedMap.size() < expectedCount) {
            log.warn("With menu items and separators, expected {} but added {} open-for-context items."
                            + "  This indicates an order key clash.  Please check the getOrder methods of all impls."
                            + "  Returning an unordered version of item list.",
                    expectedCount, orderedMap.size());
            return actionItemList;
        }

        log.debug("Created context menu items from domain object acceptors:");
        for (Integer key : orderedMap.keySet()) {
            AbstractAction action = orderedMap.get(key);
            if (action == null) {
                log.debug("{} = Separator", key);
            } else {
                log.debug("{} = {}", key, action.getValue(Action.NAME));
            }
        }

        return orderedMap.values();
    }

    public static Collection<JComponent> getOpenForContextItems(final Object obj) {
        List<JComponent> components = new ArrayList<>();
        for (AbstractAction action : getOpenForContextActions(obj)) {
            if (action == null) {
                components.add(new JSeparator());
            }
            else {
                components.add(new JMenuItem(action));
            }
        }
        return components;
    }

    /**
     * Usable for placing into menu, to make the provider just work.
     */
    private static class ServiceAction extends AbstractAction implements Comparable<ServiceAction> {

        private ObjectOpenAcceptor acceptor;
        private Object obj;

        public ServiceAction(ObjectOpenAcceptor acceptor, Object obj) {
            this.acceptor = acceptor;
            this.obj = obj;
            putValue(Action.NAME, acceptor.getActionLabel());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            acceptor.acceptObject(obj);

            if (obj instanceof DomainObject) {
                // Update "Recently Opened" history
                FrameworkAccess.getBrowsingController().updateRecentlyOpenedHistory(Reference.createFor((DomainObject) obj));
            }
        }

        @Override
        public int compareTo(ServiceAction o) {
            return ComparisonChain.start()
                    .compare(acceptor.getOrder(), o.acceptor.getOrder(), Ordering.natural().nullsLast())
                    .result();
        }
    }
}
