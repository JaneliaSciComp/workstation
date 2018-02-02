package org.janelia.it.workstation.ab2.model;

public class AB2Image3D_R32I {

    private int[] data;
    private int xDim;
    private int yDim;
    private int zDim;

    public AB2Image3D_R32I() {}

    public AB2Image3D_R32I(int xDim, int yDim, int zDim) {
        allocate(xDim, yDim, zDim);
    }

    public void allocate(int xDim, int yDim, int zDim) {
        this.xDim=xDim;
        this.yDim=yDim;
        this.zDim=zDim;
        allocate();
    }

    public int[] getData() { return data; }

    public int getXDim() { return xDim; }

    public int getYDim() { return yDim; }

    public int getZDim() { return zDim; }

    private void allocate() {
        int dataLength=xDim*yDim*zDim;
        data=new int[dataLength];
    }

    public int getVoxel(int x, int y, int z) {
        int xy=xDim*yDim;
        int offset=(z*xy+y*xDim+x);
        return data[offset];
    }

    public void setVoxel(int x, int y, int z, int value) {
        int xy=xDim*yDim;
        int offset=(z*xy+y*xDim+x);
        data[offset]=value;
    }

    public void getImageZSlice(int z, int[] slice) {
        int xy=xDim*yDim;
        int offset=z*xy;
        int s=0;
        for (int y=0;y<yDim;y++) {
            for (int x=0;x<xDim;x++) {
                slice[s++]=data[offset++];
            }
        }
    }

}
