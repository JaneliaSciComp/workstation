package org.janelia.it.workstation.gui.browser.actions;

import java.io.File;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.DesktopApi;
import org.janelia.it.workstation.shared.util.FileCallable;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.util.Utils;

/**
 * Given an entity with a File Path, reveal the path in Finder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInFinderAction implements NamedAction {

    private final String filepath;

    /**
     * @return true if this operation is supported on the current system.
     */
    public static boolean isSupported() {
        return (SystemInfo.isMac || SystemInfo.isLinux || SystemInfo.isWindows);
    }

    public OpenInFinderAction(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String getName() {
        if (SystemInfo.isMac) {
            return "Reveal In Finder";
        }
        else if (SystemInfo.isLinux) {
            return "Reveal In File Manager";
        }
        else if (SystemInfo.isWindows) {
            return "Reveal In Windows Explorer";
        }
        return null;
    }

    @Override
    public void doAction() {
        try {
            Utils.processStandardFilepath(filepath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        if (!DesktopApi.browse(file)) {
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
