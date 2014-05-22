package org.janelia.it.workstation.model.viewer;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.io.File;

/**
 * A masked volume is a combination of a single reference channel and 3 signal channels (red, green, and blue), along
 * with a set of exclusive masks across the signal volume. This API can provide access to the underlying volumes, as
 * well as "fast load" volumes at different subsampling factors, bitrates, and lossless/lossy compressions. 
 * 
 *  Neuron Separation results are currently stored as MaskedVolumes. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskedVolume {

    public enum ArtifactType {
        ConsolidatedSignal,
        ConsolidatedLabel,
        Reference
    }
    
    public enum Channels {
        All,
        Red,
        Green,
        Blue
    }
    
    public enum Size {
        Full(0),
        Subsampled_25mv(25),
        Subsampled_50mv(50),
        Subsampled_100mv(100);
        private int megaVoxels;
        Size(int megaVoxels) {
            this.megaVoxels = megaVoxels;
        }
        public int getMegaVoxels() {
            return megaVoxels;
        }
    }
    
    public static final String COLOR_RED = "Red";
    public static final String COLOR_GREEN = "Green";
    public static final String COLOR_BLUE = "Blue";
    
    private String baseDir;
    private String fastLoadDir;
    private String archiveFastLoadDir;
    
    public MaskedVolume(String baseDir) {
        this.baseDir = baseDir;
        this.fastLoadDir = baseDir + "/fastLoad";
        this.archiveFastLoadDir = baseDir + "/archive/fastLoad";
    }
    
    public String getSignalVolumePath() {
        return baseDir+"/ConsolidatedSignal.v3dpbd";
    }
    
    public String getSignalLabelPath() {
        return baseDir+"/ConsolidatedLabel.v3dpbd";
    }
    
    public String getReferenceVolumePath() {
        return baseDir+"/Reference.v3dpbd";
    }
    
    public String getFastVolumePath(ArtifactType type, Size size, Channels channels, boolean lossless) {
        if (type==ArtifactType.ConsolidatedLabel && channels!=Channels.All) {
            throw new IllegalArgumentException("ConsolidatedLabel is only available for All channels");
        }
        if (type==ArtifactType.ConsolidatedLabel && !lossless) {
            throw new IllegalArgumentException("ConsolidatedLabel is only available in lossless format");
        }
        if (type==ArtifactType.Reference && channels!=Channels.All) {
            throw new IllegalArgumentException("Reference is only available with all (1) channels");
        }
        String colorStr = (channels == Channels.All) ? "" : channels.toString();
        String sizeStr = (size == Size.Full) ? "" : "_"+size.getMegaVoxels();
        if (lossless) {
            return archiveFastLoadDir+"/"+type+"2"+colorStr+sizeStr+".v3dpbd";
        }
        else {
            return fastLoadDir+"/"+type+"2"+colorStr+sizeStr+".mp4";
        }
    }

    public String getFastMetadataPath(ArtifactType type, Size size) {
        String sizeStr = (size == Size.Full) ? "" : "_"+size.getMegaVoxels();
        return fastLoadDir+"/"+type+"2"+sizeStr+".metadata";
    }

    /**
     * Test harness
     */
    
    private static int confirmed = 0;
    
    private static final void confirm(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            System.out.println("  exists\t"+file);
            confirmed++;
        }
        else {
            System.out.println("  MISSING\t"+file);
        }
    }
    
    public static void main(String[] args) {
        
        String baseDir = SystemConfigurationProperties.getString("FileStore.CentralDir")+"/leetlab/Separation/900/834/1834683205718900834/separate";
        MaskedVolume vol = new MaskedVolume(baseDir);
        confirm(vol.getReferenceVolumePath());
        confirm(vol.getSignalVolumePath());
        confirm(vol.getSignalLabelPath());
        
        System.out.println("Confirm 8-bit volumes");
        confirm(vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, Size.Full, Channels.All, true));
        confirm(vol.getFastVolumePath(ArtifactType.ConsolidatedLabel, Size.Full, Channels.All, true));
        confirm(vol.getFastVolumePath(ArtifactType.Reference, Size.Full, Channels.All, true));

        System.out.println("Confirm subsampled volumes");
        for(Size size : Size.values()) {
            confirm(vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, size, Channels.All, true));
            confirm(vol.getFastVolumePath(ArtifactType.ConsolidatedLabel, size, Channels.All, true));
            confirm(vol.getFastVolumePath(ArtifactType.Reference, size, Channels.All, true));
        }

        System.out.println("Confirm mpeg4 volumes");
        for(Size size : Size.values()) {
            for(Channels channels : Channels.values()) {
                confirm(vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, size, channels, false));
            }
            confirm(vol.getFastVolumePath(ArtifactType.Reference, size, Channels.All, false));
        }
        
        System.out.println("Confirm metadata files");
        for(Size size : Size.values()) {
            confirm(vol.getFastMetadataPath(ArtifactType.ConsolidatedSignal, size));
            confirm(vol.getFastMetadataPath(ArtifactType.Reference, size));
        }
        
        System.out.println("Total files confirmed: "+confirmed);
    }
}
