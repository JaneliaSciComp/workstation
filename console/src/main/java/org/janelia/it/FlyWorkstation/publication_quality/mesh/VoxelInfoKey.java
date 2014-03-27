package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.Arrays;

/**
 * Key for referring to a voxel.  This key will be kept in a voxel-info bean to which it refers, as well as the
 * collection's (hash map's key-set).
 *
 * Created by fosterl on 3/24/14.
 */
public class VoxelInfoKey {
    private long[] position;

    public VoxelInfoKey( long x, long y, long z ) {
        setPosition( x, y, z );
    }

    /**
     * Equals impl to support use as key.
     *
     * @param other to compare with this one.
     * @return T if same position; F otherwise.
     */
    public boolean equals( Object other ) {
        if ( other ==  null  ||  !( other instanceof VoxelInfoKey) ) {
            return false;
        }
        else {
            VoxelInfoKey otherBean = (VoxelInfoKey)other;
            return Arrays.equals( position, otherBean.getPosition() );

        }
    }

    /**
     * Hash Code impl to support use as key.
     *
     * @return hash code of position triplet.
     */
    public int hashCode() {
        return Arrays.hashCode( position );
    }

    public long[] getPosition() {
        return position;
    }

    public void setPosition(long[] position) {
        this.position = position;
    }

    public void setPosition( long x, long y, long z ) {
        setPosition( new long[] {x, y, z} );
    }
}
