package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataChunk;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/19/13
 * Time: 1:20 PM
 *
 * This is a simplistic "bean" implementation of a volume data object.  Just allocate and set bytes.
 */
public class VolumeDataBean implements VolumeDataI {

    private VolumeDataChunk cachedChunk;
    private VolumeDataChunk[] cachedChunkArray;

    public VolumeDataBean( byte[] wholeVolume, int sX, int sY, int sZ ) {
        cachedChunk = new VolumeDataChunk();
        cachedChunk.setStartX( 0 );
        cachedChunk.setStartY( 0 );
        cachedChunk.setStartZ( 0 );
        cachedChunk.setWidth( sX );
        cachedChunk.setHeight( sY );
        cachedChunk.setDepth( sZ );
        cachedChunk.setData( wholeVolume );
        cachedChunkArray = new VolumeDataChunk[] { cachedChunk };
    }

    public VolumeDataBean( long size, int sX, int sY, int sZ ) {
        this( new byte[ (int)size ], sX, sY, sZ );
    }

    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    @Override
    public VolumeDataChunk[] getVolumeChunks() {
        return cachedChunkArray;
    }

    @Override
    public byte getValueAt(long location) {
        return cachedChunk.getData()[ (int)location ];
    }

    @Override
    public void setValueAt(long location, byte value) {
        cachedChunk.getData()[ (int)location ] = value;
    }

    @Override
    public long length() {
        return cachedChunk.getData().length;
    }
}
