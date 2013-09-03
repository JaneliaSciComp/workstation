package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
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
     * @return limited list, witho only acceptable values.
     */
    public Collection<MaskChanRenderableData> filter( Collection<MaskChanRenderableData> rawList ) {
        List<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();
        FileResolver resolver = new CacheFileResolver();
        for ( MaskChanRenderableData data: rawList ) {
            // For each data, read up its voxel count.
            String maskPath = data.getMaskPath();
            // Certain cases need not be examined.
            if ( data.isCompartment() || maskPath == null ) {
                logger.debug( "Passing through {}.", maskPath );
                rtnVal.add( data );
            }
            else {
                File infile = new File( resolver.getResolvedFilename( maskPath ) );
                if ( ! infile.canRead() ) {
                    logger.warn("Mask file {} cannot be read.", infile );
                }
                else {
                    try {
                        MaskChanSingleFileLoader loader = new MaskChanSingleFileLoader( null, null, data.getBean() );
                        FileInputStream fis = new FileInputStream( infile );
                        long voxelCount = loader.getVoxelCount( fis );

                        // Filter-in here.
                        if ( voxelCount >= thresholdVoxelCount ) {
                            rtnVal.add( data );
                            logger.debug(
                                    "Keeping {}, with {} voxels.",
                                    infile, voxelCount
                            );
                        }
                        else {
                            logger.debug(
                                    "Not keeping {}, file {}, because it has too few voxels.",
                                    data.getBean().getLabelFileNum(),
                                    infile
                            );
                        }

                        fis.close();

                    } catch ( Exception ex ) {
                        logger.error("Caught an exception while attempting to retrieve voxel count for {}.", maskPath );
                        ex.printStackTrace();
                    }
                }
            }

        }

        return rtnVal;
    }
}
