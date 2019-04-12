package org.janelia.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.browser.tools.ToolInfo;
import org.janelia.workstation.browser.tools.ToolMgr;
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
        // Do nothing. Action is performed by menu presenter.
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
        Set<String> keySet = ToolMgr.getToolMgr().getTools().keySet();
        return createMenuItems(keySet);

    }

    private List<JMenuItem> createMenuItems(Set<String> keySet) {
        List<JMenuItem> newItems = new ArrayList<>();
        for (final String o : keySet) {
            ToolInfo tmpTool = ToolMgr.getToolMgr().getTool(o);
            try {
                JMenuItem tmpMenuItem = new JMenuItem(tmpTool.getName(),
                        UIUtils.getClasspathImage(tmpTool.getIconPath()));
                newItems.add(tmpMenuItem);
                tmpMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ToolMgr.runToolSafely(o);
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
