package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolConfigurationDialog;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolInfo;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolListener;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.prefs.BackingStoreException;


/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ToolsMenu extends JMenu implements ToolListener {
    private JMenuItem toolsConfiguration;
    private JFrame parentFrame;

    public ToolsMenu(Browser console) {
        super("Tools");
        this.setMnemonic('T');
        this.parentFrame = console;
        ToolMgr.getToolMgr().registerPrefMgrListener(this);
        toolsConfiguration = new JMenuItem("Configure Tools...");
        toolsConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    new ToolConfigurationDialog(parentFrame);
                }
                catch (BackingStoreException e) {
                    e.printStackTrace();
                }
            }
        });

        rebuildMenu();
    }

    public void rebuildMenu() {
        this.removeAll();

        Set keySet = ToolMgr.getTools().keySet();
        for (final Object o : keySet) {
            JMenuItem tmpMenuItem = null;
            ToolInfo tmpTool = ToolMgr.getTool((String) o);

            try {
                tmpMenuItem = new JMenuItem(tmpTool.getName(),
                    Utils.getClasspathImage(tmpTool.getIconPath()));
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            add(tmpMenuItem).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        ToolMgr.runTool((String)o);
                    }
                    catch (IOException e1) {
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Could not launch this tool. " +
                                "Please choose the appropriate file path from the Tools->Configure Tools area", "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

        }

        add(new JSeparator());
        add(toolsConfiguration);
    }

    @Override
    public void toolsChanged() {
        rebuildMenu();
    }

    @Override
    public void preferencesChanged() {}
}
