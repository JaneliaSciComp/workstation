package org.janelia.workstation.img_3d_loader;

/**
 * Created by murphys on 5/17/2016.
 */
public interface FileByteSource {

    public byte[] loadBytesForFile(String filepath) throws Exception;

}
