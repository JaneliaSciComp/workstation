package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.OctreeMetadataSniffer;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.MicronCoordsFormatter;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;

/**
 * Helps classes that do similar things to move stuff into the clipboard.
 * @author fosterl
 */
public class ClipboardActionHelper {
    private final static String FILE_SEP = System.getProperty("file.separator");
    private final static String LINUX_FILE_SEP = "/";
    
    public static Vec3 getCoordVector(String coordStr) {
        final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(null);
        double[] micronLocation = micronCoordsFormatter.messageToTuple(coordStr);
        Vec3 vec = new Vec3( micronLocation[0], micronLocation[1], micronLocation[2] );
        return vec;
    }

    public static String getOctreePathAtCoords(
            TileFormat tileFormat, BasicObservableCamera3d camera, CoordinateAxis axis, Vec3 vec
    ) {
        // testCompare(tileFormat, camera, axis, vec);
        //Vec3 vec = camera.getFocus(); // Testing, only. Rules out location drift.
        //  Method return found to be flawed at greater-than-0-level zoom.
        //TileIndex index = tileFormat.tileIndexForXyz(vec, tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit()), axis);
        int requiredZoomLevel = tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
        TileIndex index = tileFormat.tileIndexForXyz(vec, 0, axis);
        File path = OctreeMetadataSniffer.getOctreeFilePath(index, tileFormat, true);
        String filePathStr = path.toString().replace(FILE_SEP, LINUX_FILE_SEP);
        if (requiredZoomLevel > 0) {
            // Now, we operate on the path.  The 0-level path can be modified for
            // the required zoom-out level.
            int trimPoint = filePathStr.length() + 1;
            for (int i = 0; i < requiredZoomLevel && trimPoint > 0; i++) {
                // Must "back over" the previous slash, or keep finding it.
                int nextPoint = filePathStr.lastIndexOf(LINUX_FILE_SEP, trimPoint - 1);
                trimPoint = nextPoint;
            }
            if (trimPoint >= 0) {
                filePathStr = filePathStr.substring(0, trimPoint);
            }            
        }
        //System.out.println("Returning " + filePathStr + " for required zoom of " + requiredZoomLevel + " and full zoom path is " + path.toString());
        return filePathStr;
    }
    
    public static void setClipboard(String clipboardString) {
        StringSelection selection = new StringSelection(clipboardString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
    
    /**
     * This can be used to issue log chatter telling how the 'flawed' way 
     * would have calculated the path.
     */
    @SuppressWarnings("unused")
    private static void testCompare(TileFormat tileFormat, BasicObservableCamera3d camera, CoordinateAxis axis, Vec3 vec) {
        final int zoomLeve = tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
        TileIndex index = tileFormat.tileIndexForXyz(vec, zoomLeve, axis);
        
        File path = OctreeMetadataSniffer.getOctreeFilePath(index, tileFormat, true);
        String filePathStr = path.toString().replace(FILE_SEP, LINUX_FILE_SEP);
        System.out.println("Original calculation shows: " + filePathStr + " at zoom " + zoomLeve);
    }
}
