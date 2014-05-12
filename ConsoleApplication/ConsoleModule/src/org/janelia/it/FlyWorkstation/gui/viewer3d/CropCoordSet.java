package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.util.Collection;
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
    private static final float[] DEFAULT_CROP_COORDS = new float[] {
            -1.0f, -1.0f,  // startX, endX
            -1.0f, -1.0f,  // startY, endY
            -1.0f, -1.0f   // startZ, endZ
    };

    private final Collection<float[]> acceptedCoordinates;
    private float[] currentCoordinates;

    public CropCoordSet() {
        acceptedCoordinates = new HashSet<float[]>();
    }

    public boolean isEmpty() {
        return ( ( getCurrentCoordinates() == null || getCurrentCoordinates() == DEFAULT_CROP_COORDS ) && getAcceptedCoordinates().isEmpty() );
    }

    /** Push the current, putative coord volume into the accepted collection.  */
    public void acceptCurrentCoordinates() {
        if ( currentCoordinates != null  &&  ! alreadyAccepted( getAcceptedCoordinates(), getCurrentCoordinates() ) ) {
            acceptedCoordinates.add( currentCoordinates );
        }
    }

    /** These have already been accepted as part of the finished selection, by the user. */
    public Collection<float[]> getAcceptedCoordinates() {
        return acceptedCoordinates;
    }

    public void setAcceptedCoordinates(Collection<float[]> acceptedCoordinates) {
        this.acceptedCoordinates.clear();
        if ( acceptedCoordinates  !=  null ) {
            this.acceptedCoordinates.addAll( acceptedCoordinates );
        }
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

    /** It is possible for the current coord to have already been accepted. */
    public static boolean alreadyAccepted( Collection<float[]> acceptedCoordinates, float[] currentCoordinates ) {
        boolean rtnVal = false;
        for ( float[] nextAccepted: acceptedCoordinates ) {
            // See if any of the accepted ones is the same.
            boolean comparesSame = true;
            for ( int i = 0; i < nextAccepted.length  &&  comparesSame; i++ ) {
                if ( nextAccepted[ i ] != currentCoordinates[ i ] ) {
                    comparesSame = false;
                }
            }

            // If any matches all coords, this one is already in.
            if ( comparesSame ) {
                rtnVal = true;
                break;
            }
        }

        return rtnVal;
    }

    public static boolean allMaxCoords( float[] currentCoordinates ) {
        boolean max = true;
        for ( int i = 0; i < currentCoordinates.length  &&  max; i+= 2 ) {
            if ( Math.round( currentCoordinates[ i ] ) != 0  ||  Math.round( currentCoordinates[ i + 1 ] ) != 1 ) {
                max = false;
                break;
            }
        }
        return max;
    }

}
