package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/6/13
 * Time: 10:32 AM
 *
 * This is a source interface for volume data bytes.
 */
public interface VolumeDataI {
    /**
     * Call this to learn if any data may be fetched.  This is equivalent to checking if an array is null.
     *
     * @return True: may call getters below, for actual data.
     */
    boolean isVolumeAvailable();

    /**
     * Call this to get the volume data.  Do not cache this data, as it is meant to be dynamically
     * available/generated for use.  Also the implementation may use or enforce an order-of-calls
     * dependency.
     * @see #isVolumeAvailable() The return value may be null, if this method returns false.
     *
     * @return bytes for all volume data currently available.
     */
    byte[] getCurrentVolumeData();

    /**
     * Call this to reference a specific byte in the volume.  This is not to be used at "interpretation"
     * level. It is to be used just as if a byte array had been indexed.
     * @see #isVolumeAvailable() should return true just prior to this being called.
     *
     * @param location which offset, in bytes.  A long value is used to get beyond as many restrictions as possible.
     * @return the value stored there.
     */
    byte getCurrentValue( long location );

    /**
     * Call this to push one byte into the volume data.
     * @see #isVolumeAvailable() should return true just prior to this being called.
     *
     * @param location where to place the byte.
     * @param value what to place.
     */
    void setCurrentValue( long location, byte value );

    /**
     * Tells capacity of this volume data.
     * @see #isVolumeAvailable() should return true just prior to this being called.
     *
     * @return how long is the thing?
     */
    long length();
}
