package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.*;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.shared.util.SystemInfo;

/**
 * Converts the text, as it is expected, in the status label, into a tile
 * location, and copies that into the clipboard, in a simple format.
 */
public class TileLocToClipboardAction extends AbstractAction {
    private final static String FILE_SEP = System.getProperty("file.separator");
    private final static String LINUX_FILE_SEP = "/";

    private final JLabel statusLabel;
    private final TileFormat tileFormat;
    private final BasicObservableCamera3d camera;
    private final CoordinateAxis axis;

    public TileLocToClipboardAction(
            JLabel statusLabel, 
            TileFormat tileFormat, 
            BasicObservableCamera3d camera, 
            CoordinateAxis axis
    ) {
        this.statusLabel = statusLabel;
        this.tileFormat = tileFormat;
        this.camera = camera;
        this.axis = axis;
        putValue(Action.NAME, "Copy Octree Location to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String content = statusLabel.getText();
        final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(null);
        double[] micronLocation = micronCoordsFormatter.messageToTuple(content);
        Vec3 vec = new Vec3( micronLocation[0], micronLocation[1], micronLocation[2] );
        
        TileIndex index = tileFormat.tileIndexForXyz(vec, tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit()), axis);
        File path = OctreeMetadataSniffer.getOctreeFilePath(index, tileFormat, true);
        String filePathStr = path.toString().replace(FILE_SEP, LINUX_FILE_SEP);
        // Not truly looking for the file path; just the legs of the path.
        if (SystemInfo.isWindows) {
            int colonPos = filePathStr.indexOf(":");
            if (colonPos != -1) {
                filePathStr = filePathStr.substring(colonPos+1);
            }
        }
        StringSelection selection = new StringSelection(filePathStr);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
