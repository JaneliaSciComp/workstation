package org.janelia.workstation.core.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.InstanceDataObject;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps link up Domain Objects with services from providers.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
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

    public static Collection<Action> getCurrentContextActions() {

        List<Action> actions = new ArrayList<>();
        try {
            FileObject actionsFile = FileUtil.getConfigFile("Menu/Actions");
            DataObject actionsObject = DataObject.find(actionsFile);
            DataFolder actionsFolder = actionsObject.getLookup().lookup(DataFolder.class);
            walkActionTree(actionsFolder, actions);
        }
        catch (Exception e) {
            log.error("Error getting current context actions", e);
        }

        // Remove any trailing null actions
        if (actions.get(actions.size()-1)==null) {
            actions.remove(actions.size()-1);
        }

        return actions;
    }

    private static void walkActionTree(DataFolder actionsFolder, List<Action> actions) {

        for (DataObject child : actionsFolder.getChildren()) {
            try {
                if (child instanceof DataFolder) {
                    DataFolder childFolder = (DataFolder) child;
                    walkActionTree(childFolder, actions);
                }
                else {
                    InstanceDataObject data = child.getLookup().lookup(InstanceDataObject.class);
                    if (data == null) {
                        log.warn("Got null instance from " + child);
                        continue;
                    }

                    if (SystemAction.class.isAssignableFrom(data.instanceClass())) {
                        Class<? extends SystemAction> clazz = (Class<? extends SystemAction>) data.instanceClass();
                        SystemAction action = SystemAction.get(clazz);

                        if (!ContextualActionUtils.isVisible(action)) {
                            log.trace("  Action is not visible");
                            continue;
                        }

                        actions.add(action);
                        log.debug("  {}", action);
                    } else if (JSeparator.class.isAssignableFrom(data.instanceClass())) {
                        // Don't add two separators in a row
                        if (actions.size() > 1 && actions.get(actions.size() - 1) != null) {
                            actions.add(null);
                            log.debug("-----");
                        }
                    } else {
                        log.warn("Unsupported action class: " + data.instanceClass());
                    }
                }
            }
            catch (Exception e) {
                log.warn("Could not process action "+child, e);
            }
        }
    }
}
