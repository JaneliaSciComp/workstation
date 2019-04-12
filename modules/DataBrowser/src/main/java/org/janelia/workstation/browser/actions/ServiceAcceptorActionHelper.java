package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for creating actions based on the service acceptor framework.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ServiceAcceptorActionHelper {

    private static final Logger log = LoggerFactory.getLogger(ServiceAcceptorActionHelper.class);

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

            AbstractAction action = new AbstractAction(domainObjectAcceptor.getActionLabel()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    domainObjectAcceptor.acceptObject(obj);
                }
            };
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
}
