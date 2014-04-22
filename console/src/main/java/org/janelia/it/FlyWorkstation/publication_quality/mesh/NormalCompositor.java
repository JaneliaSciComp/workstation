package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.Set;

/**
 * Makes and stores composited normals, based on the combination of all "neighboring" face normals.
 * Applies the normals to the vertices.
 *
 * Created by fosterl on 4/18/14.
 */
public class NormalCompositor {
    public void createGouraudNormals(VertexFactory vtxFactory) {
        for ( VertexInfoBean vertexInfoBean: vtxFactory.getVertices() ) {
            Set<VertexFactory.NormalDirection> uniqueNormals = vertexInfoBean.getUniqueNormals();
            if ( uniqueNormals.size() == 1 ) {
                vertexInfoBean.setAttribute(
                        VertexInfoBean.KnownAttributes.normal.toString(),
                        uniqueNormals.iterator().next().getNumericElements(),
                        3
                );
            }
            else {
                double[] normalArray = new double[ 3 ];
                for ( VertexFactory.NormalDirection direction: uniqueNormals ) {
                    normalArray[ 0 ] += direction.getNumericElements()[ 0 ];
                    normalArray[ 1 ] += direction.getNumericElements()[ 1 ];
                    normalArray[ 2 ] += direction.getNumericElements()[ 2 ];
                }

                double sumSquares = 0;
                for ( int i = 0; i < 3; i++ ) {
                    sumSquares += normalArray[ i ] * normalArray[ i ];
                }
                double magnitude = Math.sqrt( sumSquares );

                float[] attribArray = new float[ 3 ];
                for ( int i = 0; i < 3; i++ ) {
                    if ( magnitude > 0 ) {
                        attribArray[ i ] = (float)(normalArray[ i ] / magnitude);
                    }
                    else {
                        // This implies that the vertex is touching triangles in all 6 positions, making
                        // its normal a net neurtral.  Its facing direction can make it positive or negative.
                        // At time of writing, with the absolute value being used in the shader, and normalizing
                        // being done in the shader, this is a reasonable setting.  Note that w/o absolute
                        // values, we would have to decide whether all touched faces are on the front or the
                        // back of the whole structure.  LLF, 4/22/2014
                        attribArray[ i ] = 1.0f;
                    }
                }
                vertexInfoBean.setAttribute(
                        VertexInfoBean.KnownAttributes.normal.toString(),
                        attribArray,
                        3
                );
            }
        }
    }


}
