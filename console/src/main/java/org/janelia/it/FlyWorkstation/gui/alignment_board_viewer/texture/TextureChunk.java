package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/21/13
 * Time: 12:35 PM
 *
 * This allows the break up of texture arrays into manageable chunks.  Java has a limitation of Integer.MAX_VALUE
 * for array sizes, hence this is required as long as very large textures are handled by the viewer.
 *
 * @deprecated now attempting to downsample textures to avoid their being this large.
 */
public class TextureChunk {
    // A chunk spans from a coordinate triple to (inclusive) another coordinate triple.
    private byte[] textureData;
    private int startX;
    private int startY;
    private int startZ;

    private int endX;
    private int endY;
    private int endZ;

    private int bytesPerCell = 1;

    private TextureChunk nextChunk;

    /** Texture Data includes all bytes from this range. */
    public byte[] getTextureData() {
        return textureData;
    }

    public void setTextureData(byte[] textureData) {
        this.textureData = textureData;
    }

    /** X, Y, Z starting values tell where this chunk begins. */
    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) {
        this.startY = startY;
    }

    public int getStartZ() {
        return startZ;
    }

    public void setStartZ(int startZ) {
        this.startZ = startZ;
    }

    /** X, Y, Z ending values tell where this chunk ends--inclusively. */
    public int getEndX() {
        return endX;
    }

    public void setEndX(int endX) {
        this.endX = endX;
    }

    public int getEndY() {
        return endY;
    }

    public void setEndY(int endY) {
        this.endY = endY;
    }

    public int getEndZ() {
        return endZ;
    }

    public void setEndZ(int endZ) {
        this.endZ = endZ;
    }

    /** This should be calculable given the size of the texture data, and the differences between start/end. */
    public int getBytesPerCell() {
        return bytesPerCell;
    }

    public void setBytesPerCell(int bytesPerCell) {
        this.bytesPerCell = bytesPerCell;
    }

    /** This supports making a linked list of chunks, and keeping them in order, so no Collection is needed. */
    public TextureChunk getNextChunk() {
        return nextChunk;
    }

    public void setNextChunk(TextureChunk nextChunk) {
        this.nextChunk = nextChunk;
    }
}
