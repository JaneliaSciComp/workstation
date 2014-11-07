/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.loader;

import java.nio.ByteOrder;

/**
 *
 * @author fosterl
 */
public abstract class AbstractVolumeFileLoader implements VolumeFileLoaderI {
    private int[] argbTextureIntArray;
    private byte[] textureByteArray;
    private int sx = -1, sy = -1, sz = -1;
    private int channelCount = 1; // Default for non-data-bearing file formats.
    private int pixelBytes = 1;
    private ByteOrder pixelByteOrder = ByteOrder.LITTLE_ENDIAN;
    private String unCachedFileName;
    private String header;

    /**
     * @return the argbTextureIntArray
     */
    public int[] getArgbTextureIntArray() {
        return argbTextureIntArray;
    }

    /**
     * @param argbTextureIntArray the argbTextureIntArray to set
     */
    public void setArgbTextureIntArray(int[] argbTextureIntArray) {
        this.argbTextureIntArray = argbTextureIntArray;
    }
    
    public void initArgbTextureIntArray() {
        this.argbTextureIntArray = new int[ sx * sy * sz ];
    }

    /**
     * @return the textureByteArray
     */
    public byte[] getTextureByteArray() {
        return textureByteArray;
    }

    /**
     * @param textureByteArray the textureByteArray to set
     */
    public void setTextureByteArray(byte[] textureByteArray) {
        this.textureByteArray = textureByteArray;
    }

    /**
     * @return the sx
     */
    public int getSx() {
        return sx;
    }

    /**
     * @param sx the sx to set
     */
    public void setSx(int sx) {
        this.sx = sx;
    }

    /**
     * @return the sy
     */
    public int getSy() {
        return sy;
    }

    /**
     * @param sy the sy to set
     */
    public void setSy(int sy) {
        this.sy = sy;
    }

    /**
     * @return the sz
     */
    public int getSz() {
        return sz;
    }

    /**
     * @param sz the sz to set
     */
    public void setSz(int sz) {
        this.sz = sz;
    }

    /**
     * @return the channelCount
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * @param channelCount the channelCount to set
     */
    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    /**
     * @return the pixelBytes
     */
    public int getPixelBytes() {
        return pixelBytes;
    }

    /**
     * @param pixelBytes the pixelBytes to set
     */
    public void setPixelBytes(int pixelBytes) {
        this.pixelBytes = pixelBytes;
    }

    /**
     * @return the pixelByteOrder
     */
    public ByteOrder getPixelByteOrder() {
        return pixelByteOrder;
    }

    /**
     * @param pixelByteOrder the pixelByteOrder to set
     */
    public void setPixelByteOrder(ByteOrder pixelByteOrder) {
        this.pixelByteOrder = pixelByteOrder;
    }

    /**
     * @return the unCachedFileName
     */
    public String getUnCachedFileName() {
        return unCachedFileName;
    }

    /**
     * @param unCachedFileName the unCachedFileName to set
     */
    public void setUnCachedFileName(String unCachedFileName) {
        this.unCachedFileName = unCachedFileName;
    }

    /**
     * @return the header
     */
    public String getHeader() {
        return header;
    }

    /**
     * @param header the header to set
     */
    public void setHeader(String header) {
        this.header = header;
    }

}
