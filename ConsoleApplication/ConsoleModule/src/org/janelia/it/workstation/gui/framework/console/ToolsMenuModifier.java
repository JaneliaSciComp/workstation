package org.janelia.it.workstation.gui.framework.console;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolInfo;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolListener;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * Formerly the ToolsMenu, this class will post changes to the tools menu,
 * and can revert previous changes in preparation for doing so.
 *
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ToolsMenuModifier implements ToolListener {
    private final Map<JMenuItem,JMenu> itemVsMenu = new HashMap<>();
    private Logger logger = LoggerFactory.getLogger( ToolsMenuModifier.class );
    
    public ToolsMenuModifier() {
        ToolMgr.getToolMgr().registerPrefMgrListener(this);
    }

    public void rebuildMenu() {
        this.removeAll();
        changeToolsMenu();
    }

    @Override
    public void toolsChanged() {
        rebuildMenu();
    }

    @Override
    public void preferencesChanged() {}
    
    /**
     * Takes all the dynamic menus (possibly) added by this class
     * and removes them from where they were added.
     */
    private void removeAll() {
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            }

            @Override
            protected void hadSuccess() {
                for (JMenuItem item : itemVsMenu.keySet()) {
                    JMenu parent = itemVsMenu.get(item);
                    parent.remove(item);
                }
            }

            @Override
            protected void hadError(Throwable error) {
            }
            
        };
        worker.execute();
    }
    
    /**
     * Possibly add a menu menu item to the tools menu.
     * 
     * @param items items to add
     * @param menu to add items to
     * @param menuBar menuBar to add menus to
     */
    private void add( final List<JMenuItem>items, final JMenu menu, final JMenuBar menuBar ) {
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            }

            @Override
            protected void hadSuccess() {
                int pos = 0;
                menuBar.remove(menu);
                for (JMenuItem item : items) {
                    menu.insert(item, pos++);
                    logger.info("Adding item " + item.getActionCommand() + " to menu " + menu.getName());
                    itemVsMenu.put(item, menu);
                }
                int menuInx = menuBar.getComponentIndex( menu );
                menuBar.add( menu, menuInx );
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
            
        };
        worker.execute();
    }

    /**
     * By whatever means, find the tools menu.
     * 
     */
    private void changeToolsMenu() {
        ToolMenuWorker worker = new ToolMenuWorker();
        worker.execute();
    }
    
    private void addMenuItems() {
        JFrame frame = WindowLocator.getMainFrame();
        JMenuBar menuBar = frame.getJMenuBar();
        if (menuBar != null) {
            JMenu toolsMenu = null;
            for (int i = 0; i < menuBar.getMenuCount() && toolsMenu == null; i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu.getText().trim().equalsIgnoreCase("tools")) {
                    toolsMenu = menu;
                    Set keySet = ToolMgr.getTools().keySet();
                    List<JMenuItem> newItems = createMenuItems(keySet);
                    add(newItems, toolsMenu, menuBar);

                }
            }

        } else {
            final String msg = "Failed to find menu bar from main frame.";
            //JOptionPane.showMessageDialog(null, msg);
            logger.warn( msg );
        }

    }

    private List<JMenuItem> createMenuItems(Set keySet) {
        List<JMenuItem> newItems = new ArrayList<>();
        for (final Object o : keySet) {
            JMenuItem tmpMenuItem;
            ToolInfo tmpTool = ToolMgr.getTool((String) o);
            try {
                tmpMenuItem = new JMenuItem(tmpTool.getName(),
                        Utils.getClasspathImage(tmpTool.getIconPath()));
                newItems.add(tmpMenuItem);
                tmpMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ToolMgr.runTool((String) o);
                        } catch (Exception e1) {
                            JOptionPane.showMessageDialog(
                                    SessionMgr.getMainFrame(),
                                    "Could not launch this tool. "
                                            + "Please choose the appropriate file path from the Tools->Configure Tools area",
                                    "ToolInfo Launch ERROR",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                });
                
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return newItems;
    }

    /** Worker class to modify the tools menu in AWT thread. */
    private class ToolMenuWorker extends SimpleWorker {

        @Override
        protected void doStuff() throws Exception {
        }

        @Override
        protected void hadSuccess() {
            addMenuItems();
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }
        
    }
}
