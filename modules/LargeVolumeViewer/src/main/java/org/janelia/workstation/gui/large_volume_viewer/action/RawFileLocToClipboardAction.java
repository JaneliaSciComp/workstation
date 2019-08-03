package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.workstation.gui.large_volume_viewer.MicronCoordsFormatter;
import org.janelia.workstation.gui.large_volume_viewer.SharedVolumeImage;
import org.janelia.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the text, as it is expected, in the status label, into coordinates,
 * and uses the coords to lookup the tile file at that location.
 */
public class RawFileLocToClipboardAction extends AbstractAction {

    //todo move this to common location
    private final static String FILE_SEP = System.getProperty("file.separator");
    private final static String LINUX_FILE_SEP = "/";

    private final JLabel statusLabel;
    private final TileFormat tileFormat;
    private final BasicObservableCamera3d camera;
    private final CoordinateAxis axis;
    private final SharedVolumeImage volumeImage;

    private final Logger log = LoggerFactory.getLogger(RawFileLocToClipboardAction.class);

    public RawFileLocToClipboardAction(
            JLabel statusLabel,
            TileFormat tileFormat,
            SharedVolumeImage volumeImage,
            BasicObservableCamera3d camera,
            CoordinateAxis axis) {
        this.statusLabel = statusLabel;
        this.tileFormat = tileFormat;
        this.camera = camera;
        this.axis = axis;
        this.volumeImage = volumeImage;
        putValue(Action.NAME, "Copy Raw Tile File Location to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String content = statusLabel.getText();
        final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(null);
        double[] micronLocation = micronCoordsFormatter.messageToTuple(content);

        int[] micronIntCoords = new int[micronLocation.length];
        for (int i = 0; i < micronLocation.length; i++) {
            micronIntCoords[i] = (int) micronLocation[i];
        }
        log.info("Translated [" + content + "] to [" + micronIntCoords[0] + "," + micronIntCoords[1] + "," + micronIntCoords[2] + "]");
        TileFormat.VoxelXyz voxelCoords
                = tileFormat.voxelXyzForMicrometerXyz(
                        new TileFormat.MicrometerXyz(
                                micronIntCoords[0],
                                micronIntCoords[1],
                                micronIntCoords[2]
                        )
                );
        int[] voxelCoordArr = new int[]{
            voxelCoords.getX(), voxelCoords.getY(), voxelCoords.getZ()
        };

        StringSelection selection = new StringSelection(TiledMicroscopeDomainMgr.getDomainMgr().getNearestChannelFilesURL(volumeImage.getVolumeBaseURL().toString(), voxelCoordArr));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
