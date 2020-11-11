package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.gui.large_volume_viewer.MicronCoordsFormatter;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the text, as it is expected, in the status label, into coordinates,
 * and uses the coords to lookup the tile file at that location.
 */
public class RawFileLocToClipboardAction extends AbstractAction {

    private final static Logger LOG = LoggerFactory.getLogger(RawFileLocToClipboardAction.class);

    private final JLabel statusLabel;
    private final TileFormat tileFormat;
    private final String sampleAcquisitionPath;
    private final RenderedVolumeLocation renderedVolumeLocation;
    private final RenderedVolumeLoader renderedVolumeLoader;

    public RawFileLocToClipboardAction(
            JLabel statusLabel,
            TileFormat tileFormat,
            String sampleAcquisitionPath,
            RenderedVolumeLocation renderedVolumeLocation,
            RenderedVolumeLoader renderedVolumeLoader) {
        this.statusLabel = statusLabel;
        this.tileFormat = tileFormat;
        this.sampleAcquisitionPath = sampleAcquisitionPath;
        this.renderedVolumeLocation = renderedVolumeLocation;
        this.renderedVolumeLoader = renderedVolumeLoader;
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

        LOG.info("Translated [" + content + "] to [" + micronIntCoords[0] + "," + micronIntCoords[1] + "," + micronIntCoords[2] + "]");
        TileFormat.VoxelXyz voxelCoords
                = tileFormat.voxelXyzForMicrometerXyz(
                        new TileFormat.MicrometerXyz(
                                micronIntCoords[0],
                                micronIntCoords[1],
                                micronIntCoords[2]
                        )
                );

        String imageLocationURI = renderedVolumeLoader.findClosestRawImageFromVoxelCoord(renderedVolumeLocation, voxelCoords.getX(), voxelCoords.getY(), voxelCoords.getZ())
                .map(rawImage -> {
                    String acquisitionPath = StringUtils.defaultIfBlank(sampleAcquisitionPath, rawImage.getAcquisitionPath());
                    rawImage.setAcquisitionPath(acquisitionPath);
                    return renderedVolumeLocation.getContentURIFromAbsolutePath(rawImage.getRawImagePath(0, null));
                })
                .orElseThrow(() -> {
                    // I don't know if this will ever happen so for now I am throwing an exception
                    LOG.warn("No location URI found for ({}, {}, {})", voxelCoords.getX(), voxelCoords.getY(), voxelCoords.getZ());
                    return new IllegalArgumentException("No location URI found for " + voxelCoords.getX() + "," + voxelCoords.getY() + "," + voxelCoords.getZ());
                })
                ;
        StringSelection selection = new StringSelection(imageLocationURI);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
