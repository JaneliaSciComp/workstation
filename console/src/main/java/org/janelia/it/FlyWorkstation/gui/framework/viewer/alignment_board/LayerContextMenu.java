package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.RemoveEntityAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
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

    protected final AlignmentBoardContext alignmentBoardContext;
    protected final AlignedItem alignedItem;

    // Internal state
    protected boolean nextAddRequiresSeparator = false;

    public LayerContextMenu(AlignmentBoardContext alignmentBoardContext, AlignedItem alignedItem) {
        this.alignmentBoardContext = alignmentBoardContext;
        this.alignedItem = alignedItem;
    }

    public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());

        setNextAddRequiresSeparator(true);
        add(getChooseColorItem());
        add(getDropColorItem());
        add(getRawRenderToggle());
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

    protected JMenuItem getChooseColorItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Choose Color");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                Color currColor = alignedItem.getColor();
                final Color newColor = JColorChooser.showDialog(SessionMgr.getBrowser(), "Choose color", currColor);
                if (newColor==null) return;
                
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        alignedItem.setColor(newColor);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignedItem, ChangeType.ColorChange);
                        ModelMgr.getModelMgr().postOnEventBus(event);
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getRawRenderToggle() {
        JMenuItem menuItem = new JMenuItem(
                alignedItem.isPassthroughRendering() ? "  Monocolored Rendering" : "  Raw Rendering");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        alignedItem.setPassthroughRendering(!alignedItem.isPassthroughRendering());
                    }

                    @Override
                    protected void hadSuccess() {
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignedItem, ChangeType.ColorChange);
                        ModelMgr.getModelMgr().postOnEventBus(event);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return menuItem;
    }

    protected JMenuItem getDropColorItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Default Color");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        alignedItem.setColor(null);
                    }

                    @Override
                    protected void hadSuccess() {
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignedItem, ChangeType.ColorChange);
                        ModelMgr.getModelMgr().postOnEventBus(event);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getRenameItem() {
        JMenuItem renameItem = new JMenuItem("  Set Alias");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                final String newName = (String) JOptionPane.showInputDialog(browser, "Alias:\n", "Set Alias For "
                        + alignedItem.getItemWrapper().getName()+" in this Alignment Board", JOptionPane.PLAIN_MESSAGE, 
                        null, null, alignedItem.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        ModelMgr.getModelMgr().renameEntity(alignedItem.getInternalEntity(), newName);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // Updates are driven by the entity model
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
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

        final Action action = new RemoveEntityAction(rootedEntityList, false);

        JMenuItem deleteItem = new JMenuItem("  Remove From Alignment Board");
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                action.doAction();
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
