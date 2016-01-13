package org.janelia.it.workstation.gui.browser.actions;

import java.io.File;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.DesktopApi;
import org.janelia.it.workstation.shared.util.FileCallable;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.util.Utils;

/**
 * Open a file path with the default application associated with that file type
 * by the operating system.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenWithDefaultAppAction implements NamedAction {

    private final String filepath;

    /**
     * @return true if this operation is supported on the current system.
     */
    public static boolean isSupported() {
        return SystemInfo.isMac || SystemInfo.isLinux;
    }

    public OpenWithDefaultAppAction(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String getName() {
        return "Open With OS";
    }

    @Override
    public void doAction() {
        try {
            if (filepath == null) {
                throw new Exception("Entity has no file path");
            }

            Utils.processStandardFilepath(filepath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        if (!DesktopApi.open(file)) {
                            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                                    "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

}
