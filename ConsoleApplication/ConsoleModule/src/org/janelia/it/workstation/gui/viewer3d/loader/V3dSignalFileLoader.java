package org.janelia.it.workstation.gui.viewer3d.loader;

import org.janelia.it.workstation.gui.viewer3d.stream.V3dRawImageStream;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Loader of signal data, from v3dpbd format input file.
 */
public class V3dSignalFileLoader extends LociFileLoader {

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        setUnCachedFileName( fileName );

        loadV3dRaw(new BufferedInputStream(
                new FileInputStream(fileName)));

    }

    private void loadV3dRaw(InputStream inputStream) throws IOException, DataFormatException {
        V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
        setSx(sliceStream.getDimension(0));
        setSy(sliceStream.getDimension(1));
        setSz(sliceStream.getDimension(2));
        setPixelBytes(sliceStream.getPixelBytes());
        int sc = sliceStream.getDimension(3);
        setChannelCount(sc);
        setPixelByteOrder(sliceStream.getEndian());

        if ( sc >= 3 ) {
            loadV3dIntRaw( sliceStream, sc );
        }
        else if ( sliceStream.getPixelBytes() == 1 ) {
            loadV3dByteRaw( sliceStream );
        }
        else {
            throw new IOException("Unexpected pixelbytes count of " + sliceStream.getPixelBytes());
        }
    }

    private void loadV3dIntRaw(V3dRawImageStream sliceStream, int sc )
            throws IOException, DataFormatException {

        double scale = 1.0;
        if (sliceStream.getPixelBytes() > 1)
            scale = 255.0 / 4095.0; // assume it's 12 bits

        int sx = getSx();
        int sy = getSy();
        int sz = getSz();
        initArgbTextureIntArray();
        int[] argbTextureIntArray = getArgbTextureIntArray();
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

        setHeader(sliceStream.getHeaderKey());
    }

    private void loadV3dByteRaw(V3dRawImageStream sliceStream)
            throws IOException, DataFormatException {

        V3dByteReader byteReader = new V3dByteReader();
        byteReader.setInvertedY( false );
        // Bypass some bytes.
        byteReader.readBytes( sliceStream, getSx(), getSy(), getSz(), getPixelBytes() );
        setTextureByteArray(byteReader.getTextureBytes());
        setHeader(sliceStream.getHeaderKey());
    }

}
