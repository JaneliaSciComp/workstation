package org.janelia.workstation.mesh_loader;

import java.util.List;
import java.util.Set;

/**
 * Makes and stores composited normals, based on the combination of all "neighboring" face normals.
 * Applies the normals to the vertices.
 *
 * Created by fosterl on 4/18/14.
 */
public class NormalCompositor {
    private static final int X = 0, Y = 1, Z = 2;

    /**
     * Helper method, for anything that needs to make a normal.
     * @See http://math.stackexchange.com/questions/305642/how-to-find-surface-normal-of-a-triangle
     * 
     * @param vertices expecting three.
     * @return normal coords.
     */
    public static double[] computeNormal(List<VertexInfoBean> vertices) {
        float[] zeroCoords = vertices.get(0).getCoordinates();
        float[] oneCoords = vertices.get(1).getCoordinates();
        float[] twoCoords = vertices.get(2).getCoordinates();
//        float[] v = getLineDelta(zeroCoords, oneCoords);
//        float[] w = getLineDelta(zeroCoords, twoCoords);
        float[] v = getLineDelta(oneCoords, zeroCoords);
        float[] w = getLineDelta(twoCoords, zeroCoords);
//        float[] v = getLineDelta(oneCoords, zeroCoords);
//        float[] w = getLineDelta(oneCoords, twoCoords);
        double nx = (v[Y] * w[Z]) - (v[Z] * w[Y]);
        double ny = (v[Z] * w[X]) - (v[X] * w[Z]);
        double nz = (v[X] * w[Y]) - (v[Y] * w[X]);
        
        return normalize(new double[]{nx, ny, nz});
    }

    /**
     * Given all triangles have normals aligning to axis x,y, or z, this
     * method may be called to combine them for Gouraud effect.
     * 
     * @param vtxFactory source for triangles and vertices. 
     */
    public void combineAxialNormals(TriangleSource vtxFactory) {
        for ( VertexInfoBean vertexInfoBean: vtxFactory.getVertices() ) {
            Set<AxialNormalDirection> uniqueNormals = vertexInfoBean.getUniqueNormals();
            if ( uniqueNormals.size() == 1 ) {
                vertexInfoBean.setAttribute(
                        VertexInfoBean.KnownAttributes.a_normal.toString(),
                        uniqueNormals.iterator().next().getNumericElements(),
                        3
                );
            }
            else {
                double[] normalArray = new double[ 3 ];
                for ( AxialNormalDirection direction: uniqueNormals ) {
                    normalArray[ 0 ] += direction.getNumericElements()[ 0 ];
                    normalArray[ 1 ] += direction.getNumericElements()[ 1 ];
                    normalArray[ 2 ] += direction.getNumericElements()[ 2 ];
                }
                combineNormals(normalArray, vertexInfoBean);
            }
        }
    }

    /**
     * Given only custom normals (unpredictable alignment, and frequently not
     * along any axis), this method can be used to combine them for Gouraud
     * effect.
     * 
     * @param vtxFactory source for triangles and vertices.  
     */
    public void combineCustomNormals(TriangleSource vtxFactory) {
        for (VertexInfoBean vertexInfoBean : vtxFactory.getVertices()) {
            double[] normalArray = new double[3];
            for (Triangle triangle: vertexInfoBean.getIncludingTriangles() ) {
                if (triangle.isNormalCombinationParticant()) {
                    double[] customNormal = triangle.getCustomNormal();
                    normalArray[ 0 ] += customNormal[ 0 ];
                    normalArray[ 1 ] += customNormal[ 1 ];
                    normalArray[ 2 ] += customNormal[ 2 ];
                }
                else {
                    // Special case: use custom normal of this
                    // very triangle, and apply that to all its
                    // vertices.
                    double[] customNormal = triangle.getCustomNormal();
                    float[] attributeArray = new float[] {
                        (float)customNormal[0], (float)customNormal[1], (float)customNormal[2]
                    };
                    vertexInfoBean.setAttribute(
                            VertexInfoBean.KnownAttributes.a_normal.toString(),
                            attributeArray,
                            3
                    );
                    normalArray = customNormal;
                }
            }
            
            combineNormals( normalArray, vertexInfoBean );
        }
    }

    private void combineNormals(double[] normalArray, VertexInfoBean vertexInfoBean) {
        double sumSquares = 0;
        for (int i = 0; i < 3; i++) {
            sumSquares += normalArray[ i] * normalArray[ i];
        }
        double magnitude = Math.sqrt(sumSquares);

        float[] attribArray = new float[3];
        for (int i = 0; i < 3; i++) {
            if (magnitude > 0) {
                attribArray[ i] = (float) (normalArray[ i] / magnitude);
            } else {
                // This implies that the vertex is touching triangles in all 6 positions, making
                // its normal a net neutral.  Its facing direction can make it positive or negative.
                // At time of writing, with the absolute value being used in the shader, and normalizing
                // being done in the shader, this is a reasonable setting.  Note that w/o absolute
                // values, we would have to decide whether all touched faces are on the front or the
                // back of the whole structure.  LLF, 4/22/2014
                attribArray[ i] = 1.0f;
            }
        }
        vertexInfoBean.setAttribute(
                VertexInfoBean.KnownAttributes.a_normal.toString(),
                attribArray,
                3
        );
    }

    private static double[] normalize(double[] distance) {
        double magnitude = getMagnitude(distance);
        distance[0] /= magnitude;
        distance[1] /= magnitude;
        distance[2] /= magnitude;
        return distance;
    }

    private static double getMagnitude(double[] distance) {
        float magnitude = (float) Math.sqrt(distance[0] * distance[0] + distance[1] * distance[1] + distance[2] * distance[2]);
        return magnitude;
    }

    private static float[] getLineDelta(float[] startCoords, float[] endCoords) {
        float[] delta = new float[startCoords.length];
        for (int i = 0; i < startCoords.length; i++) {
            delta[ i] = startCoords[ i] - endCoords[ i];
        }
        return delta;
    }

}
