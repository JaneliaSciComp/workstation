package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Makes JOGL-compatible/NIO buffers from the contents of vertex factory.
 *
 * Created by fosterl on 4/18/14.
 */
public class BufferPackager {
    /**
     * Create index buffer suitable for an upload to GPU.
     *
     * @param factory from which to pull these data.
     * @return as-needed buffer.
     */
    public IntBuffer getIndices( VertexFactory factory ) {
        // Iterate over triangles to get the index buffer.
        List<Triangle> triangleList = factory.getTriangleList();

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect( triangleList.size() * 3 * (Integer.SIZE / 8) );
        byteBuffer.order( ByteOrder.nativeOrder() );
        IntBuffer indexBuffer = byteBuffer.asIntBuffer();
        indexBuffer.rewind();
        for ( Triangle triangle: triangleList ) {
            List<VertexInfoBean> triangleVertices = triangle.getVertices();
            indexBuffer.put( triangleVertices.get(0).getVtxBufOffset() );
            indexBuffer.put( triangleVertices.get(1).getVtxBufOffset() );
            indexBuffer.put( triangleVertices.get(2).getVtxBufOffset() );
        }
        indexBuffer.rewind();

        return indexBuffer;
    }

    /**
     * Create vertex-attrib buffer suitable for upload to GPU.
     *
     * @param factory from which to pull these data.
     * @return as-needed buffer.
     */
    public FloatBuffer getVertexAttributes( VertexFactory factory ) {
        List<VertexInfoBean> vertices = factory.getVertices();

        // Iterate over the vertices to get vertex attributes.  The order of vertices in that collection should
        // match the numbers used in making the indices above.
        //   Need three floats for each vertex followed by three floats for each normal.
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect( 2 * (vertices.size() * 3 * ( Float.SIZE / Byte.SIZE ) ) );
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertexAttribBuffer = byteBuffer.asFloatBuffer();
        vertexAttribBuffer.rewind();
        List<String> vertexAttributeOrderList = null;
        for ( VertexInfoBean bean: vertices ) {

            vertexAttribBuffer.put( bean.getCoordinates() );
            // Add all other vertex attributes. Should be same keys in all vertices.
            Map<String,float[]> attributes = bean.getAttributeMap();
            if ( vertexAttributeOrderList == null ) {
                vertexAttributeOrderList = new ArrayList<String>();
                vertexAttributeOrderList.addAll( attributes.keySet() );
            }
            for ( String attName: vertexAttributeOrderList ) {
                vertexAttribBuffer.put( bean.getAttribute( attName ) );
            }
        }

        return vertexAttribBuffer;
    }


}
