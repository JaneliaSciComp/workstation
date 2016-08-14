package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Converts the text, as it is expected, in the status label, into a tile
 * location, and copies that into the clipboard, in a simple format.
 */
public class TileLocToClipboardAction extends AbstractAction {

    private final JLabel statusLabel;
    private final TileFormat tileFormat;
    private final BasicObservableCamera3d camera;
    private final CoordinateAxis axis;
    private final Logger log = LoggerFactory.getLogger(TileLocToClipboardAction.class);

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
        Vec3 vec = ClipboardActionHelper.getCoordVector(content);
        //Vec3 vec = camera.getFocus(); // Testing, only.
        String filePathStr = ClipboardActionHelper.getOctreePathAtCoords(tileFormat, camera, axis, vec);
        log.debug("(1)For location {}, camera={}. File path string {}.", statusLabel.getText(), vec, filePathStr);
        // Not truly looking for the file path; just the legs of the path.
        if (SystemInfo.isWindows) {
            int colonPos = filePathStr.indexOf(":");
            if (colonPos != -1) {
                filePathStr = filePathStr.substring(colonPos+1);
            }
        }
        ClipboardActionHelper.setClipboard(filePathStr);
        log.debug("(2)For location {}, camera={}. File path string {}.", statusLabel.getText(), vec, filePathStr);
    }
}
