package org.janelia.it.workstation.gui.browser.actions;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.gui.browser.gui.editor.SampleResultContextMenu;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named action to open a file with a given tool in a specified mode.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInToolAction implements NamedAction {

    private static final Logger log = LoggerFactory.getLogger(SampleResultContextMenu.class);

    private final String tool;
    private final String path;
    private final String mode;
    
    public OpenInToolAction(String tool, String path, String mode) {
        this.tool = tool;
        this.path = path;
        this.mode = mode;
    }

    @Override
    public String getName() {
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
    public void doAction() {
        try {
            ToolMgr.openFile(tool, path, mode);
        } 
        catch (Exception e) {
            log.error("Error launching tool", e);
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Could not launch this tool. "
                    + "Please choose the appropriate file path from the Tools->Configure Tools area",
                    "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }
}
