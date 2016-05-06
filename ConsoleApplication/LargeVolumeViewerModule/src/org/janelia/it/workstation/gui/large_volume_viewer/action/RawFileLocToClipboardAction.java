package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.MicronCoordsFormatter;
import org.janelia.it.workstation.gui.large_volume_viewer.SharedVolumeImage;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
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
            CoordinateAxis axis
    ) {
        this.statusLabel = statusLabel;
        this.tileFormat = tileFormat;
        this.camera = camera;
        this.axis = axis;
        this.volumeImage = volumeImage;
        putValue(Action.NAME, "Copy Tile File Location to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String content = statusLabel.getText();
        final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(null);
        double[] micronLocation = micronCoordsFormatter.messageToTuple(content);
        Vec3 vec = new Vec3( micronLocation[0], micronLocation[1], micronLocation[2] );
        
        int[] micronIntCoords = new int[ micronLocation.length ];
        for (int i = 0; i < micronLocation.length; i++ ) {
            micronIntCoords[i] = (int)micronLocation[i];
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
        int[] voxelCoordArr = new int[] {
            voxelCoords.getX(), voxelCoords.getY(), voxelCoords.getZ()
        };

        StringSelection selection;
        try {
            RawFileInfo rfi = ModelMgr.getModelMgr().getNearestChannelFiles(volumeImage.getRemoteBasePath(), voxelCoordArr);
            File c0File = rfi.getChannel0();
            String filePathStr = c0File.toString().replace(FILE_SEP, LINUX_FILE_SEP);
            // Not truly looking for the file path; just the legs of the path.
            if (SystemInfo.isWindows) {
                int colonPos = filePathStr.indexOf(":");
                if (colonPos != -1) {
                    filePathStr = filePathStr.substring(colonPos + 1);
                }
            }
            selection = new StringSelection(filePathStr);
        } catch (Exception ex) {
            ex.printStackTrace();
            final String msg = "Failed to copy file location to clipboard.";
            selection = new StringSelection(msg);
            log.error(msg);
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}

