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
import java.util.*;

/**
 * Keeps track of vertex attributes from high-gloss mesh surface.  Will aid in constructing normals, and packaging
 * the information for use in the GPU.
 *
 * Created by fosterl on 4/2/14.
 */
public class VtxAttribMgr implements VertexAttributeManagerI {
    private Logger logger = LoggerFactory.getLogger(VtxAttribMgr.class);

    private List<MaskChanRenderableData> beanList;
    private List<TriangleSource> vertexFactories;

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
     * This is the big command step.  It scans all the renderables into factories which can crank out the
     * vertices and triangles representing them.  Return value here is useful for unit testing.
     *
     * @return list of factories--one per renderable bean.
     * @throws Exception for called methods.
     */
    public List<TriangleSource> execute() throws Exception {
        vertexFactories = new ArrayList<TriangleSource>();
        renderIdToBuffers = new HashMap<Long,RenderBuffersBean>();
        NormalCompositor normalCompositor = new NormalCompositor();
        for ( MaskChanRenderableData bean: beanList ) {
            VoxelSurfaceCollector collector = getVoxelSurfaceCollector( bean.getMaskPath(), bean.getChannelPath(), bean.getBean() );
            Map<Long,Map<Long,Map<Long,VoxelInfoBean>>> voxelMap = collector.getVoxelMap();
            Set<VoxelInfoBean> exposedBeans = getExposedVoxelSet( voxelMap, collector );
            VertexFactory vtxFactory = new VertexFactory();
            vertexFactories.add(vtxFactory);
            for ( VoxelInfoBean exposedBean: exposedBeans ) {
                vtxFactory.addEnclosure( exposedBean );
            }

            long totalVoxelCountForRenderable = 0L;
            for ( Map<Long,Map<Long,VoxelInfoBean>> mapA: voxelMap.values() ) {
                for ( Map<Long,VoxelInfoBean> mapB: mapA.values() ) {
                    totalVoxelCountForRenderable += mapB.size();
                }
            }
            long totalVoxelCountForSurface = exposedBeans.size();
            vtxFactory.setVolumeSurfaceRatio(totalVoxelCountForRenderable, totalVoxelCountForSurface);

            // Now have a full complement of triangles and vertices.  For this renderable, can traverse the
            // vertices, making a "composite normal" based on the normals of all entangling triangles.
            normalCompositor.createGouraudNormals(vtxFactory);

            // Build buffers out of all this, and save them against bean's unique ID.
            BufferPackager packager = new BufferPackager();
            RenderBuffersBean buffersBean = new RenderBuffersBean();
            buffersBean.setAttributesBuffer( packager.getVertexAttributes(vtxFactory) );
            buffersBean.setIndexBuffer( packager.getIndices(vtxFactory) );

            renderIdToBuffers.put( bean.getBean().getAlignedItemId(), buffersBean );
        }
        return vertexFactories;
    }

    public Map<Long,RenderBuffersBean> getRenderIdToBuffers() { return renderIdToBuffers; }

    /**
     * Call this after all use of this manager's data.  It will be in a useless state afterwards.
     */
    public void close() {
        renderIdToBuffers.clear();
        beanList.clear();
        vertexFactories.clear();
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
            final String maskFileName, final String chanFileName, RenderableBean renderableBean ) throws Exception {
        // Time-of-writing: only thing bean is used for is its tanslated number.
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
        loader.read(renderableBean, streamSource);
        return voxelAcceptor;
    }

}
