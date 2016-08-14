package org.janelia.it.workstation.gui.alignment_board_viewer;

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

import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.workstation.gui.alignment_board.swing.AlignmentBoardItemDetailsDialog;
import org.janelia.it.workstation.gui.alignment_board.util.ABItem;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for layers in the LayersPanel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LayerContextMenu extends JPopupMenu {

    private static final Logger log = LoggerFactory.getLogger(LayerContextMenu.class);

    private final DomainHelper domainHelper;
    protected final AlignmentBoardContext alignmentBoardContext;
    protected final AlignmentBoardItem alignmentBoardItem;
    protected final ABItem alignedItemTarget;
    protected final List<AlignmentBoardItem> multiSelectionItems;

    // Internal state
    protected boolean nextAddRequiresSeparator = false;

    public LayerContextMenu(AlignmentBoardContext alignmentBoardContext, AlignmentBoardItem alignmentBoardItem, List<AlignmentBoardItem> multiSelectionItems) {
        this.alignmentBoardContext = alignmentBoardContext;
        this.alignmentBoardItem = alignmentBoardItem;
        this.domainHelper = new DomainHelper();
        this.alignedItemTarget = domainHelper.getObjectForItem(alignmentBoardItem);
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
        add(getShowOverlapsItem());
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
        String name = alignedItemTarget.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Copy Name To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(alignedItemTarget.getName());
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
                Transferable t = new StringSelection(alignedItemTarget.getId().toString());
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
                new AlignmentBoardItemDetailsDialog().show(alignmentBoardItem);
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getChooseColorItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Choose Color");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AlignmentBoardMgr.getInstance().getLayersPanel().chooseColor(alignmentBoardItem);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getRawRenderToggle() {
        JMenuItem menuItem = new JMenuItem(
                RenderUtils.isPassthroughRendering(alignmentBoardItem) ? "  Monocolored Rendering" : "  Raw Rendering");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        RenderUtils.setPassthroughRendering(
                                !RenderUtils.isPassthroughRendering(alignmentBoardItem), alignmentBoardItem);
                    }

                    @Override
                    protected void hadSuccess() {
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignmentBoardItem, ChangeType.ColorChange);
                        Events.getInstance().postOnEventBus(event);
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
                        alignmentBoardItem.setColor(null);
                    }

                    @Override
                    protected void hadSuccess() {
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignmentBoardItem, ChangeType.ColorChange);
                        Events.getInstance().postOnEventBus(event);
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

    protected JMenuItem getShowOverlapsItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Show Only Overlapping");
        copyMenuItem.setToolTipText("Display only items which overlap this one.");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        alignmentBoardItem.setColor(null);
                    }

                    @Override
                    protected void hadSuccess() {
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignmentBoardItem, ChangeType.OverlapFilter);
                        Events.getInstance().postOnEventBus(event);
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
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                final String newName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Alias:\n", "Set Alias For "
                        + alignedItemTarget.getName()+" in this Alignment Board", JOptionPane.PLAIN_MESSAGE, 
                        null, null, alignedItemTarget.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
						alignmentBoardItem.setName(newName);
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        //Need to notify.  Call it a visibility change for now.
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
								alignmentBoardContext, alignmentBoardItem, ChangeType.NameChange);
						Events.getInstance().postOnEventBus(event);
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });

        //TODO: find modern code.
//        if (!ModelMgrUtils.hasWriteAccess(alignmentBoardItem.getInternalEntity())) {
//            renameItem.setEnabled(false);
//        }
        
        return renameItem;
    }

    protected JMenuItem getDeleteUnderClickItem() {

        List<AlignmentBoardItem> domainObjectList = new ArrayList<>();
        domainObjectList.add(alignmentBoardItem);
        ABItem dObj = domainHelper.getObjectForItem(alignmentBoardItem);
        String name = dObj.getName();
        if ( name.length() > 15 ) {
            name = name.substring( 0, 6 ) + "..." + name.substring( name.length() - 6 );
        }
        String text = String.format("  Remove the '%s' from Alignment Board", name);
        return getDeleteListItem(domainObjectList, text);
    }

    private JMenuItem getDeleteMultiSelectionItem() {
        List<AlignmentBoardItem> abiList = new ArrayList<>();
        for ( AlignmentBoardItem item: multiSelectionItems ) {
            abiList.add(item);
        }
        String text = String.format("  Remove %d items from Alignment Board", multiSelectionItems.size() );
        return getDeleteListItem(abiList, text);
    }
    
    private JMenuItem getDeleteListItem(final List<AlignmentBoardItem> alignmentBoardItems, String text) {
        final Action action = new Action() { //new RemoveEntityAction(domainObjects, false, false, new Callable<Void>() {
            @Override
            public String getName() {
                return "Delete List";
            }
            @Override
            public void doAction() {
                try {
                    call();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            //@Override
            public Void call() throws Exception {
				// Let's take them off the board.
				alignmentBoardContext.removeDomainObjectRefs(alignmentBoardItems);				
                return null;
            }
        };

        JMenuItem deleteItem = new JMenuItem(text);
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                action.doAction();
            }
        });
        
        return deleteItem;
    }

}
