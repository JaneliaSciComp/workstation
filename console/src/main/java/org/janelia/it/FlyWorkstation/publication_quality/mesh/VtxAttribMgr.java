package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

/**
 * Keeps track of vertex attributes from high-gloss mesh surface.  Will aid in constructing normals, and packaging
 * the information for use in the GPU.
 *
 * Created by fosterl on 4/2/14.
 */
public class VtxAttribMgr {
    private Logger logger = LoggerFactory.getLogger(VtxAttribMgr.class);

    private List<MaskChanRenderableData> beanList;
    private Map<Long,RenderBuffersBean> renderIdToBuffers;

    /**
     * Provide a list of mask/chan renderables.  Note that only the mask file, channel file, and a unique id
     * are used from the items on this list.  They could be represented differently with relatively minimal
     * code changes below.
     *
     * @param beanList describes a unqiue id and Mask/Channel files.
     */
    public VtxAttribMgr( List<MaskChanRenderableData> beanList ) {
        this.beanList = beanList;
    }

    /**
     * This builds up the map of buffers.
     * @throws Exception
     */
    public void execute() throws Exception {
        renderIdToBuffers = new HashMap<Long,RenderBuffersBean>();
        for ( MaskChanRenderableData bean: beanList ) {
            VoxelSurfaceCollector collector = getVoxelSurfaceCollector( bean.getMaskPath(), bean.getChannelPath() );
            Map<Long,Map<Long,Map<Long,VoxelInfoBean>>> voxelMap = collector.getVoxelMap();
            Set<VoxelInfoBean> exposedBeans = getExposedVoxelSet( voxelMap, collector );
            VertexFactory vtxFactory = new VertexFactory();
            for ( VoxelInfoBean exposedBean: exposedBeans ) {
                vtxFactory.addEnclosure( exposedBean );
            }

            // Now have a full complement of triangles and vertices.  For this renderable, can traverse the
            // vertices, making a "composite normal" based on the normals of all entangling triangles.
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
                }
            }

            // Build buffers out of all this, and save them against bean's unique ID.
            RenderBuffersBean buffersBean = new RenderBuffersBean();
            buffersBean.setAttributesBuffer( getVertexAttributes( vtxFactory ) );
            buffersBean.setIndexBuffer( getIndices( vtxFactory ) );

            renderIdToBuffers.put( bean.getBean().getAlignedItemId(), buffersBean );
        }
    }

    /**
     * Create buffers suitable for an upload to GPU.
     */
    private IntBuffer getIndices( VertexFactory factory ) {
        // Iterate over triangles to get the index buffer.
        List<Triangle> triangleList = factory.getTriangleList();

        ByteBuffer byteBuffer = ByteBuffer.allocate( triangleList.size() * 3 * (Integer.SIZE / 8) );
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

    public FloatBuffer getVertexAttributes( VertexFactory factory ) {
        List<VertexInfoBean> vertices = factory.getVertices();

        // Iterate over the vertices to get vertex attributes.  The order of vertices in that collection should
        // match the numbers used in making the indices above.
        //   Need three floats for each vertex followed by three floats for each normal.
        ByteBuffer byteBuffer = ByteBuffer.allocate( 2 * (vertices.size() * 3 * ( Float.SIZE / 8 ) ) );
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
        vertexAttribBuffer.rewind();

        return vertexAttribBuffer;
    }

    private Set<VoxelInfoBean> getExposedVoxelSet(Map<Long, Map<Long, Map<Long, VoxelInfoBean>>> voxelMap, VoxelSurfaceCollector voxelSurfaceCollector ) {
        Set<VoxelInfoBean> exposedVoxels = new HashSet<VoxelInfoBean>();

        // Need browse submaps, exploring neighborhoods.
        for ( Map<Long,Map<Long,VoxelInfoBean>> yMaps: voxelMap.values() ) {
            for ( Map<Long,VoxelInfoBean> zMap: yMaps.values() ) {
                for ( VoxelInfoBean bean: zMap.values() ) {
                    long[][] neighborhood = bean.getNeighborhood();
                    int neighborPos = 0;
                    boolean isExposed = false;
                    for ( long[] neighbor: neighborhood ) {
                        VoxelInfoBean neighborBean = voxelSurfaceCollector.getVoxelBean( neighbor[ 0 ], neighbor[ 1 ], neighbor[ 2 ] );
                        if ( neighborBean == null ) {
                            bean.setExposedFace(neighborPos);
                            isExposed = true;
                        }
                        neighborPos ++;
                    }
                    if ( isExposed ) {
                        exposedVoxels.add( bean );
                    }
                }
            }
        }
        return exposedVoxels;
    }

    private VoxelSurfaceCollector getVoxelSurfaceCollector(
            final String maskFileName, final String chanFileName ) throws Exception {
        // Time-of-writing: only thing bean is used for is its tanslated number.
        RenderableBean bean = new RenderableBean();
        bean.setTranslatedNum( 1 );

        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();

        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setShowChannelData( true );
        settings.setGammaFactor( AlignmentBoardSettings.DEFAULT_GAMMA );
        settings.setChosenDownSampleRate(AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE);

        VoxelSurfaceCollector voxelAcceptor = new VoxelSurfaceCollector();

        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList(voxelAcceptor) );

        MaskChanStreamSourceI streamSource = new MaskChanStreamSourceI() {
            @Override
            public InputStream getMaskInputStream() throws IOException {
                InputStream testMaskStream = this.getClass().getResourceAsStream( maskFileName );
                if ( testMaskStream == null ) {
                    testMaskStream = new FileInputStream( maskFileName );
                    logger.warn("Resorting to hardcoded mask path.");
                }

                return testMaskStream;

            }

            @Override
            public InputStream getChannelInputStream() throws IOException {
                InputStream testChannelStream = this.getClass().getResourceAsStream( chanFileName );
                if ( testChannelStream == null ) {
                    testChannelStream = new FileInputStream( chanFileName );
                    logger.warn("Resorting to hardcoded channel path.");
                }

                return testChannelStream;
            }
        };
        loader.read(bean, streamSource);
        return voxelAcceptor;
    }

    public static class RenderBuffersBean {
        private IntBuffer indexBuffer;
        private FloatBuffer attributesBuffer;

        public IntBuffer getIndexBuffer() {
            return indexBuffer;
        }

        public void setIndexBuffer(IntBuffer indexBuffer) {
            this.indexBuffer = indexBuffer;
        }

        public FloatBuffer getAttributesBuffer() {
            return attributesBuffer;
        }

        public void setAttributesBuffer(FloatBuffer attributesBuffer) {
            this.attributesBuffer = attributesBuffer;
        }
    }

}
