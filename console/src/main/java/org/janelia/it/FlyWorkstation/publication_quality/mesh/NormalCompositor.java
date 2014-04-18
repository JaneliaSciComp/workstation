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
                    attribArray[ i ] = (float)(normalArray[ i ] / magnitude);
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
