package org.janelia.workstation.browser.actions;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.browser.gui.editor.SampleResultContextMenu;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
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
            if (ToolMgr.MODE_VAA3D_3D.equals(mode)) {
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
        
        if (path.endsWith(Utils.EXTENSION_LSM_BZ2)) {

            SimpleWorker worker = new SimpleWorker() {

                String uncompressedFilepath;
                
                @Override
                protected void doStuff() throws Exception {
                    
                    // TODO: this should be one step, download and decompress
                    
                    setStatus("Downloading LSM...");
                    File file = FileMgr.getFileMgr().getFile(path, false).getLocalFile(false);
                    uncompressedFilepath = file.getAbsolutePath().replaceFirst(Utils.EXTENSION_LSM_BZ2, Utils.EXTENSION_LSM);

                    File uncompressedFile = new File(uncompressedFilepath);
                    if (!uncompressedFile.exists()) {
                        setStatus("Decompressing LSM...");
                        Utils.copyFileToFile(file, new File(uncompressedFilepath), this, true);
                    }
                }

                @Override
                protected void hadSuccess() {
                    openFile(uncompressedFilepath);
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            // TODO: it should be possible to indicate progress for file downloads to cache
            worker.setProgressMonitor(new IndeterminateProgressMonitor(
                    FrameworkAccess.getMainFrame(), "Downloading and decompressing LSM...", ""));
            worker.execute();
        }
        else {
            openFile(path);
        }
    }
    
    private void openFile(String filepath) {
        
        try {
            ToolMgr.openFile(FrameworkAccess.getMainFrame(), tool, filepath, mode);
        } 
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
}
