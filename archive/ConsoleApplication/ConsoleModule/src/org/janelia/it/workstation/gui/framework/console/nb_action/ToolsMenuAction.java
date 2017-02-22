package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolInfo;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Tools",
        id = "ToolsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_ToolsMenuAction",
        lazy = false
)
@ActionReference(path = "Menu/Tools", position = 100)
@Messages("CTL_ToolsMenuAction=Tools")
public final class ToolsMenuAction extends AbstractAction implements Presenter.Menu {

    private static final Logger log = LoggerFactory.getLogger(ToolsMenuAction.class);
    
    private final JMenu subMenu;

    public ToolsMenuAction() {
        subMenu = new JMenu("Configured Tools");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
    }

    @Override
    public JMenuItem getMenuPresenter() {
        List<JMenuItem> newItems = createMenuItems();
        subMenu.removeAll();
        if (newItems != null) {
            for (JMenuItem item : newItems) {
                subMenu.add(item);
            }
        }
        return subMenu;
    }

    private List<JMenuItem> createMenuItems() {
        Set keySet = ToolMgr.getTools().keySet();
        return createMenuItems(keySet);

    }

    private List<JMenuItem> createMenuItems(Set keySet) {
        List<JMenuItem> newItems = new ArrayList<>();
        for (final Object o : keySet) {
            JMenuItem tmpMenuItem = null;
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
                        }
                        catch (Exception e1) {
                            log.error("Could launch tool: "+o,e1);
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

            }
            catch (FileNotFoundException e) {
                log.error("Could not create tool menu item: {}",o,e);
            }
        }
        return newItems;
    }

}