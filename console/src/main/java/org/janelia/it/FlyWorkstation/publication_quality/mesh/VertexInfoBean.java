package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.*;

/**
 * This class represents all about a vertex.  A vertex must have three coordinates.  However it may have other
 * attributes (normals, for example).  If used, it will be included in one or more triangles.  This application
 * dictates that all vertices should be included in at least three triangles (non-split-triangles forming the
 * outer corner of a cube), up to 8 (forming triangle-split sides all around a vertex).
 *
 * Created by fosterl on 4/3/14.
 */
public class VertexInfoBean {
    private float[] coordinates;
    public float[] getCoordinates() {
        if ( coordinates == null ) {
            coordinates = new float[3];
            for ( int i = 0; i < 3; i++ ) {
                coordinates[ i ] = (float)key.getPosition()[ i ];
            }
        }
        return coordinates;
    }

    public int getVtxBufOffset() {
        return vtxBufOffset;
    }

    public void setVtxBufOffset(int vtxBufOffset) {
        this.vtxBufOffset = vtxBufOffset;
    }

    public enum KnownAttributes {
        normal, color
    }

    private VertexInfoKey key;
    private int vtxBufOffset;

    private Map<String,float[]> attributeMap = new HashMap<String,float[]>();
    private Map<String,Integer> attributeNameVsCount = new HashMap<String,Integer>();
    private List<Triangle> triangleInclusions = new ArrayList<Triangle>();

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

    public VertexInfoKey getKey() {
        return key;
    }

    public void setKey(VertexInfoKey key) {
        this.key = key;
    }

    public void setIncludingTriangle( Triangle triangle ) {
        triangleInclusions.add( triangle );
    }

    /**
     * Count on the uniqueness of the enum values, to whittle down the output to a unique
     * set of normals.
     *
     * @return unique list of normal directions.
     */
    public Set<VertexFactory.NormalDirection> getUniqueNormals() {
        Set<VertexFactory.NormalDirection> rtnVal = new HashSet<VertexFactory.NormalDirection>();
        for ( Triangle triangle: triangleInclusions ) {
            rtnVal.add( triangle.getNormalVector() );
        }
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

}
