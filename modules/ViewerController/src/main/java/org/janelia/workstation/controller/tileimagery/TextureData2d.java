package org.janelia.workstation.controller.tileimagery;


import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by murphys on 5/13/2016.
 */
public class TextureData2d {

    // Direct glTexImage argument properties
    protected int mipmapLevel = 0;
    protected int width = 0; // padded to a multiple of 8
    protected int usedWidth = 0; // possibly odd original image width
    protected int height = 0;
    protected int border = 0;
    protected ByteBuffer pixels = null; // prefer direct buffer; array backed buffer works too

    // Derived properties
    protected boolean srgb; // vs. linear
    protected int bitDepth = 8;
    protected int channelCount = 3;
    protected float textureCoordX = 1.0f;

    public void loadRenderedImage(RenderedImage image) {
        ColorModel colorModel = image.getColorModel();
        // If input image uses indexed color table, convert to RGB first.
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel indexColorModel = (IndexColorModel) colorModel;
            image = indexColorModel.convertToIntDiscrete(image.getData(), false);
            colorModel = image.getColorModel();
        }
        this.width = this.usedWidth = image.getWidth();
        // pad image to a multiple of 8
        textureCoordX = 1.0f;
        if ((this.width % 8) != 0) {
            int dw = 8 - (this.width % 8);
            this.width += dw;
            textureCoordX = this.usedWidth / (float)this.width;
        }
        this.height = image.getHeight();
        this.srgb = colorModel.getColorSpace().isCS_sRGB();
        this.channelCount = colorModel.getNumComponents();
        this.bitDepth = colorModel.getPixelSize() / this.channelCount;
        // treat indexed image as rgb
        if (this.bitDepth < 8)
            this.bitDepth = 8;
        assert((this.bitDepth == 8) || (this.bitDepth == 16));
        int pixelByteCount = this.channelCount * this.bitDepth/8;
        int rowByteCount = pixelByteCount * this.width;
        int imageByteCount = this.height * rowByteCount;
        // Allocate image store buffer, exactly as it will be passed to openGL
        // TODO - Consider sharing a buffer among multiple textures to save allocation time.
        // (there would need to be a separate one for each thread)
        byte byteArray[] = new byte[imageByteCount];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.nativeOrder());
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer(); // for 16-bit case
        //
        Raster raster = image.getData();
        int pixelData[] = new int[this.channelCount];
        int padData[] = new int[this.channelCount]; // color for edge padding
        final boolean is16Bit = (this.bitDepth == 16);
        if (is16Bit) {
            for (int y = 0; y < this.height; ++y) {
                // Choose ragged right edge pad color from right
                // edge of used portion of scan line.
                raster.getPixel(this.usedWidth-1, y, padData);
                for (int x = 0; x < this.width; ++x) {
                    if (x < this.usedWidth) { // used portion of scan line
                        raster.getPixel(x, y, pixelData);
                        for (int i : pixelData) {
                            shortBuffer.put((short)i);
                        }
                    } else { // (not zero) pad right edge
                        for (int i : padData) {
                            shortBuffer.put((short)i);
                        }
                    }
                }
            }
        } else { // 8-bit
            for (int y = 0; y < this.height; ++y) {
                raster.getPixel(this.usedWidth-1, y, padData);
                for (int x = 0; x < this.width; ++x) {
                    if (x < this.usedWidth) {
                        raster.getPixel(x, y, pixelData);
                        for (int i : pixelData) {
                            byteBuffer.put((byte)i);
                        }
                    } else { // zero pad right edge
                        for (int i : padData) {
                            byteBuffer.put((byte)i);
                        }
                    }
                }
            }
        }
        pixels = byteBuffer;
        return;
    }

    public void releaseMemory() {
        width = height = usedWidth = 0;
        pixels = null;
    }

    public void setBitDepth(int bitDepth) {
        this.bitDepth = bitDepth;
    }

    public void setWidth(int width) {
        this.width = width;
        if (width != 0)
            textureCoordX = this.usedWidth / (float)this.width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setPixels(ByteBuffer pixels) {
        this.pixels = pixels;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public int getMipmapLevel() {
        return mipmapLevel;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBorder() {
        return border;
    }

    public int getUsedWidth() {
        return usedWidth;
    }

    public void setUsedWidth(int usedWidth) {
        this.usedWidth = usedWidth;
        if (width != 0)
            textureCoordX = this.usedWidth / (float)this.width;
    }

    public ByteBuffer getPixels() {
        return pixels;
    }

    public boolean isSrgb() {
        return srgb;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public ImageBrightnessStats getBrightnessStats() {
        ByteBuffer bb = getPixels();
        if (bb == null)
            return null;
        if (bb.capacity() < 1)
            return null;
        if (height*width*channelCount < 1)
            return null;
        ImageBrightnessStats result = new ImageBrightnessStats();
        bb.rewind();
        // Initialize channel statistics
        for (int c = 0; c < channelCount; ++c)
            result.add(new ChannelBrightnessStats());
        // Read pixel values
        ShortBuffer buf16 = bb.asShortBuffer(); // ...which might be 16-bit values...
        // First set min/max
        for (int c = 0; c < channelCount; ++c) {
            ChannelBrightnessStats chanStats = result.get(c);
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int val = 0;
                    if (getBitDepth() > 8)
                        val = (buf16.get() & 0xffff); // unsigned 16 bit value
                    else
                        val = (bb.get() & 0xff); // unsigned 8 bit value
                    if (val == 0)
                        continue; // zero means "no data"
                    chanStats.setMax(Math.max(chanStats.getMax(), val));
                    chanStats.setMin(Math.min(chanStats.getMin(), val));
                }
            }
        }
        bb.rewind();
        buf16.rewind();
        // Next set histogram, now that min/max are set
        for (int c = 0; c < channelCount; ++c) {
            ChannelBrightnessStats chanStats = result.get(c);
            chanStats.clearHistogram();
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int val = 0;
                    if (getBitDepth() > 8)
                        val = (buf16.get() & 0xffff); // unsigned 16 bit value
                    else
                        val = (bb.get() & 0xff); // unsigned 8 bit value
                    if (val == 0)
                        continue; // zero means "no data"
                    chanStats.updateHistogram(val, 1);
                }
            }
        }
        bb.rewind();
        return result;
    }

    // This method takes 4ms, vs 40ms for the above more general version
    public void load8bitStackSliceByteBufferTo16bitTexture(int xSize, int ySize, int zSize, int cSize, int zSlice, int ushortOffset, int ushortRange, ByteBuffer sourceBuffer) {
        try {
            this.width = this.usedWidth = xSize;
            // pad image to a multiple of 8
            textureCoordX = 1.0f;
            if ((this.width % 8) != 0) {
                int dw = 8 - (this.width % 8);
                this.width += dw;
                textureCoordX = this.usedWidth / (float) this.width;
            }
            this.height = ySize;
            this.srgb = false;
            this.channelCount = cSize;
            this.bitDepth = 16;
            int pixelByteCount = channelCount * bitDepth / 8;
            int rowByteCount = pixelByteCount * this.width;
            int imageByteCount = this.height * rowByteCount;

            ByteBuffer byteBuffer=ByteBuffer.allocateDirect(imageByteCount);
            byteBuffer.order(ByteOrder.nativeOrder());
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
            //short[] targetArray = shortBuffer.array();

            int padData[] = new int[channelCount]; // color for edge padding
            byte[] sourceArr = sourceBuffer.array();
            int channelOffset = xSize * ySize * zSize;
            int zOffset = xSize * ySize * zSlice;
            int[] czOffsetArr = new int[cSize];
            for (int c = 0; c < cSize; c++) {
                czOffsetArr[c] = channelOffset * c + zOffset;
            }
            //log.info("Check1");
            for (int y = 0; y < height; ++y) {
                int ySourceOffset = y * usedWidth;
                int yTargetOffset = y * width;
                int lastX = usedWidth - 1;
                for (int c = 0; c < cSize; c++) {
                    padData[c] = (short) (((sourceArr[czOffsetArr[c] + ySourceOffset + lastX] + 128) * ushortRange) / 256 + ushortOffset);
                }
                for (int x = 0; x < width; ++x) {
                    int xSourceOffset = ySourceOffset + x;
                    int xTargetOffset = yTargetOffset + x;
                    if (x < usedWidth) { // used portion of scan line
                        for (int c = 0; c < cSize; c++) {
							/*targetArray[xTargetOffset + c] =*/ shortBuffer.put( (short) (((sourceArr[czOffsetArr[c] + xSourceOffset] + 128) * ushortRange) / 256 + ushortOffset) );
                        }
                    } else {
                        for (int c = 0; c < cSize; c++) {
							/*targetArray[xTargetOffset + c] =*/ shortBuffer.put( (short) padData[c] );
                        }
                    }
                }
            }
            //log.info("Check2");
            pixels = byteBuffer;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    public byte[] copyToByteArray() {
        int byteBufferSize = (Integer.SIZE / 8) * 8 + (Float.SIZE / 8) + pixels.capacity();
        byte[] textureData2dArray=new byte[byteBufferSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(textureData2dArray);
        byteBuffer.putInt(mipmapLevel);
        byteBuffer.putInt(width);
        byteBuffer.putInt(usedWidth);
        byteBuffer.putInt(height);
        byteBuffer.putInt(border);
        byteBuffer.putInt(srgb ? 1 : 0);
        byteBuffer.putInt(bitDepth);
        byteBuffer.putInt(channelCount);
        byteBuffer.putFloat(textureCoordX);
        byteBuffer.put(pixels);
        return textureData2dArray;
    }

    TextureData2d() {}

    TextureData2d(byte[] bytes) {
        ByteBuffer byteBuffer=ByteBuffer.wrap(bytes);
        mipmapLevel=byteBuffer.getInt();
        width=byteBuffer.getInt();
        usedWidth=byteBuffer.getInt();
        height=byteBuffer.getInt();
        border=byteBuffer.getInt();
        int srgbProxy=byteBuffer.getInt();
        if (srgbProxy>0) {
            srgb=true;
        } else {
            srgb=false;
        }
        bitDepth=byteBuffer.getInt();
        channelCount=byteBuffer.getInt();
        textureCoordX=byteBuffer.getFloat();
        int remainingBytes=byteBuffer.remaining();
        byte[] pixelArray=new byte[remainingBytes];
        byteBuffer.get(pixelArray);
        pixels=ByteBuffer.wrap(pixelArray);
    }

}
