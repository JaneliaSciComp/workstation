package org.janelia.workstation.core.actions;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.InstanceDataObject;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helps link up Domain Objects with services from providers.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ContextualNodeActionUtils {

    private static final Logger log = LoggerFactory.getLogger(ContextualNodeActionUtils.class);

    public static Collection<Action> getCurrentContextActions() {

        List<Action> actions = new ArrayList<>();
        try {
            FileObject actionsFile = FileUtil.getConfigFile("Menu/actions");
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
                        actions.add(action);
                        log.debug("  {}", action);
                    }
                    else if (JSeparator.class.isAssignableFrom(data.instanceClass())) {
                        // Don't add two separators in a row
                        if (actions.size() > 1 && actions.get(actions.size()-1) != null) {
                            actions.add(null);
                            log.debug("-----");
                        }
                    }
                    else {
                        log.warn("Unsupported action class: " + data.instanceClass());
                    }
                }
            }
            catch (Exception e) {
                log.warn("Could not process action "+child, e);
            }
        }
    }

    public static List<Component> getCurrentContextMenuItems() {

        List<Component> components = new ArrayList<>();

        boolean sep = true;
        Collection<Action> contextActions = getCurrentContextActions();
        for (Action action : contextActions) {
            if (action==null) {
                if (!sep) {
                    components.add(new JPopupMenu.Separator());
                    log.debug("-----");
                    sep = true;
                }
            }
            else if (action instanceof PopupMenuGenerator) {
                try {
                    JMenuItem popupPresenter = ((PopupMenuGenerator) action).getPopupPresenter();
                    if (popupPresenter != null) {
                        components.add(popupPresenter);
                        log.debug("  PopupMenuGenerator for {}", action);
                        sep = false;
                    }
                }
                catch (Exception e) {
                    log.debug("Error getting popup presenter from {}", action, e);
                }
            }
            else {
                JMenuItem mi = new JMenuItem(action);
                mi.setHorizontalTextPosition(JButton.TRAILING);
                mi.setVerticalTextPosition(JButton.CENTER);
                components.add(mi);
                log.info("  "+action);
                sep = false;
            }
        }

        // Remove any trailing separators
        if (components.get(components.size()-1) instanceof JSeparator) {
            components.remove(components.size()-1);
        }

        return components;
    }
}
