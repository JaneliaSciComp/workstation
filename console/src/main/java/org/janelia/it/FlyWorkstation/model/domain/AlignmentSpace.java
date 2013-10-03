package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * An alignment space is a defined 3d volume for alignments. Primary use for comparing two such spaces.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentSpace {

    private String name;
    private String opticalResolution;
    private String pixelResolution;

    public AlignmentSpace( String name, String opticalResolution, String pixelResolution ) {
        if ( name == null || opticalResolution == null || pixelResolution == null ) {
            throw new IllegalArgumentException( "No nulls in constructor." );
        }
        setName( name );
        setOpticalResolution( opticalResolution );
        setPixelResolution( pixelResolution );
    }

    public AlignmentSpace( AlignmentContext context ) {
        this( context.getAlignmentSpaceName(), context.getOpticalResolution(), context.getPixelResolution() );
    }

    public AlignmentSpace( Entity compartmentSetEntity ) {
        this(
                compartmentSetEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE ),
                compartmentSetEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION ),
                compartmentSetEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION )
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpticalResolution() {
        return opticalResolution;
    }

    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }

    public String getPixelResolution() {
        return pixelResolution;
    }

    public void setPixelResolution(String pixelResolution) {
        this.pixelResolution = pixelResolution;
    }

    @Override
    public String toString() {
        return "AlignmentSpace [name=" + getName() + ", optical resolution=" + getOpticalResolution() + ", pixel resolution=" + getPixelResolution() + "]";
    }

    @Override
    public boolean equals( Object other ) {
        boolean rtnVal = true;
        if ( other == null || ! ( other instanceof AlignmentSpace ) ) {
            rtnVal = false;
        }
        else {
            AlignmentSpace otherAlignmentSpace = (AlignmentSpace)other;
            rtnVal = otherAlignmentSpace.getName().equals( getName() ) &&
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
