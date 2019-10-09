package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the text, as it is expected in the status label, into a tile
 * location, and copies that into the clipboard, in a simple format.
 */
public class OctreeFilePathToClipboardAction extends AbstractAction {
    private final JLabel statusLabel;
    private final RenderedVolumeLocation renderedVolumeLocation;
    private final TileFormat tileFormat;
    private final BasicObservableCamera3d camera;
    private final CoordinateAxis axis;
    private final Logger log = LoggerFactory.getLogger(OctreeFilePathToClipboardAction.class);

    public OctreeFilePathToClipboardAction(
            JLabel statusLabel,
            TileFormat tileFormat,
            RenderedVolumeLocation renderedVolumeLocation,
            BasicObservableCamera3d camera, 
            CoordinateAxis axis
    ) {
        this.statusLabel = statusLabel;
        this.tileFormat = tileFormat;
        this.renderedVolumeLocation = renderedVolumeLocation;
        this.camera = camera;
        this.axis = axis;
        putValue(Action.NAME, "Copy Octree Filepath to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String content = statusLabel.getText();
        Vec3 vec = ClipboardActionHelper.getCoordVector(content);

        String filePathStr = ClipboardActionHelper.getOctreePathAtCoords(tileFormat, camera, axis, vec);

        // Looking for whole path.  Need to strip away any Wndows assumptions.
        if (SystemInfo.isWindows) {
            int colonPos = filePathStr.indexOf(":");
            if (colonPos != -1) {
                filePathStr = filePathStr.substring(colonPos+1);
            }
        }
        ClipboardActionHelper.setClipboard(renderedVolumeLocation.getContentURIFromRelativePath(filePathStr));
        log.info("For location {}, camera={}. File path string {}.", content, vec, filePathStr);
    }
}
