package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.core.util.FileCallable;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;

/**
 * Open a file path with the default application associated with that file type
 * by the operating system.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenWithDefaultAppAction extends AbstractAction {

    private final String filepath;

    /**
     * @return true if this operation is supported on the current system.
     */
    public static boolean isSupported() {
        return SystemInfo.isMac || SystemInfo.isLinux;
    }

    public OpenWithDefaultAppAction(String filepath) {
        super("Open With OS");
        this.filepath = filepath;
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        try {
            if (filepath == null) {
                throw new Exception("Entity has no file path");
            }
            ActivityLogHelper.logUserAction("OpenInFinderAction.doAction", filepath);
            Utils.processStandardFilepath(filepath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        if (!DesktopApi.open(file)) {
                            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                    "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

}
