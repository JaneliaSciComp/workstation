package org.janelia.it.FlyWorkstation.model.domain;

/**
 * An alignment context defines the combination of target alignment space and optical format. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentContext {

    private AlignmentSpace alignmentSpace;
    private String opticalResolution;
    private String pixelResolution;
    
    public AlignmentContext(AlignmentSpace alignmentSpace, String opticalResolution, String pixelResolution) {
        this.alignmentSpace = alignmentSpace;
        this.opticalResolution = opticalResolution;
        this.pixelResolution = pixelResolution;
    }
    
    public AlignmentSpace getAlignmentSpace() {
        return alignmentSpace;
    }

    public String getOpticalResolution() {
        return opticalResolution;
    }
    
    public String getPixelResolution() {
        return pixelResolution;
    }
}
