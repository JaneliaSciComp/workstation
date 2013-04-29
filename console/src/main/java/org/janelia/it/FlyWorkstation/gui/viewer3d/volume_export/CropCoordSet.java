package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/29/13
 * Time: 11:14 AM
 *
 * These are user-chosen cropping coordinates for boxes to be added to the selected volume.  Generally, there
 * are three sets of linear coordinates: start/stop in x, y, and z.  However, there may be one special
 * coordinate set for the current, working selection.  This one is handled differently from the others.
 *
 * The responsibility of this class, is to hold all these coordinates, and having it helps to cut down the
 * number of different interfaces, and messages, between the GUI and GLSL wrap code.
 */
public class CropCoordSet {
    /** All -1 sends signal: no cropping requried. */
    public static final float[] DEFAULT_CROP_COORDS = new float[] {
            -1.0f, -1.0f,  // startX, endX
            -1.0f, -1.0f,  // startY, endY
            -1.0f, -1.0f   // startZ, endZ
    };

    private Collection<float[]> acceptedCoordinates;
    private float[] currentCoordinates;

    public CropCoordSet() {
        acceptedCoordinates = new HashSet<float[]>();
    }

    /** These have already been accepted as part of the finished selection, by the user. */
    public Collection<float[]> getAcceptedCoordinates() {
        return acceptedCoordinates;
    }

    public void setAcceptedCoordinates(Collection<float[]> acceptedCoordinates) {
        this.acceptedCoordinates = acceptedCoordinates;
    }

    /** These are being actively modified by the user. */
    public float[] getCurrentCoordinates() {
        return currentCoordinates;
    }

    public void setCurrentCoordinates(float[] currentCoordinates) {
        this.currentCoordinates = currentCoordinates;
    }

    public static CropCoordSet getDefaultCropCoordSet() {
        CropCoordSet rtnVal = new CropCoordSet();
        rtnVal.setCurrentCoordinates( DEFAULT_CROP_COORDS );
        return rtnVal;
    }
}
