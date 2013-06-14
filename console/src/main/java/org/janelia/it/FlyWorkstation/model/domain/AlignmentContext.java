package org.janelia.it.FlyWorkstation.model.domain;

/**
 * An alignment context defines the combination of target alignment space and optical format. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentContext {

    private String alignmentSpaceName;
    private String opticalResolution;
    private String pixelResolution;
    
    public AlignmentContext(String alignmentSpaceName, String opticalResolution, String pixelResolution) {
        this.alignmentSpaceName = alignmentSpaceName;
        this.opticalResolution = opticalResolution;
        this.pixelResolution = pixelResolution;
    }
    
    public String getAlignmentSpaceName() {
        return alignmentSpaceName;
    }

    public String getOpticalResolution() {
        return opticalResolution;
    }
    
    public String getPixelResolution() {
        return pixelResolution;
    }
    
    public boolean canDisplay(AlignmentContext alignmentContext) {
        return (alignmentSpaceName.equals(alignmentContext.getAlignmentSpaceName()) 
                && opticalResolution.equals(alignmentContext.getOpticalResolution())
                && pixelResolution.equals(alignmentContext.getPixelResolution()));
    }

    @Override
    public String toString() {
        return getAlignmentSpaceName()+" "+getOpticalResolution()+" "+getPixelResolution();
    }
}
