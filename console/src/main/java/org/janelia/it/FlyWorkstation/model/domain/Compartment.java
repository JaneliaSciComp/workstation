package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 5/3/13
 * Time: 12:40 PM
 *
 * This represents an alignment-space-specific compartment, or standardized sub volume suitable for gauging spacial
 * relationships when presented in a viewer, with other things aligned to the same space.
 */
public class Compartment extends EntityWrapper implements Viewable2d, Masked3d, MaskIndexed {

    public Compartment( RootedEntity compartmentEntity ) {
        super( compartmentEntity );
    }

    @Override
    public Integer getMaskIndex() {
        String value = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (StringUtils.isEmpty(value)) return null;
        return Integer.parseInt(value)+1;
    }

    @Override
    public String getMask3dImageFilepath() {
        return getFilePath(EntityConstants.ATTRIBUTE_MASK_IMAGE);
    }

    @Override
    public String getChan3dImageFilepath() {
        return getFilePath(EntityConstants.ATTRIBUTE_CHAN_IMAGE);
    }

    @Override
    public String get2dImageFilepath() {
        return null;
    }

    public int[] getColor() {
        int[] rtnVal = null;

        String colorString = getInternalEntity().getValueByAttributeName( EntityConstants.ATTRIBUTE_COLOR );
        if ( colorString != null  &&  colorString.length() == 6 ) {
            rtnVal = new int[ 3 ];
            for ( int i = 0; i < rtnVal.length; i++ ) {
                String nextHexNum = colorString.substring( i * 2, (i * 2) + 2 );
                rtnVal[ i ] = Integer.parseInt( nextHexNum, 16 );
            }
        }

        return rtnVal;
    }

    private String getFilePath( String attribName ) {
        String rtnVal = getInternalEntity().getValueByAttributeName( attribName );
        if ( rtnVal == null ) {
            EntityData childData = getInternalEntity().getEntityDataByAttributeName( attribName );
            Entity childEntity = childData.getChildEntity();
            rtnVal = childEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_FILE_PATH );
        }
        return rtnVal;
    }
}
