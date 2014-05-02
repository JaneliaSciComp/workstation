package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.loader.MaskSingleFileLoader;
import org.janelia.it.jacs.shared.loader.file_resolver.FileResolver;
import org.janelia.it.jacs.shared.loader.renderable.MaskChanRenderableData;
import org.janelia.it.jacs.shared.loader.renderable.RDComparator;
import org.janelia.it.jacs.shared.loader.renderable.RenderableBean;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

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
    private Logger logger = Logger.getLogger(FragmentSizeSetterAndFilter.class);
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
                    logger.warn("Mask file " + infile + " cannot be read.");
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
                        logger.error("Caught an exception while attempting to retrieve voxel count for "+maskPath+".");
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
                if ( logger.isDebugEnabled() ) {
                    logger.debug(
                            "Not keeping "+data.getBean().getLabelFileNum()+", entity "+data.getBean().getRenderableEntity()+", because it has too few voxels."
                    );
                }
            }

        }
        logger.debug( "Discarded "+discardCount+" renderables." );

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
                        AlignedItem alignedItem = SessionMgr.getBrowser().getLayersPanel()
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

                if ( rtnVal  &&  logger.isDebugEnabled() ) {
                    logger.debug(
                            "Keeping "+data.getBean().getRenderableEntity()+", with "+voxelCount+" voxels."
                    );
                }
            }
        }

        return rtnVal;
    }

}
