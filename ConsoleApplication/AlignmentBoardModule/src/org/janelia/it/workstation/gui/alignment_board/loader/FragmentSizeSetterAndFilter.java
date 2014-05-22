package org.janelia.it.workstation.gui.alignment_board.loader;

import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RDComparator;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.model.domain.EntityWrapper;
import org.janelia.it.workstation.model.viewer.AlignedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/5/13
 * Time: 11:58 AM
 *
 * Given a collection of renderable beans, this will eliminate from that list, all whose voxel count is less than
 * some threshold. All beans which have passed through the filter will have their voxel counts set to the found value.
 */
public class FragmentSizeSetterAndFilter {
    private Logger logger = LoggerFactory.getLogger(FragmentSizeSetterAndFilter.class);
    private long thresholdVoxelCount;
    private long thresholdNeuronCount;

    private Map<Long,Long> renDataBeanItemParentIdToChildCount;

    public FragmentSizeSetterAndFilter(long thresholdVoxelCount, long thresholdNeuronCount) {
        this.thresholdVoxelCount = thresholdVoxelCount;
        if ( thresholdNeuronCount == -1 ) {
            this.thresholdNeuronCount = Long.MAX_VALUE;
        }
        else {
            renDataBeanItemParentIdToChildCount = new HashMap<Long,Long>();
            this.thresholdNeuronCount = thresholdNeuronCount;
        }
    }

    /**
     * Eliminates non-usables off the list.
     *
     * @param rawList whole list containing goods/bads.
     * @return limited list, with only acceptable values.
     */
    public Collection<MaskChanRenderableData> filter( Collection<MaskChanRenderableData> rawList ) {

        // For each data, read up its voxel count.
        FileResolver resolver = new CacheFileResolver();
        for ( MaskChanRenderableData data: rawList ) {
            String maskPath = data.getMaskPath();
            if ( maskPath != null ) {
                File infile = new File( resolver.getResolvedFilename( maskPath ) );
                if ( ! infile.canRead() ) {
                    logger.warn("Mask file {} cannot be read.", infile);
                }
                else {
                    try {
                        RenderableBean bean = data.getBean();
                        if ( bean.getVoxelCount() == 0L ) {
                            MaskSingleFileLoader loader = new MaskSingleFileLoader(bean);
                            FileInputStream fis = new FileInputStream( infile );
                            long voxelCount = loader.getVoxelCount( fis );
                            fis.close();
                            bean.setVoxelCount( voxelCount );
                        }

                    } catch ( Exception ex ) {
                        logger.error("Caught an exception while attempting to retrieve voxel count for {}.", maskPath );
                        ex.printStackTrace();
                    }

                }
            }
        }

        List<MaskChanRenderableData> sortedRenderableDatas = new ArrayList<MaskChanRenderableData>();
        sortedRenderableDatas.addAll( rawList );
        Collections.sort( sortedRenderableDatas, new RDComparator( false ) );

        // Now discard based on filtering criteria.
        List<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();
        int discardCount = 0;
        for ( MaskChanRenderableData data: sortedRenderableDatas ) {
            if ( filter(data) ) {
                rtnVal.add( data );
            }
            else {
                discardCount ++;
                logger.debug(
                        "Not keeping {}, entity {}, because it has too few voxels.",
                        data.getBean().getLabelFileNum(),
                        data.getBean().getRenderableEntity()
                );
            }

        }
        logger.debug( "Discarded {} renderables.", discardCount );

        return rtnVal;
    }

    private boolean filter(MaskChanRenderableData data) {
        boolean rtnVal = false;
        RenderableBean bean = data.getBean();

        // Filter-in here.
        if ( data.isCompartment() ) {
            rtnVal = true;
        }
        else {
            Long voxelCount = data.getBean().getVoxelCount();
            if ( voxelCount >= thresholdVoxelCount ) {
                rtnVal = true;

                // Filter for first-N neurons cutoff.
                if ( renDataBeanItemParentIdToChildCount != null ) {
                    rtnVal = false;
                    if ( bean.getRenderableEntity() != null ) {
                        AlignedItem alignedItem = AlignmentBoardMgr.getInstance().getLayersPanel()
                                .getAlignmentBoardContext().getAlignedItemWithEntityId(
                                        bean.getAlignedItemId()
                                );
                        if ( alignedItem != null ) {
                            EntityWrapper parent = alignedItem.getParent();
                            if ( parent != null ) {
                                long parentId = parent.getId();

                                Long countForParent = renDataBeanItemParentIdToChildCount.get( parentId );
                                if ( countForParent == null ) {
                                    countForParent = 0L;
                                }
                                if ( countForParent < thresholdNeuronCount ) {
                                    rtnVal = true;
                                    countForParent ++;
                                    renDataBeanItemParentIdToChildCount.put( parentId, countForParent );
                                }
                            }
                        }
                    }
                }

                if ( rtnVal ) {
                    logger.debug(
                            "Keeping {}, with {} voxels.",
                            data.getBean().getRenderableEntity(), voxelCount
                    );
                }
            }
        }

        return rtnVal;
    }

}
