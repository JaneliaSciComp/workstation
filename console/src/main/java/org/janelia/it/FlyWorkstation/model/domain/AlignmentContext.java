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
        if ( alignmentSpaceName == null || opticalResolution == null || pixelResolution == null ) {
            throw new IllegalArgumentException( "No nulls in constructor." );
        }
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
    @Override
    public boolean equals( Object other ) {
        boolean rtnVal;
        if ( other == null || ! ( other instanceof AlignmentContext ) ) {
            rtnVal = false;
        }
        else {
            AlignmentContext otherAlignmentSpace = (AlignmentContext)other;
            rtnVal = otherAlignmentSpace.getAlignmentSpaceName().equals( getAlignmentSpaceName() ) &&
                    otherAlignmentSpace.getPixelResolution().equals( getPixelResolution() ) &&
                    otherAlignmentSpace.getOpticalResolution().equals( getOpticalResolution() );
        }

        return rtnVal;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
