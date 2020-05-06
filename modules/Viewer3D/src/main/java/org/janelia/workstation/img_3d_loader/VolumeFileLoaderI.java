package org.janelia.workstation.img_3d_loader;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:26 PM
 *
 * Implement this to load a volume file.
 */
public interface VolumeFileLoaderI {
    String CONSOLIDATED_SIGNAL_FILE = "ConsolidatedSignal2";
    String REFERENCE_FILE = "Reference2";
    String TIF_EXT = "TIF";
    String LSM_EXT = "LSM";
    String V3D_EXT = "V3D";
    String MP4_EXT = "MP4";
    String H264_EXT = "H264";
    String H265_EXT = "H265";
    String H5J_EXT  = "H5J";

    void loadVolumeFile(String fileName) throws Exception;
}
