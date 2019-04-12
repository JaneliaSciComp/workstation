package org.janelia.workstation.browser.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFilepath;
import org.janelia.model.domain.workspace.ProxyGroup;
import org.janelia.model.domain.workspace.GroupedFolder;

/**
 * Context pop up menu for color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupContextMenu extends PopupContextMenu {
    
    // Current selection
    protected GroupedFolder contextObject;
    protected ProxyGroup group;
    protected DomainObject proxyObject;
    protected boolean multiple = false;
    protected String groupName;
    
    public GroupContextMenu(GroupedFolder groupedFolder, ProxyGroup group, DomainObject proxyObject) {
        this.contextObject = groupedFolder;
        this.group = group;
        this.proxyObject = proxyObject;
        this.groupName = proxyObject == null ? group.getName() : proxyObject.getName();
        ActivityLogHelper.logUserAction("GroupContextMenu.create", group);
    }

    public void runDefaultAction() {
    }

    public void addMenuItems() {

        if (group == null) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        add(getTitleItem());
        add(getCopyNameToClipboardItem());

        setNextAddRequiresSeparator(true);
        add(getRemoveFromFolderItem());
        
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : groupName;
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        return getNamedActionItem(new CopyToClipboardAction("Name",groupName));
    }

    protected JMenuItem getRemoveFromFolderItem() {

        Action action = new RemoveItemsFromFolderAction(contextObject, Arrays.asList(group));
        JMenuItem deleteItem = getNamedActionItem(action);
        
        if (!ClientDomainUtils.hasWriteAccess(contextObject)) {
            deleteItem.setEnabled(false);
        }

        return deleteItem;
    }
    
    protected JMenuItem getHudMenuItem() {
        if (multiple) return null;
        
        if (proxyObject instanceof HasFilepath) {
            
            JMenuItem toggleHudMI = new JMenuItem("  Show in Lightbox");
            toggleHudMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
            toggleHudMI.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ActivityLogHelper.logUserAction("ColorDepthMatchContentMenu.showInLightbox", proxyObject);
                    Hud.getSingletonInstance().setFilepathAndToggleDialog(((HasFilepath)proxyObject).getFilepath(), false, false);
                }
            });
    
            return toggleHudMI;
        }
        
        return null;
    }
}
