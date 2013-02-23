package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for layers in the LayersPanel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LayerContextMenu extends JPopupMenu {

    private static final Logger log = LoggerFactory.getLogger(LayerContextMenu.class);

    protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();

    protected final AlignedItem alignedItem;

    // Internal state
    protected boolean nextAddRequiresSeparator = false;

    public LayerContextMenu(AlignedItem alignedItem) {
        this.alignedItem = alignedItem;
    }

    public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());

        setNextAddRequiresSeparator(true);
        add(getRenameItem());
        add(getDeleteItem());
    }

    protected JMenuItem getTitleItem() {
        String name = alignedItem.getItemWrapper().getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Copy Name To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(alignedItem.getItemWrapper().getName());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Copy GUID To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(alignedItem.getItemWrapper().getId().toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getDetailsItem() {
        JMenuItem detailsMenuItem = new JMenuItem("  View Details");
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new EntityDetailsDialog().showForRootedEntity(alignedItem.getItemWrapper().getInternalRootedEntity());
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getRenameItem() {
        JMenuItem renameItem = new JMenuItem("  Set Alias");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(browser, "Alias:\n", "Set Alias For "
                        + alignedItem.getItemWrapper().getName()+" in this Alignment Board", JOptionPane.PLAIN_MESSAGE, 
                        null, null, alignedItem.getItemWrapper().getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }

                try {
                    ModelMgr.getModelMgr().renameEntity(alignedItem.getInternalEntity(), newName);
                } 
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(),
                            "Error renaming entity", "Error", JOptionPane.ERROR_MESSAGE);
                }

            }
        });

        if (!ModelMgrUtils.hasWriteAccess(alignedItem.getInternalEntity())) {
            renameItem.setEnabled(false);
        }
        
        return renameItem;
    }

    protected JMenuItem getDeleteItem() {

        List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
        rootedEntityList.add(alignedItem.getInternalRootedEntity());

        JMenuItem deleteItem = new JMenuItem("  Remove From Alignment Board");
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SimpleWorker worker = new SimpleWorker() {
                    
                    @Override
                    protected void doStuff() throws Exception {
                        SessionMgr.getBrowser().getLayersPanel().removeAlignedEntity(alignedItem);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // everything is done by the layers panel
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        
        for (RootedEntity rootedEntity : rootedEntityList) {
            Entity entity = rootedEntity.getEntity();
            Entity parent = rootedEntity.getEntityData().getParentEntity();
            
            boolean canDelete = true;
            // User can't delete if they don't have write access
            if (!ModelMgrUtils.hasWriteAccess(entity)) {
                canDelete = false;
                // Unless they own the parent
                if (parent!=null && parent.getId()!=null && ModelMgrUtils.hasWriteAccess(parent)) {
                    canDelete = true;
                }
            }
            // Can never delete protected entities
            if (EntityUtils.isProtected(entity)) {
                canDelete = false;
            }
            if (!canDelete) deleteItem.setEnabled(false);
        }
        
        return deleteItem;
    }

    @Override
    public JMenuItem add(JMenuItem menuItem) {

        if (menuItem == null)
            return null;

        if ((menuItem instanceof JMenu)) {
            JMenu menu = (JMenu) menuItem;
            if (menu.getItemCount() == 0)
                return null;
        }

        if (nextAddRequiresSeparator) {
            addSeparator();
            nextAddRequiresSeparator = false;
        }

        return super.add(menuItem);
    }

    public JMenuItem add(JMenu menu, JMenuItem menuItem) {
        if (menu == null || menuItem == null)
            return null;
        return menu.add(menuItem);
    }

    public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
        this.nextAddRequiresSeparator = nextAddRequiresSeparator;
    }
}
