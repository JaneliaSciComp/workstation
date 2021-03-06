package org.janelia.workstation.mesh_loader;

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
    private VertexInfoKey key;
    private int vtxBufOffset;

    private Map<String, float[]> attributeMap = new HashMap<>();
    private Map<String, Integer> attributeNameVsCount = new HashMap<>();
    private List<Triangle> triangleInclusions = new ArrayList<>();

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
        a_normal, b_color
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
     * Certain attributes will be well-known. Any that are may be fetched via the known-attributes enum.
     * @param attribute to fetch
     * @return array of its values.
     */
    public float[] getKnownAttribute( KnownAttributes attribute ) {
        return attributeMap.get( attribute.toString() );
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

    public void addIncludingTriangle( Triangle triangle ) {
        triangleInclusions.add( triangle );
    }
    
    /**
     * This clone is partially-deep.  It uses same key ref, but copies
     * attribute values.  Note: not overriding clone, purposely.
     * 
     * @return separately-changeable copy of this bean.
     */
    public VertexInfoBean cloneIt() {
        // Specifically avoid triangle inclusions, here.
        VertexInfoBean rtnVal = new VertexInfoBean();
        rtnVal.setKey(key);
        rtnVal.attributeMap = new HashMap<>( this.getAttributeMap() );
        rtnVal.coordinates = getCoordinates();
        rtnVal.setVtxBufOffset( this.getVtxBufOffset() );
        rtnVal.attributeNameVsCount = new HashMap<>( this.attributeNameVsCount );
        return rtnVal;
    }

    /**
     * Count on the uniqueness of the enum values, to whittle down the output to a unique
     * set of normals.
     *
     * @return unique list of normal directions.
     */
    public Set<AxialNormalDirection> getUniqueNormals() {
        Set<AxialNormalDirection> rtnVal = new HashSet<>();
        for ( Triangle triangle: triangleInclusions ) {
            rtnVal.add( triangle.getNormalVector() );
        }
        return rtnVal;
    }
    
    public Collection<Triangle> getIncludingTriangles() {
        return triangleInclusions;
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
