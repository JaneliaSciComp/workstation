
package org.janelia.gltools.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import org.janelia.gltools.GL3Resource;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Texture2d extends BasicTexture implements GL3Resource 
{
    protected int height = 0;
    
    public Texture2d() {
        textureTarget = GL.GL_TEXTURE_2D;
    }

    public int getHeight() {
        return height;
    }

    @Override
    protected void uploadTexture(GL3 gl) 
    {
        gl.glTexSubImage2D(
                textureTarget,
                mipMapLevel,
                0, 0,// offsets
                width, height,
                format,
                type,
                pixels);
        needsUpload = false;
    }
    
    @Override
    protected void allocateTextureStorage(GL3 gl, int mipmapCount) {
        if (useImmutableTexture) {
            gl.glTexStorage2D(textureTarget, 
                    mipmapCount, 
                    internalFormat, 
                    width, height);
        } else {
            gl.glTexImage2D(textureTarget,
                    mipMapLevel,
                    internalFormat, 
                    width, height,
                    border,
                    format,
                    type,
                    null);
        }
    }    
    
    @Override
    protected int maxDimension() {
        return Math.max(width, height);
    }
    
    private String parseToken(PushbackInputStream inStream) throws IOException {
        StringBuilder s = new StringBuilder();
        int c = inStream.read();
        // skip white space
        while (Character.isWhitespace((char)c))
            c = inStream.read();
        while (! Character.isWhitespace((char)c)) {
            s.append((char)c);
            c = inStream.read();
        }
        inStream.unread(c);
        return s.toString();
    }
    
    private String skipLine(PushbackInputStream inStream) throws IOException {
        StringBuilder s = new StringBuilder();
        char c = (char) inStream.read();
        while ( c != '\n') {
            c = (char) inStream.read();
            s.append(c);
        }
        return s.toString();
    }
    
    public Texture2d loadFromBufferedImage(BufferedImage bi)
    {
        width = bi.getWidth();
        height = bi.getHeight();
        bytesPerIntensity = bi.getColorModel().getComponentSize(0) / 8;
        if (bytesPerIntensity > 1)
            type = GL.GL_UNSIGNED_SHORT;
        else
            type = GL.GL_UNSIGNED_BYTE;
        numberOfComponents = bi.getColorModel().getNumComponents();        
        int byteCount = numberOfComponents * bytesPerIntensity * width * height;
        byte[] pixelBytes = new byte[byteCount];
        pixels = ByteBuffer.wrap(pixelBytes);
        pixels.rewind();
        
        switch (numberOfComponents ) 
        {
            case 1:
                format = internalFormat = GL.GL_LUMINANCE;
                break;
            case 2:
                format = internalFormat = GL.GL_LUMINANCE_ALPHA;
                break;
            case 3:
                format = internalFormat = GL.GL_RGB;
                break;
            case 4:
                format = internalFormat = GL.GL_RGBA;
                break;
        }
        
        int[] intPixels = new int[width * height];
        bi.getRGB(0, 0, width, height, intPixels, 0, width);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int rgb = intPixels[x + y*width];
                pixels.put((byte) ((rgb >> 16) & 0xFF)); // red
                if (numberOfComponents < 2) continue;
                pixels.put((byte) ((rgb >> 8) & 0xFF)); // green
                pixels.put((byte) (rgb & 0xFF)); // blue
                if (numberOfComponents < 4) continue;
                pixels.put((byte) ((rgb >> 24) & 0xFF)); // alpha
            }
        }
        
        pixels.flip();
        needsUpload = true;
        return this;
    }
    
    public void loadFromPpm(InputStream inputStream) throws IOException
    {
        PushbackInputStream inStream = new PushbackInputStream(inputStream);
        
        // "P6"
        String magicCookie = parseToken(inStream);
        // System.out.println("#"+magicCookie+"#");
        if (! magicCookie.equals("P6"))
            throw new IOException( "not a PPM_RAW file" );
        
        String field = parseToken(inStream);
        // Skip comments
        while (field.startsWith("#")) {
            String commentLine = skipLine(inStream);
            // System.out.println("***: #"+commentLine+":***");
            field = parseToken(inStream);
        }
        width = Integer.parseInt(field);
        field = parseToken(inStream);
        height = Integer.parseInt(field);
        field = parseToken(inStream);
        int maxVal = Integer.parseInt(field);
        if (maxVal > 255) {
            bytesPerIntensity = 2;
            type = GL.GL_UNSIGNED_SHORT;
        }
        else {
            bytesPerIntensity = 1;
            type = GL.GL_UNSIGNED_BYTE;
        }
        skipLine(inStream); // discard remainder of final header line
        
        int byteCount = numberOfComponents * bytesPerIntensity * width * height;
        byte[] pixelBytes = new byte[byteCount];
        pixels = ByteBuffer.wrap(pixelBytes);
        int numBytesRead = inStream.read(pixelBytes, 0, byteCount);
        
        pixels.rewind();
        doSwapBytes = true;
        needsUpload = true;
    }

}
