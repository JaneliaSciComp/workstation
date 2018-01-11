package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;

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
    }
        
    protected JMenuItem getTitleItem() {
        JMenuItem titleMenuItem = new JMenuItem(mask.getName());
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

        JMenuItem removeItem = new JMenuItem("  Remove mask from this search");
        removeItem.addActionListener((ActionEvent actionEvent) -> {

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
                    ConsoleApp.handleException(error);
                }
            };

            worker.execute();
        });

        if (!ClientDomainUtils.hasWriteAccess(search)) {
            removeItem.setEnabled(false);
        }

        return removeItem;
    }

}
