package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.support.DesktopApi;
import org.janelia.it.workstation.browser.util.FileCallable;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.janelia.it.workstation.browser.util.Utils;

/**
 * Given an entity with a File Path, reveal the path in Finder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInFinderAction extends AbstractAction {

    private final String filepath;

    /**
     * @return true if this operation is supported on the current system.
     */
    public static boolean isSupported() {
        return (SystemInfo.isMac || SystemInfo.isLinux || SystemInfo.isWindows);
    }

    public OpenInFinderAction(String filepath) {
        super(getName());
        this.filepath = filepath;
    }

    public static final String getName() {
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
    public void actionPerformed(ActionEvent event) {
        try {
            ActivityLogHelper.logUserAction("OpenInFinderAction.doAction", filepath);
            Utils.processStandardFilepath(filepath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        if (!DesktopApi.browse(file)) {
                            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                                    "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
}
