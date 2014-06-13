package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.actions.RemoveEntityAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.viewer3d.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.gui.viewer3d.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.workstation.gui.viewer3d.events.AlignmentBoardItemRemoveEvent;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.viewer.AlignedItem;
import org.janelia.it.workstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
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

    protected final AlignmentBoardContext alignmentBoardContext;
    protected final AlignedItem alignedItem;
    protected final List<AlignedItem> multiSelectionItems;

    // Internal state
    protected boolean nextAddRequiresSeparator = false;

    public LayerContextMenu(AlignmentBoardContext alignmentBoardContext, AlignedItem alignedItem, List<AlignedItem> multiSelectionItems) {
        this.alignmentBoardContext = alignmentBoardContext;
        this.alignedItem = alignedItem;
        this.multiSelectionItems = multiSelectionItems;
    }

    public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());

        setNextAddRequiresSeparator(true);
        add(getChooseColorItem());
        add(getDropColorItem());
        // This is a special debug item. It may also not be working as intended. LLF, 1/10/2014
        if ( SessionMgr.getUsername().equals( "fosterl" ) ) {
            add(getRawRenderToggle());
        }
        add(getRenameItem());
        add(getDeleteUnderClickItem());
        add(getDeleteMultiSelectionItem());
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
                AlignmentBoardMgr.getInstance().getLayersPanel().chooseColor(alignedItem);
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

                final String newName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Alias:\n", "Set Alias For "
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

    protected JMenuItem getDeleteUnderClickItem() {

        List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
        rootedEntityList.add(alignedItem.getInternalRootedEntity());
        String name = alignedItem.getName();
        if ( name.length() > 15 ) {
            name = name.substring( 0, 6 ) + "..." + name.substring( name.length() - 6 );
        }
        String text = String.format("  Remove the '%s' from Alignment Board", name);
        return getDeleteListItem(rootedEntityList, text);
    }

    private JMenuItem getDeleteMultiSelectionItem() {
        List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
        for ( AlignedItem item: multiSelectionItems ) {
            rootedEntityList.add(item.getInternalRootedEntity());
        }
        String text = String.format("  Remove %d items from Alignment Board", multiSelectionItems.size() );
        return getDeleteListItem(rootedEntityList, text);
    }
    
    private JMenuItem getDeleteListItem(final List<RootedEntity> rootedEntityList, String text) {
        final Action action = new RemoveEntityAction(rootedEntityList, false, false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AlignmentBoardItemRemoveEvent abEvent;
                if ( rootedEntityList.size() == 1 ) {
                    RootedEntity nextRootedEntity = rootedEntityList.get( 0 );
                    EntityData myEd = nextRootedEntity.getEntityData();
                    log.debug("The removed entity was an aligned item, firing alignment board event...");
                    abEvent = new AlignmentBoardItemRemoveEvent(
                            alignmentBoardContext, new AlignedItem(nextRootedEntity), myEd == null ? null : myEd.getOrderIndex());
                }
                else {
                    abEvent = new AlignmentBoardItemRemoveEvent(
                            alignmentBoardContext, null, null
                    );
                }
                ModelMgr.getModelMgr().postOnEventBus(abEvent);
                return null;
            }
        });

        JMenuItem deleteItem = new JMenuItem(text);
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

}
