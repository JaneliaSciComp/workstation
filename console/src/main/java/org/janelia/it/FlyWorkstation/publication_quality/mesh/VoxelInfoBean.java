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
    private static final int TOP_FACE = 0;
    private static final int LEFT_FACE = 1;
    private static final int FRONT_FACE = 2;
    private static final int BOTTOM_FACE = 3;
    private static final int BACK_FACE = 4;
    private static final int RIGHT_FACE = 5;

    private Map<String,Integer> attributeNameVsCount = new HashMap<String,Integer>();
    private VoxelInfoKey key;
    private Map<String,float[]> attributeMap = new HashMap<String,float[]>();
    private boolean[] exposedFaces = new boolean[ 6 ];

    private boolean exposed;

    /** This key is by 3D coordinate. */
    public VoxelInfoKey getKey() {
        return key;
    }

    public void setKey(VoxelInfoKey key) {
        this.key = key;
    }

    /**
     * Attributes are meant to become vertex attributes: float arrays describing a vertex.
     *
     * @return full mapping.
     */
    public Map<String, float[]> getAttributeMap() {
        return attributeMap;
    }

    /**
     * Pass back the very attribute named, only.
     *
     * @param attributeName which to lob.
     * @return array as set previously.
     */
    public float[] getAttribute( String attributeName ) {
        return attributeMap.get( attributeName );
    }

    /**
     * Set one attribute. Attributes may be 2D (as in tex coords) or 3D (as in vertex positions).
     *
     * @param attributeName called this at GPU time.
     * @param attributeCount count of items in array.
     * @param attribute array of the values.
     */
    public void setAttribute(String attributeName, float[] attribute, int attributeCount) {
        checkAttributeSanity(attributeName, attributeCount);
        attributeMap.put( attributeName, attribute );
    }

    public void setExposedFace( int faceOffset ) {
        exposedFaces[ faceOffset ] = true;
        exposed = true;
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
        rtnVal[ FRONT_FACE ][ 2 ] = z - 1;

        rtnVal[ BOTTOM_FACE ][ 0 ] = x;
        rtnVal[ BOTTOM_FACE ][ 1 ] = y - 1;
        rtnVal[ BOTTOM_FACE ][ 2 ] = z;

        rtnVal[ BACK_FACE ][ 0 ] = x;
        rtnVal[ BACK_FACE ][ 1 ] = y;
        rtnVal[ BACK_FACE ][ 2 ] = z + 1;

        rtnVal[ RIGHT_FACE ][ 0 ] = x + 1;
        rtnVal[ RIGHT_FACE ][ 1 ] = y;
        rtnVal[ RIGHT_FACE ][ 2 ] = z;

        return rtnVal;
    }

    //------------------------------------HELPERS
    private void checkAttributeSanity(String attributeName, int attributeCount) {
        Integer previousCount = attributeNameVsCount.get( attributeName );
        if ( previousCount == null ) {
            attributeNameVsCount.put( attributeName, attributeCount );
        }
        else {
            if ( attributeCount != previousCount ) {
                String msg = String.format(
                        "Cannot mix array sizes for attributes.  Previously set to %d but now seeing %d for %s.",
                        previousCount, attributeCount, attributeName
                );
                throw new IllegalArgumentException( msg );
            }
        }
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean isExposed) {
        this.exposed = isExposed;
    }
}
