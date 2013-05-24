package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.stream.V3dRawImageStream;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import javax.media.opengl.GL;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.DataFormatException;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Loader of signal data, from v3dpbd format input file.
 */
public class V3dSignalFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {

    @Override
    protected TextureDataI createTextureDataBean() {
        int interpolationMethod = GL.GL_LINEAR;
        if ( channelCount >= 3 ) {
            TextureDataBean textureDataBean = new TextureDataBean(argbTextureIntArray, sx, sy, sz);
            textureDataBean.setInterpolationMethod( interpolationMethod );
            return textureDataBean;
        }
        else {
            TextureDataBean textureDataBean = new TextureDataBean(textureByteArray, sx, sy, sz);
            textureDataBean.setInterpolationMethod( interpolationMethod );
            return textureDataBean;
        }
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        unCachedFileName = fileName;

        loadV3dRaw(new BufferedInputStream(
                new FileInputStream(unCachedFileName)));

    }

    private void loadV3dRaw(InputStream inputStream) throws IOException, DataFormatException {
        V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
        sx = sliceStream.getDimension(0);
        sy = sliceStream.getDimension(1);
        sz = sliceStream.getDimension(2);
        pixelBytes = sliceStream.getPixelBytes();
        int sc = sliceStream.getDimension(3);
        channelCount = sc;
        pixelByteOrder = sliceStream.getEndian();

        if ( channelCount >= 3 ) {
            loadV3dIntRaw( sliceStream, sc );
        }
        else if ( pixelBytes == 1 ) {
            loadV3dByteRaw( sliceStream, sc );
        }
        else {
            throw new IOException("Unexpected pixelbytes count of " + pixelBytes);
        }
    }

    private void loadV3dIntRaw(V3dRawImageStream sliceStream, int sc )
            throws IOException, DataFormatException {

        double scale = 1.0;
        if (sliceStream.getPixelBytes() > 1)
            scale = 255.0 / 4095.0; // assume it's 12 bits

        argbTextureIntArray = new int[sx*sy*sz];
        for (int c = 0; c < sc; ++c) {
            // create a mask to manipulate one color byte of a 32-bit ARGB int
            int bitShift = 8 * (c + 2);
            while (bitShift >= 32) bitShift -= 32; // channel 4 gets shifted zero (no shift)
            bitShift = 32 - bitShift;  // opposite shift inside loop
            int mask = (0x000000ff << bitShift);
            int notMask = ~mask;
            for (int z = 0; z < sz; ++z) {
                int zOffset = z * sx * sy;
                sliceStream.loadNextSlice();
                V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
                for (int y = 0; y < sy; ++y) {
                    int yOffset = zOffset + y * sx;
                    for (int x = 0; x < sx; ++x) {
                        int argb = argbTextureIntArray[yOffset + x] & notMask; // zero color component
                        double value = scale * slice.getValue(x, y);
                        int ival = (int)(value + 0.5);
                        if (ival < 0) ival = 0;
                        if (ival > 255) ival = 255;
                        ival = ival << bitShift;
                        argb = argb | ival; // insert updated color component
                        argbTextureIntArray[yOffset + x] = argb;
                    }
                }
            }
        }

        header = sliceStream.getHeaderKey();
    }

    private void loadV3dByteRaw(V3dRawImageStream sliceStream, int sc)
            throws IOException, DataFormatException {

        V3dByteReader byteReader = new V3dByteReader();
        byteReader.setInvertedY( false );
        Set<Integer> values= byteReader.readBytes( sliceStream, sx, sy, sz, pixelBytes );
        textureByteArray = byteReader.getTextureBytes();
        header = sliceStream.getHeaderKey();
    }

}
