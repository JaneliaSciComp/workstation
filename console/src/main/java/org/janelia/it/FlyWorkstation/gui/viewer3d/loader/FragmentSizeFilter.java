package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.model.domain.Masked3d;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/5/13
 * Time: 11:58 AM
 *
 * Given a collection of renderable beans, this will eliminate from that list, all whose voxel count is less than
 * some threshold.
 */
public class FragmentSizeFilter {
    private Logger logger = LoggerFactory.getLogger(FragmentSizeFilter.class);
    private long thresholdVoxelCount;

    public FragmentSizeFilter( long thresholdVoxelCount ) {
        this.thresholdVoxelCount = thresholdVoxelCount;
    }

    /**
     * Eliminates non-usables off the list.
     *
     * @param rawList whole list containing goods/bads.
     * @return limited list, with only acceptable values.
     */
    public Collection<MaskChanRenderableData> filter( Collection<MaskChanRenderableData> rawList ) {
        List<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();
        FileResolver resolver = new CacheFileResolver();
        int discardCount = 0;
        for ( MaskChanRenderableData data: rawList ) {
            // For each data, read up its voxel count.
            String maskPath = data.getMaskPath();
            // Certain cases need not be examined.
            if ( data.isCompartment() || maskPath == null ) {
                logger.debug( "Passing through {}.", maskPath );
                rtnVal.add( data );
            }
            else {
                if ( filterByFileSize( resolver, maskPath ) ) {
                    rtnVal.add( data );
                }
                else {
                    discardCount ++;
                    logger.debug(
                            "Not keeping {}, file {}, because it has too few voxels.",
                            data.getBean().getLabelFileNum(),
                            maskPath
                    );
                }

            }

        }
        logger.info( "Discarded {} renderables.", discardCount );

        return rtnVal;
    }

    /**
     * This filter method reduces the input list by the size given for this filter object.
     *
     * @param rawList what to check.
     * @return what remains after size taken into account.
     */
    public List<Neuron> filterNeurons(Collection<Neuron> rawList) {
        List<Neuron> rtnVal = new ArrayList<Neuron>();
        FileResolver resolver = new CacheFileResolver();
        for ( Neuron masked3d: rawList ) {
            if ( filterByFileSize( resolver, masked3d.getMask3dImageFilepath() ) ) {
                rtnVal.add( masked3d );
            }
        }
        return rtnVal;
    }

    /**
     * Filter a single neuron: tell if it is in or out.
     * @param neuron check this.
     * @return true=in; false=out
     */
    public boolean filterNeuron( Neuron neuron ) {
        FileResolver resolver = new CacheFileResolver();
        boolean rtnVal = false;
        if ( filterByFileSize( resolver, neuron.getMask3dImageFilepath() ) ) {
            rtnVal = true;
        }
        return rtnVal;
    }

    private boolean filterByFileSize( FileResolver resolver, String maskPath ) {
        File infile = new File( resolver.getResolvedFilename( maskPath ) );
        boolean rtnVal = false;
        if ( ! infile.canRead() ) {
            logger.warn("Mask file {} cannot be read.", infile );
        }
        else {
            try {
                MaskSingleFileLoader loader = new MaskSingleFileLoader( null, null, null, null );
                FileInputStream fis = new FileInputStream( infile );
                long voxelCount = loader.getVoxelCount( fis );

                // Filter-in here.
                if ( voxelCount >= thresholdVoxelCount ) {
                    rtnVal = true;
                    logger.debug(
                            "Keeping {}, with {} voxels.",
                            infile, voxelCount
                    );
                }

                fis.close();

            } catch ( Exception ex ) {
                logger.error("Caught an exception while attempting to retrieve voxel count for {}.", maskPath );
                ex.printStackTrace();
            }
        }
        return rtnVal;
    }

}
