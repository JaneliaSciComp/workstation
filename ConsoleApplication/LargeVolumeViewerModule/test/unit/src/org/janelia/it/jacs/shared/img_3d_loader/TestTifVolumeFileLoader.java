/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.io.FileInputStream;
import org.janelia.it.workstation.cache.large_volume.Utilities;
import org.junit.Assert;
import org.junit.Test;

/**
 * Need to test the like-named class.
 *
 * @author fosterl
 */
public class TestTifVolumeFileLoader {
    //private static final String TEST_INPUT = "/tier2/mousebrainmicro-nb/fromnobackup/2014-06-24-Descriptor-stitch1/3/default.0.tif";
    //private static final String TEST_INPUT = "/tier2/mousebrainmicro-nb/fromnobackup/2014-06-24-Descriptor-stitch1/3/8/4/1/8/8/default.0.tif";
    //private static final String TEST_INPUT = "/tier2/mousebrainmicro-nb/fromnobackup/2014-06-24-Descriptor-stitch1/3/8/4/2/7/5/default.1.tif";
    private static final String TEST_INPUT = "/tier2/mousebrainmicro-nb/fromnobackup/2014-06-24-Descriptor-stitch1/3/8/4/2/7/5/default.0.tif";
    private static final int TEST_INPUT_SIZE = 93734408;
    public static final int KNOWN_PIXELBYTES = 2;
    public static final int KNOWN_SLICECOUNT = 200;
    public static final int KNOWN_HEIGHT = 485;
    public static final int KNOWN_WIDTH = 483;

    @Test
    public void testLoadVolumeInFormat() throws Exception {
        int blockSize = KNOWN_WIDTH * KNOWN_HEIGHT * KNOWN_SLICECOUNT * KNOWN_PIXELBYTES;
        byte[] finalTiffBytes = new byte[ blockSize ];
        byte[] rawBytes = new byte[TEST_INPUT_SIZE];
        
        //  Need to pull raw tiff bytes into memory.
        try (FileInputStream fis = new FileInputStream(TEST_INPUT)) {
            fis.read(rawBytes);
        } catch (Exception ex) {
            throw ex;
        }
        
        TifVolumeFileLoader loader = new TifVolumeFileLoader();
        loader.setTextureByteArray(finalTiffBytes);
        loader.setPixelBytes(2);
        loader.loadVolumeInFormat(rawBytes);

        boolean zeroScan = Utilities.zeroScan(finalTiffBytes, TEST_INPUT, "TestTifVolumeFileLoader--Whole Buffer");
        Assert.assertEquals("Whole buffer is all zero", true, zeroScan);

        final int sliceSize = loader.getSx() * loader.getSy() * 2;
        System.out.println("Slice size=" + sliceSize);
        Assert.assertEquals("Unexpected slice size", sliceSize, KNOWN_WIDTH * KNOWN_HEIGHT * KNOWN_PIXELBYTES);

        for (int i = 0; i < 200; i++) {
            byte[] slice = new byte[sliceSize];
            System.arraycopy( finalTiffBytes, sliceSize * i, slice, 0, sliceSize );
            zeroScan = Utilities.zeroScan(slice, TEST_INPUT, "TestTifVolumeFileLoader--slice #" + i);
            Assert.assertEquals("Slice buffer number " + i + " all zero.", zeroScan, true);
        }
    }
}
