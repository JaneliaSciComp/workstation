package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
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
    private long alignedItemId;
    private boolean invertedY;
    private String type;
    private Long voxelCount = 0L; // Never null.

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
        String typeName = entity.getEntityType().getName();
        if ( this.type == null ) {
            this.type = typeName;
        }
        if ( typeName.equals(EntityConstants.TYPE_NEURON_FRAGMENT) ) {
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
        if ( getRenderableEntity() != null )
            return getRenderableEntity().getId().hashCode();
        else
            return translatedNum;
    }

    public boolean isInvertedY() {
        return invertedY;
    }

    public void setInvertedY(boolean invertedY) {
        this.invertedY = invertedY;
    }

    public String getType() {
        return type;
    }

    /** Call this to override a simplistic type picked up from the entity itself. */
    public void setType( String type ) {
        this.type = type;
    }

    public Long getVoxelCount() {
        return voxelCount;
    }

    public void setVoxelCount(Long voxelCount) {
        this.voxelCount = voxelCount;
    }

    public long getAlignedItemId() {
        return alignedItemId;
    }

    public void setAlignedItemId(long alignedItemId) {
        this.alignedItemId = alignedItemId;
    }
}
