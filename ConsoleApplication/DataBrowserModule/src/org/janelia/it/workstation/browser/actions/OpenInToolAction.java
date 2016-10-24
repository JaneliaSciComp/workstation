package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.editor.SampleResultContextMenu;
import org.janelia.it.workstation.browser.tools.ToolMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named action to open a file with a given tool in a specified mode.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInToolAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(SampleResultContextMenu.class);

    private final String tool;
    private final String path;
    private final String mode;
    
    public OpenInToolAction(String tool, String path, String mode) {
        super(getName(tool, mode));
        this.tool = tool;
        this.path = path;
        this.mode = mode;
    }

    public static String getName(String tool, String mode) {
        if (ToolMgr.TOOL_VAA3D.equals(tool)) {
            if (ToolMgr.MODE_3D.equals(mode)) {
                return "View In Vaa3D 3D View";
            }
            else {
                return "View In Vaa3D Tri-View";
            }
        }
        else if (ToolMgr.TOOL_FIJI.equals(tool)) {
            return "View In Fiji";
        }
        return "View In "+tool;
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("OpenInToolAction.doAction", tool);
        try {
            ToolMgr.openFile(tool, path, mode);
        } 
        catch (Exception e) {
            log.error("Error launching tool", e);
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "Could not launch this tool. "
                    + "Please choose the appropriate file path from the Tools->Configure Tools area",
                    "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }
}
