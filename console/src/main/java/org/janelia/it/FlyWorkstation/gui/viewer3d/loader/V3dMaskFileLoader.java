package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.stream.V3dRawImageStream;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 *
 */
public class V3dMaskFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {
    public static final String COMPARTMENT_MASK_INDEX = "maskIndex";
    public static final String CONSOLIDATED_LABEL_MASK = "ConsolidatedLabel";
    @Override
    protected TextureDataI createTextureDataBean() {
        return new MaskTextureDataBean( maskByteArray, sx, sy, sz );
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        unCachedFileName = fileName;
        loadV3dMask(
                new BufferedInputStream(
                    new FileInputStream(unCachedFileName)
                )
        );
    }

    private void loadV3dMask(InputStream inputStream)
            throws IOException, DataFormatException {
        //isMask = true;

        V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
        sx = sliceStream.getDimension(0);
        sy = sliceStream.getDimension(1);
        sz = sliceStream.getDimension(2);
        pixelBytes = sliceStream.getPixelBytes();
        int sc = sliceStream.getDimension(3);
        channelCount = sc;
        pixelByteOrder = sliceStream.getEndian();

        // Java implicitly sets newly-allocated byte arrays to all zeros.
        maskByteArray = new byte[(sx*sy*sz) * pixelBytes];

        if ( sc > 1 ) {
            throw new RuntimeException( "Unexpected multi-channel mask file." );
        }

        if ( sc == 0 ) {
            throw new RuntimeException( "Unexpected zero channel count mask file." );
        }

        Set<Integer> values = new TreeSet<Integer>();
        for (int z = 0; z < sz; z ++ ) {
            int zOffset = z * sx * sy;
            sliceStream.loadNextSlice();
            V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
            for (int y = 0; y < sy; y ++ ) {
                int yOffset = zOffset + (sy-y) * sx;
                for (int x = 0; x < sx; x ++ ) {
                    Integer value = slice.getValue(x, y);
                    if ( value > 0 ) {
                        values.add( value );
                        for ( int pi = 0; pi < pixelBytes; pi ++ ) {
                            byte piByte = (byte)(value >>> (pi * 8) & 0x000000ff);
//                            maskByteArray[(yOffset * 2) + (x * 2) + (pixelBytes - pi - 1)] = piByte;
                            maskByteArray[(yOffset * pixelBytes) + (x * pixelBytes) + (pi)] = piByte;
                        }
                    }
                }
            }
        }

        for ( Integer value: values ) {
            System.out.print( value + "," );
        }
        System.out.println();
        header = sliceStream.getHeaderKey();
    }

}
