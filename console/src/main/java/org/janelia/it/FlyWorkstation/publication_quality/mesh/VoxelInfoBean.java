package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.HashMap;
import java.util.Map;

/**
 * Bag-o-data about a voxel.  Includes the key for finding it in a collection.
 *
 * Created by fosterl on 3/24/14.
 */
public class VoxelInfoBean {

    // These constants establish face-order.
    public static final int TOP_FACE = 0;
    public static final int LEFT_FACE = 1;
    public static final int FRONT_FACE = 2;
    public static final int BOTTOM_FACE = 3;
    public static final int BACK_FACE = 4;
    public static final int RIGHT_FACE = 5;

    private VoxelInfoKey key;
    private boolean[] exposedFaces = new boolean[ 6 ];
    private int exposedFaceCount = 0;


    private boolean exposed;

    /** This key is by 3D coordinate. */
    public VoxelInfoKey getKey() {
        return key;
    }

    public void setKey(VoxelInfoKey key) {
        this.key = key;
    }

    public void setExposedFace( int faceOffset ) {
        exposedFaces[ faceOffset ] = true;
        exposed = true;
        exposedFaceCount ++;
    }

    /**
     * Return the array, in face-order, of the coords of all neighboring coords. Lower edge cases make -1 coords.
     * Upper edge cases make larger-than max (and hence unoccuppied).
     *
     * @return array of 6 x 3 --> 6 faces times coords of each.
     */
    public long[][] getNeighborhood() {
        long[][] rtnVal = new long[][] {
                new long[ 3 ],
                new long[ 3 ],
                new long[ 3 ],
                new long[ 3 ],
                new long[ 3 ],
                new long[ 3 ],
        };

        long x = key.getPosition()[ 0 ];
        long y = key.getPosition()[ 1 ];
        long z = key.getPosition()[ 2 ];

        rtnVal[ TOP_FACE ][ 0 ] = x;
        rtnVal[ TOP_FACE ][ 1 ] = y + 1;
        rtnVal[ TOP_FACE ][ 2 ] = z;

        rtnVal[ LEFT_FACE ][ 0 ] = x - 1;
        rtnVal[ LEFT_FACE ][ 1 ] = y;
        rtnVal[ LEFT_FACE ][ 2 ] = z;

        rtnVal[ FRONT_FACE ][ 0 ] = x;
        rtnVal[ FRONT_FACE ][ 1 ] = y;
        rtnVal[ FRONT_FACE ][ 2 ] = z + 1;

        rtnVal[ BOTTOM_FACE ][ 0 ] = x;
        rtnVal[ BOTTOM_FACE ][ 1 ] = y - 1;
        rtnVal[ BOTTOM_FACE ][ 2 ] = z;

        rtnVal[ BACK_FACE ][ 0 ] = x;
        rtnVal[ BACK_FACE ][ 1 ] = y;
        rtnVal[ BACK_FACE ][ 2 ] = z - 1;

        rtnVal[ RIGHT_FACE ][ 0 ] = x + 1;
        rtnVal[ RIGHT_FACE ][ 1 ] = y;
        rtnVal[ RIGHT_FACE ][ 2 ] = z;

        return rtnVal;
    }

    public boolean[] getExposedFaces() {
        return exposedFaces;
//        long[][] neighborhood = getNeighborhood();
//        long[][] rtnVal = new long[ exposedFaceCount ][3];
//        int outputPos = 0;
//        for ( int i = 0; i < exposedFaces.length; i++ ) {
//            if ( exposedFaces[i] ) {
//                rtnVal[ outputPos ][ 0 ] = neighborhood[ i ][ 0 ];
//                rtnVal[ outputPos ][ 1 ] = neighborhood[ i ][ 1 ];
//                rtnVal[ outputPos ][ 2 ] = neighborhood[ i ][ 2 ];
//            }
//        }
//
//        return rtnVal;

    }

    public boolean isExposed() {
        return exposed;
    }

    public int getExposedFaceCount() {
        return exposedFaceCount;
    }

    public int hashCode() { return key.hashCode(); }
    public boolean equals( Object o ) {
        if ( o == null   ||  ! ( o instanceof  VoxelInfoBean ) ) {
            return false;
        }
        else {
            return ((VoxelInfoBean) o).getKey().equals( key );
        }
    }
}
