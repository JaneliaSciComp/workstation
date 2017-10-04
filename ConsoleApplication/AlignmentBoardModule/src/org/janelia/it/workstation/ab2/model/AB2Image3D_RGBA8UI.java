package org.janelia.it.workstation.ab2.model;

public class AB2Image3D_RGBA8UI {

    private byte[] data;
    private int xDim;
    private int yDim;
    private int zDim;

    public AB2Image3D_RGBA8UI() {}

    public void allocate(int xDim, int yDim, int zDim) {
        this.xDim=xDim;
        this.yDim=yDim;
        this.zDim=zDim;
        allocate();
    }

    public AB2Image3D_RGBA8UI(int xDim, int yDim, int zDim) {
        allocate(xDim, yDim, zDim);
    }

    public byte[] getData() { return data; }

    public int getXDim() { return xDim; }

    public int getYDim() { return yDim; }

    public int getZDim() { return zDim; }

    private void allocate() {
        int dataLength=xDim*yDim*zDim;
        data=new byte[dataLength*4];
    }

    public void getVoxel(int x, int y, int z, byte[] voxel) {
        int xy=xDim*yDim;
        int offset=(z*xy+y*xDim+x)*4;
        voxel[0]=data[offset++];
        voxel[1]=data[offset++];
        voxel[2]=data[offset++];
        voxel[3]=data[offset++];
    }

    public void setVoxel(int x, int y, int z, byte[] voxel) {
        int xy=xDim*yDim;
        int offset=(z*xy+y*xDim+x)*4;
        data[offset++]=voxel[0];
        data[offset++]=voxel[1];
        data[offset++]=voxel[2];
        data[offset++]=voxel[3];
    }

    public void getImageZSlice(int z, byte[] slice) {
        int xy=xDim*yDim;
        int offset=z*xy*4;
        int s=0;
        for (int y=0;y<yDim;y++) {
            for (int x=0;x<xDim;x++) {
                slice[s]=data[offset++];
                slice[s+1]=data[offset++];
                slice[s+2]=data[offset++];
                slice[s+3]=data[offset++];
                s+=4;
            }
        }
    }

}
