package org.janelia.it.FlyWorkstation.gui.viewer3d.renderable;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/1/13
 * Time: 1:34 PM
 *
 * Holds information about a anything that can be rendered inside the alignment board.
 */
public class RenderableBean {
    private int labelFileNum = -1;
    private int translatedNum;
    private byte[] rgb;
    private Entity renderableEntity;
    private boolean invertedY;

    public int getLabelFileNum() {
        return labelFileNum;
    }

    public int getTranslatedNum() {
        return translatedNum;
    }

    public void setTranslatedNum(int translatedNum) {
        this.translatedNum = translatedNum;
    }

    public void setLabelFileNum( int labelFileNum ) {
        this.labelFileNum = labelFileNum;
    }

    public byte[] getRgb() {
        return rgb;
    }

    public void setRgb(byte[] rgb) {
        this.rgb = rgb;
    }

    public Entity getRenderableEntity() {
        return renderableEntity;
    }

    public void setRenderableEntity(Entity entity) {
        this.renderableEntity = entity;
        if ( entity.getEntityType().getName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
            String[] nameParts = entity.getName().trim().split(" ");
            // In establishing the label file number, must add one to account for 0-based neuron numbering
            // by name.  The number 0 cannot be used to represent a neuron, since it is needed for "nothing".
            // Therefore, there is a discrepancy between the naming and the numbering as done in the luminance file.
            if ( labelFileNum == -1 ) {
                labelFileNum = (Integer.parseInt( nameParts[ nameParts.length - 1 ] )) + 1;
            }
        }
    }

    /** Establish equality based on contained entity. */
    @Override
    public boolean equals( Object o ) {
        boolean rtnVal = false;
        if ( o != null  &&  o instanceof RenderableBean ) {
            RenderableBean other = (RenderableBean)o;
            if ( other.getRenderableEntity().getId().equals( getRenderableEntity().getId() ) ) {
                rtnVal = true;
            }
        }

        return rtnVal;
    }

    /** Hash based on contained entity. */
    @Override
    public int hashCode() {
        return getRenderableEntity().getId().hashCode();
    }

    public boolean isInvertedY() {
        return invertedY;
    }

    public void setInvertedY(boolean invertedY) {
        this.invertedY = invertedY;
    }
}
