package org.janelia.workstation.browser.actions;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.workstation.browser.gui.options.ToolsOptionsPanelController;
import org.janelia.workstation.browser.tools.ToolInfo;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
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
@Messages("CTL_ToolsMenuAction=Configured Tools")
public final class ToolsMenuAction extends BaseContextualPopupAction {

    private static final Logger log = LoggerFactory.getLogger(ToolsMenuAction.class);

    public ToolsMenuAction() {
        setEnabledAndVisible(true);
    }

    @Override
    protected List<JComponent> getItems() {
        List<JComponent> items = new ArrayList<>();
        for (final String toolKey : ToolMgr.getToolMgr().getTools().keySet()) {
            ToolInfo tool = ToolMgr.getToolMgr().getTool(toolKey);
            try {
                JMenuItem menuItem = new JMenuItem(tool.getName(), UIUtils.getClasspathImage(tool.getIconPath()));
                menuItem.addActionListener(e -> ToolMgr.runToolSafely(toolKey));
                items.add(menuItem);
            }
            catch (FileNotFoundException e) {
                log.error("Could not create tool menu item: {}", toolKey, e);
            }
        }

        items.add(null);

        JMenuItem tmpMenuItem = new JMenuItem("Edit tools...");
        items.add(tmpMenuItem);
        tmpMenuItem.addActionListener(e -> {
            OptionsDisplayer.getDefault().open(ToolsOptionsPanelController.PATH);
        });

        return items;
    }

}
