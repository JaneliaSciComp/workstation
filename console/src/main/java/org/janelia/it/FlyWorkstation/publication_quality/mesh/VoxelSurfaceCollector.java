package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.compute.access.loader.ChannelMetaData;
import org.janelia.it.jacs.compute.access.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;

/**
 * This accepts mask/chan data to create a collection of voxels.
 * Created by fosterl on 3/24/14.
 */
public class VoxelSurfaceCollector implements MaskChanDataAcceptorI {
    private Map<Long,Map<Long,Map<Long,VoxelInfoBean>>> xMap;
    private int channelCount = -1;

    public VoxelSurfaceCollector() {
        xMap = new HashMap<Long,Map<Long,Map<Long,VoxelInfoBean>>>();
    }

    @Override
    public synchronized int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        // Store this into the map for later use.
        getVoxelBeanHelper(x, y, z, true);
        return 1;
    }

    @Override
    public synchronized int addChannelData(Integer orignalMaskNum, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        VoxelInfoBean bean = getVoxelBeanHelper(x, y, z, true);
        if ( bean == null ) {
            throw new IllegalStateException( "Adding channel data before mask data to " + x + " " + y + " " + z );
        }
        channelCount = channelMetaData.channelCount;

        return 1;
    }

    @Override
    public void setSpaceSize(long x, long y, long z, long paddedX, long paddedY, long paddedZ, float[] coordCoverage) {
        // Do nothing
    }

    @Override
    public Acceptable getAcceptableInputs() {
        return Acceptable.both;
    }

    @Override
    public int getChannelCount() {
        return channelCount;
    }

    @Override
    public void setChannelMetaData(ChannelMetaData metaData) {
        // Do nothing.
    }

    @Override
    public void endData(Logger logger) {
        // Need browse submaps, exploring neighborhoods.
        for ( Map<Long,Map<Long,VoxelInfoBean>> yMaps: xMap.values() ) {
            for ( Map<Long,VoxelInfoBean> zMap: yMaps.values() ) {
                for ( VoxelInfoBean bean: zMap.values() ) {
                    long[][] neighborhood = bean.getNeighborhood();
                    int neighborPos = 0;
                    for ( long[] neighbor: neighborhood ) {
                        VoxelInfoBean neighborBean = getVoxelBean( neighbor[ 0 ], neighbor[ 1 ], neighbor[ 2 ] );
                        if ( neighborBean == null ) {
                            bean.setExposedFace(neighborPos);
                        }
                        neighborPos ++;
                    }
                }
            }
        }
    }

    /** Returns the data collected thus far. */
    public Map<Long,Map<Long,Map<Long,VoxelInfoBean>>> getVoxelMap() {
        return xMap;
    }
    public VoxelInfoBean getVoxelBean( long x, long y, long z ) {
        // Legitimate question, with always-null answer: -1 can be sentinel value for at-zero edge case.
        if ( x < 0  ||  y < 0  || z < 0 ) {
            return null;
        }
        return getVoxelBeanHelper(x, y, z, false);
    }

    //------------------------------------------------HELPERS
    private VoxelInfoBean getVoxelBeanHelper( long x, long y, long z, boolean createIfMissing ) {
        Map<Long,Map<Long,VoxelInfoBean>> yMap = xMap.get( x );
        if ( yMap == null ) {
            if ( createIfMissing ) {
                yMap = new HashMap<Long,Map<Long,VoxelInfoBean>>();
                xMap.put(x, yMap);
            }
            else {
                return null;
            }
        }

        Map<Long,VoxelInfoBean> zMap = yMap.get( y );
        if ( zMap == null ) {
            if ( createIfMissing ) {
                zMap = new HashMap<Long,VoxelInfoBean>();
                yMap.put(y, zMap);
            }
            else {
                return null;
            }
        }

        VoxelInfoBean bean = zMap.get( z );
        if ( bean == null ) {
            if ( createIfMissing ) {
                bean = new VoxelInfoBean();
                bean.setKey( new VoxelInfoKey( x, y, z ) );
                zMap.put(z, bean);
            }
            else {
                return null;
            }
        }
        return bean;
    }

}
