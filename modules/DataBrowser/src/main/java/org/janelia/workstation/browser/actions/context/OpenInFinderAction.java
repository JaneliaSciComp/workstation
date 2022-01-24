package org.janelia.workstation.browser.actions.context;

import java.io.File;

import javax.swing.JOptionPane;

import org.janelia.model.domain.enums.FileType;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.util.FileCallable;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OpenInFinderAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInFinderAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 200, separatorBefore = 199)
})
@NbBundle.Messages("CTL_OpenInFinderAction=Reveal in Finder")
public class OpenInFinderAction extends BaseOpenExternallyAction {

    @Override
    public String getName() {
        if (SystemInfo.isMac) {
            return "Reveal In Finder";
        } else if (SystemInfo.isLinux) {
            return "Reveal In File Manager";
        } else if (SystemInfo.isWindows) {
            return "Reveal In Windows Explorer";
        }
        return "Unsupported";
    }

    @Override
    public void performAction() {
        String filepath = getFilepath();
        if (filepath == null) return;
        try {
            ActivityLogHelper.logUserAction("OpenInFinderAction.doAction", filepath);
            Utils.processStandardFilepath(filepath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        if (!DesktopApi.browse(file)) {
                            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                    "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    /**
     * Allow any file.
     */
    protected boolean allowFileType(FileType fileType) {
        return true;
    }
}
