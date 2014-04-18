package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * This factory can have a number of voxel beans pumped through it over time, after which it can provide a
 * full set of vertices and a full set of triangles, which have reference to each other, and which can represent
 * a mesh enclosing all the original voxels.  One instance of this class should produce the "geometry"
 * for one contiguous surface.  Do not re-use instance for multiple renderables (such as neuron fragments).
 *
 * Created by fosterl on 4/2/14.
 */
public class VertexFactory implements TriangleSource {
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /**
     * This establishes the only possible normals that can be produced when envoloping a cubic voxel that is
     * oriented with all its faces parallel to axes.
     */
    public static enum NormalDirection {
        TOP_FACE_NORMAL, FRONT_FACE_NORMAL, BOTTOM_FACE_NORMAL, LEFT_FACE_NORMAL, RIGHT_FACE_NORMAL, BACK_FACE_NORMAL;
        public float[] getNumericElements() {
            float[] rtnVal = null;
            switch ( this ) {
                case TOP_FACE_NORMAL:
                    rtnVal = TOP_FACE_NORMAL_VECT;
                    break;
                case FRONT_FACE_NORMAL:
                    rtnVal = FRONT_FACE_NORMAL_VECT;
                    break;
                case BOTTOM_FACE_NORMAL:
                    rtnVal = BOTTOM_FACE_NORMAL_VECT;
                    break;
                case LEFT_FACE_NORMAL:
                    rtnVal = LEFT_FACE_NORMAL_VECT;
                    break;
                case RIGHT_FACE_NORMAL:
                    rtnVal = RIGHT_FACE_NORMAL_VECT;
                    break;
                case BACK_FACE_NORMAL:
                    rtnVal = BACK_FACE_NORMAL_VECT;
                    break;
            }
            return rtnVal;
        }
    }

    private static final float[] TOP_FACE_NORMAL_VECT = new float[]{0, 1, 0};
    private static final float[] BOTTOM_FACE_NORMAL_VECT = new float[]{0, -1, 0};
    private static final float[] FRONT_FACE_NORMAL_VECT = new float[]{0, 0, 1};
    private static final float[] RIGHT_FACE_NORMAL_VECT = new float[]{-1, 0, 0};
    private static final float[] LEFT_FACE_NORMAL_VECT = new float[]{-1, 0, 0};
    private static final float[] BACK_FACE_NORMAL_VECT = new float[]{0, 0, 1};

    private Map<VertexInfoKey, VertexInfoBean> vertexMap = new HashMap<VertexInfoKey, VertexInfoBean>();
    private List<VertexInfoBean> vertices = new ArrayList<VertexInfoBean>();
    private List<Triangle> triangleList = new ArrayList<Triangle>();

    private long totalVolumeVoxels;
    private long totalSurfaceVoxels;

    // This is for state enforcement: do not wish to allow partial-fetch of product.
    private boolean getterCalled = false;
    private int currentVertexNumber = 0;

    /**
     * Make vertices and triangles for all exposed sides of the voxel represented by the bean. Feed data here.
     *
     * NOTE on normals: if coinciding vertices are ordered the same regardless of which faces they are in,
     * normal calculations will have to be done "smartly", rather then simply following vertex flow with cross
     * products.  Since we are using vertex-blended normals, this will not be a problem
     * todo confirm this in due course.
     */
    public void addEnclosure( VoxelInfoBean bean ) {
        if ( getterCalled ) {
            throw new IllegalStateException( "Attempting to enclose more vertices after product has been fetched." );
        }
        VoxelInfoKey key = bean.getKey();

        boolean[] exposedFaces = bean.getExposedFaces();
        for ( int i = 0; i < exposedFaces.length; i++ ) {
            if ( exposedFaces[ i ] ) {
                NormalDirection normalVector = null;
                // Ensure vertices exist for each face; make two triangles each face.
                VertexInfoBean[] vertices = new VertexInfoBean[ 4 ];
                switch (i) {
                    case VoxelInfoBean.TOP_FACE :
                        vertices[ 0 ] = addVertex( topFrontLeft( key ) );
                        vertices[ 1 ] = addVertex( topFrontRight( key ) );
                        vertices[ 2 ] = addVertex( topBackRight( key ) );
                        vertices[ 3 ] = addVertex( topBackLeft( key ) );
                        normalVector = NormalDirection.TOP_FACE_NORMAL;
                        break;
                    case VoxelInfoBean.FRONT_FACE :
                        vertices[ 0 ] = addVertex( topFrontRight( key ) );
                        vertices[ 1 ] = addVertex( topFrontLeft( key ) );
                        vertices[ 2 ] = addVertex( bottomFrontLeft( key ) );
                        vertices[ 3 ] = addVertex( bottomFrontRight( key ) );
                        normalVector = NormalDirection.FRONT_FACE_NORMAL;
                        break;
                    case VoxelInfoBean.BOTTOM_FACE :
                        vertices[ 0 ] = addVertex( bottomFrontRight( key ) );
                        vertices[ 1 ] = addVertex( bottomFrontLeft(key) );
                        vertices[ 2 ] = addVertex( bottomBackLeft(key) );
                        vertices[ 3 ] = addVertex( bottomBackRight(key) );
                        normalVector = NormalDirection.BOTTOM_FACE_NORMAL;
                        break;
                    case VoxelInfoBean.LEFT_FACE :
                        vertices[ 0 ] = addVertex( bottomFrontLeft( key ) );
                        vertices[ 1 ] = addVertex( topFrontLeft(key) );
                        vertices[ 2 ] = addVertex( topBackLeft(key) );
                        vertices[ 3 ] = addVertex( bottomBackLeft(key) );
                        normalVector = NormalDirection.LEFT_FACE_NORMAL;
                        break;
                    case VoxelInfoBean.RIGHT_FACE :
                        vertices[ 0 ] = addVertex( bottomFrontRight( key ) );
                        vertices[ 1 ] = addVertex( bottomBackRight(key) );
                        vertices[ 2 ] = addVertex( topBackRight(key) );
                        vertices[ 3 ] = addVertex( topFrontRight(key) );
                        normalVector = NormalDirection.RIGHT_FACE_NORMAL;
                        break;
                    case VoxelInfoBean.BACK_FACE :
                        vertices[ 0 ] = addVertex( bottomBackRight( key ) );
                        vertices[ 1 ] = addVertex( bottomBackLeft(key) );
                        vertices[ 2 ] = addVertex( topBackLeft(key) );
                        vertices[ 3 ] = addVertex( topBackRight(key) );
                        normalVector = NormalDirection.BACK_FACE_NORMAL;
                        break;
                }

                Triangle t1 = new Triangle();
                t1.addVertex( vertices[ 0 ] );
                t1.addVertex( vertices[ 1 ] );
                t1.addVertex( vertices[ 2 ] );
                t1.setNormalVector( normalVector );
                vertices[ 0 ].setIncludingTriangle(t1);
                vertices[ 1 ].setIncludingTriangle(t1);
                vertices[ 2 ].setIncludingTriangle( t1 );

                Triangle t2 = new Triangle();
                t2.addVertex( vertices[ 2 ] );
                t2.addVertex( vertices[ 3 ] );
                t2.addVertex( vertices[ 0 ] );
                t2.setNormalVector( normalVector );
                vertices[ 2 ].setIncludingTriangle(t2);
                vertices[ 3 ].setIncludingTriangle(t2);
                vertices[ 0 ].setIncludingTriangle(t2);

                triangleList.add( t1 );
                triangleList.add( t2 );
            }
        }
    }

    /** Establish some statistical numbers. */
    public void setVolumeSurfaceRatio(long totalVoxels, long surfaceVoxels) {
        totalVolumeVoxels = totalVoxels;
        totalSurfaceVoxels = surfaceVoxels;
    }

    /** Return the computed ratio for statistical purposes. */
    public double getSurfaceToVolumeRatio() {
        return (double)totalSurfaceVoxels / (double)totalVolumeVoxels;
    }

    //-----------------------------------------------------IMPLEMENT TriangleSource
    /**
     * Gets the full list of vertices.
     *
     * @return all vertices produced to this point.
     */
    public List<VertexInfoBean> getVertices() {
        getterCalled = true;
        return vertices;
    }

    /**
     * Gets the full list of triangles, which have references to the vertices above.
     *
     * @return all triangles produced to this point.
     */
    public List<Triangle> getTriangleList() {
        getterCalled = true;
        return triangleList;
    }

    //-----------------------------------------------------HELPERS
    /**
     * If this is a new vertex, add it to our map and collection.
     * @param vertexCoords basis of a key.
     */
    private VertexInfoBean addVertex( double[] vertexCoords ) {
        VertexInfoKey key = new VertexInfoKey();
        key.setPosition( vertexCoords );
        VertexInfoBean bean = vertexMap.get( key );
        if ( bean == null ) {
            bean = new VertexInfoBean();
            bean.setKey( key );
            vertices.add( bean );
            vertexMap.put( key, bean );

            // Provide the offset, for use in making triangle indices.
            bean.setVtxBufOffset( currentVertexNumber++ );
        }
        return bean;
    }

    // Where subtraction of 1/2 is carried out below, it is converted to
    // subract one and add 1/2 so that identicial operations are carried out against
    // all "floating-point-domain" numbers.  This so that key values will be identical
    // for cases where 1/2 is added or 1/2 is subtracted from voxel positions.
    private double[] bottomBackRight(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = key.getPosition()[ X ] + 0.5;
        vertex[ Y ] = (key.getPosition()[ Y ] - 1L) + 0.5;
        vertex[ Z ] = key.getPosition()[ Z ] - 0.5;
        return vertex;
    }

    private double[] bottomBackLeft(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = (key.getPosition()[ X ] - 1L) + 0.5;
        vertex[ Y ] = (key.getPosition()[ Y ] - 1L) + 0.5;
        vertex[ Z ] = (key.getPosition()[ Z ] - 1L) + 0.5;
        return vertex;
    }

    private double[] topBackLeft(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = (key.getPosition()[ X ] - 1L) + 0.5;
        vertex[ Y ] = key.getPosition()[ Y ] + 0.5;
        vertex[ Z ] = (key.getPosition()[ Z ] - 1L) + 0.5;
        return vertex;
    }

    private double[] bottomFrontLeft(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = (key.getPosition()[ X ] - 1L) + 0.5;
        vertex[ Y ] = (key.getPosition()[ Y ] - 1L) + 0.5;
        vertex[ Z ] = key.getPosition()[ Z ] + 0.5;
        return vertex;
    }

    private double[] bottomFrontRight(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = key.getPosition()[ X ] + 0.5;
        vertex[ Y ] = (key.getPosition()[ Y ] - 1L) + 0.5;
        vertex[ Z ] = key.getPosition()[ Z ] + 0.5;
        return vertex;
    }

    private double[] topBackRight(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = key.getPosition()[ X ] + 0.5;
        vertex[ Y ] = key.getPosition()[ Y ] + 0.5;
        vertex[ Z ] = (key.getPosition()[ Z ] - 1L) + 0.5;
        return vertex;
    }

    private double[] topFrontRight(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = key.getPosition()[ X ] + 0.5;
        vertex[ Y ] = key.getPosition()[ Y ] + 0.5;
        vertex[ Z ] = key.getPosition()[ Z ] + 0.5;
        return vertex;
    }

    private double[] topFrontLeft(VoxelInfoKey key) {
        double[] vertex = new double[ 3 ];
        vertex[ X ] = (key.getPosition()[ X ] - 1L) + 0.5;
        vertex[ Y ] = key.getPosition()[ Y ] + 0.5;
        vertex[ Z ] = key.getPosition()[ Z ] + 0.5;
        return vertex;
    }

}
