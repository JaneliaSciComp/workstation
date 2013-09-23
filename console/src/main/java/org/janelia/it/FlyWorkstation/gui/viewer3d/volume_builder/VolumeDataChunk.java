package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/23/13
 * Time: 10:13 AM
 *
 * This is a chunk-wise division of a mask or other volume.  It will have metadata within it telling how it may
 * be used, wrt the GPU.
 */
public class VolumeDataChunk {
    private byte[] data;
    private int startX;
    private int startY;
    private int startZ;
    private int width;
    private int height;
    private int depth;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

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


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
