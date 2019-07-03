package org.janelia.workstation.browser.actions.context;

import java.io.File;

import javax.swing.JOptionPane;

import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.util.FileCallable;
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
        category = "Actions",
        id = "OpenInDefaultAppAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInDefaultAppAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 210)
})
@NbBundle.Messages("CTL_OpenInDefaultAppAction=Open With Default App")
public class OpenInDefaultAppAction extends BaseOpenExternallyAction {

    @Override
    public void performAction() {
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
