package org.janelia.workstation.core.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
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
    public static boolean isSupported(DomainObject domainObject) {
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
            Collection<ContextualActionBuilder> builders = ServiceAcceptorHelper.findAcceptors(domainObject);
            if (builders.size() == 1) {
                ContextualActionBuilder builder = builders.iterator().next();
                Action action = getAction(builder, domainObject);
                action.actionPerformed(null);
            } else if (builders.size() > 1) {
                showMenu(domainObject, builders);
            }
        }
        return handledHere;
    }

    private static void showMenu(DomainObject domainObject, Collection<ContextualActionBuilder> builders) {
        JPopupMenu popupMenu = new JPopupMenu("Multiple Choices for " + domainObject.getName());
        for (ContextualActionBuilder builder : builders) {
            Action action = getAction(builder, domainObject);
            popupMenu.add(action);
        }
        popupMenu.setVisible(true);
    }

    private static Action getAction(ContextualActionBuilder acceptor, Object obj) {

        Action action = acceptor.getNodeAction(obj);
        if (action == null) {
            // No node action is defined, try the regular action
            action = acceptor.getAction(obj);
        }

        action.setEnabled(ContextualActionUtils.isEnabled(action));
        return action;
    }

    /**
     * Makes the item for showing the object in its own viewer iff the object
     * type is correct.
     */
    public static Collection<Action> getOpenForContextActions(final Object obj) {

        Collection<ContextualActionBuilder> domainObjectAcceptors = ServiceAcceptorHelper.findAcceptors(obj);

        List<Action> actions = new ArrayList<>();

        int i = 0;
        for (final ContextualActionBuilder builder : domainObjectAcceptors) {

            if (builder.isPrecededBySeparator()) {
                if (!actions.isEmpty() && actions.get(actions.size()-1) != null) {
                    actions.add(null);
                }
            }

            Action action = getAction(builder, obj);
            actions.add(action);

            if (builder.isSucceededBySeparator()) {
                actions.add(null);
            }
        }

        if (!actions.isEmpty() && actions.get(actions.size()-1) == null) {
            // Remove trailing nulls
            actions.remove(actions.size()-1);
        }

        return actions;
    }

    public static Collection<JComponent> getOpenForContextItems(final Object obj) {
        List<JComponent> components = new ArrayList<>();
        for (Action action : getOpenForContextActions(obj)) {
            if (action == null) {
                components.add(new JSeparator());
            }
            else {
                components.add(new JMenuItem(action));
            }
        }
        return components;
    }

    public static Collection<Action> getNodeContextMenuItems(Object obj) {

        Collection<ContextualActionBuilder> domainObjectAcceptors = ServiceAcceptorHelper.findAcceptors(obj);

        List<Action> actions = new ArrayList<>();

        int i = 0;
        for (final ContextualActionBuilder builder : domainObjectAcceptors) {

            Action action = builder.getNodeAction(obj);
            if (action == null) {
                log.debug("Action builder accepted object but returned null NodeAction: {}",
                        builder.getClass().getName());
                continue;
            }

            if (!ContextualActionUtils.isVisible(action)) {
                continue;
            }

            // Add pre-separator
            if (builder.isPrecededBySeparator()) {
                if (!actions.isEmpty() && actions.get(actions.size()-1) != null) {
                    actions.add(null);
                }
            }

            actions.add(action);

            if (builder.isSucceededBySeparator()) {
                actions.add(null);
            }
        }

        // Add post-separator
        if (!actions.isEmpty() && actions.get(actions.size()-1) == null) {
            // Remove trailing nulls
            actions.remove(actions.size()-1);
        }

        return actions;
    }

    public static Collection<JComponent> getContextMenuItems(Object obj, ViewerContext viewerContext) {

        List<JComponent> items = new ArrayList<>();

        for (final ContextualActionBuilder builder : ServiceAcceptorHelper.findAcceptors(obj)) {
            try {
                buildAction(builder, items, obj, viewerContext);
            }
            catch (Exception e) {
                log.error("Error processing contextual action builder {}", builder.getClass().getName());
            }
        }

        if (!items.isEmpty() && items.get(items.size()-1) instanceof JSeparator) {
            // Remove trailing separators
            items.remove(items.size()-1);
        }

        return items;
    }

    private static void buildAction(ContextualActionBuilder builder, List<JComponent> items, Object obj, ViewerContext viewerContext) {

        log.trace("Using builder {}", builder.getClass().getSimpleName());

        Action action = builder.getAction(obj);
        if (action == null) {
            log.trace("  Action builder accepted object but returned null Action");
            return;
        }

        if (action instanceof ViewerContextReceiver) {
            // Inject the context
            log.trace("  Injecting viewer context: {}", viewerContext);
            ((ViewerContextReceiver)action).setViewerContext(viewerContext);
        }

        if (!ContextualActionUtils.isVisible(action)) {
            log.trace("  Action is not visible");
            return;
        }

        // Add pre-separator
        if (builder.isPrecededBySeparator()) {
            if (!items.isEmpty() && !(items.get(items.size()-1) instanceof JSeparator)) {
                log.trace("  Adding pre-separator");
                items.add(new JSeparator());
            }
        }

        if (action instanceof PopupMenuGenerator) {
            // If the action has a popup generator, use that
            JMenuItem popupPresenter = ((PopupMenuGenerator) action).getPopupPresenter();
            if (popupPresenter != null) {
                log.trace("  Adding popup presenter");
                items.add(popupPresenter);
            }
            else {
                log.trace("  Popup presenter was null, falling back on wrapping action in menu item");
                JMenuItem item = new JMenuItem(action);
                items.add(item);
            }
        }
        else {
            // Otherwise, just wrap the action
            log.trace("  Wrapping action in menu item");
            JMenuItem item = new JMenuItem(action);
            items.add(item);
        }

        // Add post-separator
        if (builder.isSucceededBySeparator()) {
            log.trace("  Adding post-separator");
            items.add(new JSeparator());
        }
    }
}
