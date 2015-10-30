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
import org.janelia.it.workstation.gui.large_volume_viewer.BlockTiffOctreeLoadAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.MicronCoordsFormatter;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;

/**
 * Converts the text, as it is expected, in the status label, into coordinates,
 * and uses the coords to lookup the tile file at that location.
 */
public class RawFileLocToClipboardAction extends AbstractAction {

    private final JLabel statusLabel;
    private final TileFormat tileFormat;
    private final BasicObservableCamera3d camera;
    private final CoordinateAxis axis;
    private final String topFolder;

    public RawFileLocToClipboardAction(
            JLabel statusLabel, 
            TileFormat tileFormat, 
            String topFolder,
            BasicObservableCamera3d camera, 
            CoordinateAxis axis
    ) {
        this.statusLabel = statusLabel;
        this.tileFormat = tileFormat;
        this.camera = camera;
        this.axis = axis;
        this.topFolder = topFolder;
        putValue(Action.NAME, "Copy Tile File Location to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String content = statusLabel.getText();
        final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(null);
        double[] micronLocation = micronCoordsFormatter.messageToTuple(content);
        Vec3 vec = new Vec3( micronLocation[0], micronLocation[1], micronLocation[2] );
        
        TileIndex index = tileFormat.tileIndexForXyz(vec, tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit()), axis);
        File path = BlockTiffOctreeLoadAdapter.getOctreeFilePath(index, tileFormat, true);
        StringSelection selection = new StringSelection(topFolder + path.getAbsolutePath());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
