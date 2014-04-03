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
public class VtxAttribMgr {
    private Logger logger = LoggerFactory.getLogger(VtxAttribMgr.class);

    private List<MaskChanRenderableData> beanList;
    public VtxAttribMgr( List<MaskChanRenderableData> beanList ) {
        this.beanList = beanList;
    }

    public void execute() throws Exception {
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
        }
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

}
