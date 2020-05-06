package org.janelia.workstation.mesh_loader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes JOGL-compatible/NIO buffers from the contents of vertex factory.
 *
 * Created by fosterl on 4/18/14.
 */
public class BufferPackager {
    private static final int COORDS_PER_VERTEX = 3;
    private static final int BYTES_PER_FLOAT = Float.SIZE / Byte.SIZE;

    private Logger log = LoggerFactory.getLogger(BufferPackager.class);
    
    /**
     * Create index buffer suitable for an upload to GPU.
     *
     * @param triangleSource from which to pull these data.
     * @return as-needed buffer.
     */
    public IntBuffer getIndices( TriangleSource triangleSource ) {
        // Iterate over triangles to get the index buffer.
        List<Triangle> triangleList = triangleSource.getTriangleList();

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
    public FloatBuffer getVertexAttributes( TriangleSource factory ) {
        List<VertexInfoBean> vertices = factory.getVertices();
        if (vertices.isEmpty()) {
            log.info("No vertices.");
            //new RuntimeException("Getting NO vertex attributes in packager.").printStackTrace(); // Marking the spot.
        }
        else {
            log.info("Got {} vertices.", vertices.size());
        }

        // Iterate over the vertices to get vertex attributes.  The order of vertices in that collection should
        // match the numbers used in making the indices above.
        //   Need three floats for each vertex followed by three floats for each normal.  Also,
        //   need any optional extra attributes accounted.
        int attributesPerVertex = 1;
        FloatBuffer vertexAttribBuffer = null;
        List<String> vertexAttributeOrderList = null;
        for ( VertexInfoBean bean: vertices ) {
            if ( vertexAttribBuffer == null ) {
                Map<String, float[]> attributes = bean.getAttributeMap();
                if (attributes != null) {
                    // Expect the attributes of all vertices to be same size.
                    attributesPerVertex += attributes.size();
                }
                vertexAttribBuffer = allocate( vertices, attributesPerVertex );
            }
            vertexAttribBuffer.put( bean.getCoordinates() );
            // Add all other vertex attributes. Should be same keys in all vertices.
            Map<String,float[]> attributes = bean.getAttributeMap();
            if ( vertexAttributeOrderList == null ) {
                vertexAttributeOrderList = new ArrayList<>();
                vertexAttributeOrderList.addAll( attributes.keySet() );
            }
            Collections.sort(vertexAttributeOrderList);
            for ( String attName: vertexAttributeOrderList ) {
                vertexAttribBuffer.put( bean.getAttribute( attName ) );
            }
        }
        
        if (vertexAttribBuffer == null) {
            // Return an empty buffer.
            vertexAttribBuffer = FloatBuffer.allocate(0);
        }

        return vertexAttribBuffer;
    }
        
    private FloatBuffer allocate(List<VertexInfoBean> vertices, int attributesPerVertex) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                attributesPerVertex * vertices.size() * COORDS_PER_VERTEX * BYTES_PER_FLOAT
        );
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertexAttribBuffer = byteBuffer.asFloatBuffer();
        vertexAttribBuffer.rewind();
        return vertexAttribBuffer;
    }

}
