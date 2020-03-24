package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.workstation.integration.util.FrameworkAccess;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.browser.actions.OpenInFinderAction;
import org.janelia.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;

/**
 * Right-click context menu for masks in the color depth search editor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskContextMenu extends PopupContextMenu {

    private final ColorDepthSearch search;
    private final ColorDepthMask mask;

    public MaskContextMenu(ColorDepthSearch search, ColorDepthMask mask) {
        this.search = search;
        this.mask = mask;
        ActivityLogHelper.logUserAction("MaskContextMenu.create", mask);
    }
    
    public void addMenuItems() {
        
        if (mask==null) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        
        setNextAddRequiresSeparator(true);
        add(getRemoveFromSearchItem());

        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());

        setNextAddRequiresSeparator(true);
        add(getCreateMaskAction());

        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
    }
        
    protected JMenuItem getTitleItem() {
        String name = StringUtils.abbreviate(mask.getName(), 50);
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }
    
    protected JMenuItem getCopyNameToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("Name",mask.getName()));
    }
    
    protected JMenuItem getCopyIdToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("GUID",mask.getId().toString()));
    }

    protected JMenuItem getRemoveFromSearchItem() {

        JMenuItem removeItem = new JMenuItem("Remove mask from this search");
        removeItem.addActionListener((ActionEvent actionEvent) -> {

            ActivityLogHelper.logUserAction("MaskContextMenu.removeFromSearch", mask);
            
            SimpleWorker worker = new SimpleWorker() {
                                    
                @Override
                protected void doStuff() throws Exception {
                    DomainMgr.getDomainMgr().getModel().removeMaskFromSearch(search, mask);
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            worker.execute();
        });

        if (!ClientDomainUtils.hasWriteAccess(search)) {
            removeItem.setEnabled(false);
        }

        return removeItem;
    }

    protected JMenuItem getOpenInFinderItem() {
        String path = mask.getFilepath();
        if (path==null) return null;
        if (!OpenInFinderAction.isSupported()) return null;
        return getNamedActionItem(new OpenInFinderAction(path));
    }

    protected JMenuItem getOpenWithAppItem() {
        String path = mask.getFilepath();
        if (path==null) return null;
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }

    protected JMenuItem getCreateMaskAction() {
        return getNamedActionItem(new CreateMaskFromImageAction(mask));
    }

    protected JMenuItem getHudMenuItem() {
        
        JMenuItem toggleHudMI = new JMenuItem("Show in Lightbox");
        toggleHudMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("MaskContextMenu.showInLightbox", mask);
                Hud.getSingletonInstance().setFilepathAndToggleDialog(mask.getFilepath(), true, true);
            }
        });

        return toggleHudMI;
    }
}
