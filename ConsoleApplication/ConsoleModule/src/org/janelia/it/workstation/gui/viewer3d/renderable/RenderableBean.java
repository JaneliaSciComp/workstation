package org.janelia.it.workstation.gui.viewer3d.renderable;

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
    private Long id;
    private String name;
    private boolean invertedY;
    private String type;
    private Long voxelCount = 0L; // Never null.
    private Object item;

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

    @Override
    public String toString() {
        return getName() + "::" + getId();
    }

    /** Establish equality based on contained entity. */
    @Override
    public boolean equals( Object o ) {
        boolean rtnVal = false;
        if ( o != null  &&  o instanceof RenderableBean ) {
            RenderableBean other = (RenderableBean)o;
            if ( other.getId().equals( getId() ) ) {
                rtnVal = true;
            }
        }

        return rtnVal;
    }

    /** Hash based on contained entity. */
    @Override
    public int hashCode() {
        if ( getId() != null )
            return getId().hashCode();
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
        // CAUTION: Dependency on name of class.
        if (type.equals(type.contains("Fragment"))) {
            String[] nameParts = name.trim().split(" ");
            // In establishing the label file number, must add one to account for 0-based neuron numbering
            // by name.  The number 0 cannot be used to represent a neuron, since it is needed for "nothing".
            // Therefore, there is a discrepancy between the naming and the numbering as done in the luminance file.
            if (labelFileNum == -1) {
                labelFileNum = (Integer.parseInt(nameParts[ nameParts.length - 1])) + 1;
            }
        }
    }

    public Long getVoxelCount() {
        return voxelCount;
    }

    public void setVoxelCount(Long voxelCount) {
        this.voxelCount = voxelCount;
    }

    public void setItem(Object item ) {
        this.item = item;
    }
    
    public Object getItem() {
        return item;
    }
    
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
}
