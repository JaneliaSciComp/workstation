package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.jacs.shared.loader.AbstractAcceptorDecorator;
import org.janelia.it.jacs.shared.loader.ChannelMetaData;
import org.janelia.it.jacs.shared.loader.MaskChanDataAcceptorI;

import java.util.Map;

/**
 * Constrains output only to surface-dwelling voxels.  Such are non-obstructed on at least one side.
 *
 * Created by fosterl on 3/27/14.
 */
public class SurfaceOnlyAcceptorDecorator extends AbstractAcceptorDecorator {
    private Map<Integer,VoxelSurfaceCollector> voxelSurfaceCollectorMap;

    public SurfaceOnlyAcceptorDecorator(MaskChanDataAcceptorI wrappedAcceptor, Map<Integer,VoxelSurfaceCollector> voxelSurfaceCollector) {
        setWrappedAcceptor( wrappedAcceptor );
        this.voxelSurfaceCollectorMap = voxelSurfaceCollector;
    }

    @Override
    public int addChannelData(Integer maskNumber, byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != Acceptable.mask ) {
            VoxelSurfaceCollector surfaceCollector = voxelSurfaceCollectorMap.get( maskNumber );
            VoxelInfoBean voxelBean = surfaceCollector.getVoxelBean(x, y, z);
            if ( voxelBean != null   &&  voxelBean.isExposed() ) {
                return wrappedAcceptor.addChannelData( maskNumber, channelData, position, x, y, z, channelMetaData );
            }
        }
        return 1;
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        if ( wrappedAcceptor.getAcceptableInputs() != Acceptable.channel ) {
            VoxelSurfaceCollector surfaceCollector = voxelSurfaceCollectorMap.get( maskNumber );
            VoxelInfoBean voxelBean = surfaceCollector.getVoxelBean(x, y, z);
            if ( voxelBean != null  &&  voxelBean.isExposed() ) {
                return wrappedAcceptor.addMaskData(maskNumber, position, x, y, z);
            }
        }
        return 1;
    }
}
