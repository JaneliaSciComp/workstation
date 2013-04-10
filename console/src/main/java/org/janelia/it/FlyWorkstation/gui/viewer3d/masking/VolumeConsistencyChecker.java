package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/27/13
 * Time: 11:27 AM
 *
 * Accumulates "statistics" from volumes that need to have consistent constituents, and then allows
 * a report to be created if those statisitics vary.
 */
public class VolumeConsistencyChecker {
    // Final sanity check information, gleaned from all reads.
    private Map<Integer,Long[]> dimensions = new HashMap<Integer,Long[]>();
    private Map<Integer,ChannelMetaData> channelMetaDatas = new HashMap<Integer,ChannelMetaData>();

    /**
     * Add a dimension to this.
     *
     * @param renderableLabel this is a tag for reporting
     * @param dimensions x,y,z expected.
     * @param channelMetaData other info relating to channel.
     */
    public void accumulate( Integer renderableLabel, Long[] dimensions, ChannelMetaData channelMetaData ) {
        if ( dimensions != null ) {
            this.dimensions.put( renderableLabel, dimensions );
        }
        if ( channelMetaData != null ) {
            this.channelMetaDatas.put( renderableLabel, channelMetaData );
        }
    }

    /**
     * This sends a report to the logger provided.
     *
     * @param exceptOnInconsistency if TRUE, throw an exception if anything fails.
     * @param logger used to accept the report.
     */
    public void report( boolean exceptOnInconsistency, Logger logger ) {
        boolean isConsistent = true;
        if ( dimensions.size() > 0 ) {
            // Look at ANY dimensions, and set that as the "correct" to test all others against it.
            Long[] consensusDimensions = null;
            for ( Integer key: dimensions.keySet() ) {
                Long[] nextDims = dimensions.get( key );
                if ( consensusDimensions == null ) {
                    consensusDimensions = nextDims;
                }
                else {
                    for ( int i = 0; i < consensusDimensions.length; i++ ) {
                        if ( ! consensusDimensions[ i ].equals( nextDims[ i ] ) ) {
                            isConsistent = false;
                            logger.error( formatDimensionMismatchError( key, nextDims, consensusDimensions ) );
                        }
                    }
                }
            }

        }
        if ( channelMetaDatas.size() > 0 ) {
            ChannelMetaData consensusCMD = null;
            for ( Integer key: channelMetaDatas.keySet() ) {
                if ( consensusCMD == null ) {
                    consensusCMD = channelMetaDatas.get( key );
                }
                else {
                    ChannelMetaData nextCMD = channelMetaDatas.get( key );
                    if ( consensusCMD.byteCount != nextCMD.byteCount ) {
                        logger.info(
                                String.format( "Byte count of %d for renderable with target label of %d," +
                                        " mismatches consensus byte count of %d.",
                                        nextCMD.byteCount,
                                        key,
                                        consensusCMD.byteCount
                                )
                        );
                        isConsistent = false;
                    }
                    if ( consensusCMD.channelCount != nextCMD.channelCount ) {
                        logger.error(
                                String.format( "Channel count of %d for renderable with target label of %d," +
                                        " mismatches consensus channel count of %d.",
                                        nextCMD.channelCount,
                                        key,
                                        consensusCMD.channelCount
                                )
                        );
                        isConsistent = false;
                    }
                }
            }
        }

        if ( ! isConsistent  &&  exceptOnInconsistency ) {
            throw new RuntimeException( "Unexpected inconsistency in input.  See log." );
        }

    }

    /** Make a nice message for dimension-check. */
    private String formatDimensionMismatchError( int translatedNum, Long[] errantDimensions, Long[] correctDimensions ) {
        return String.format(
                "Dimensions ( %d x %d %d ) of renderable with target label of %d," +
                        " mismatch consensus dimensions of ( %d x %d x %d ).",
                errantDimensions[ 0 ],
                errantDimensions[ 1 ],
                errantDimensions[ 2 ],
                translatedNum,
                correctDimensions[ 0 ],
                correctDimensions[ 1 ],
                correctDimensions[ 2 ]
        );
    }

}
