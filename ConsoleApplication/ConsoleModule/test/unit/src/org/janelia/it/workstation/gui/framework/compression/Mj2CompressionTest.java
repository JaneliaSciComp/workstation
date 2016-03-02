/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.compression;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of the MJW compression.
 *
 * @author fosterl
 */
public class Mj2CompressionTest {
    //private static final File MOCK_DECOMPRESSED = new File("/tier2/mousebrainmicro/mousebrainmicro/data/2015-06-19/Tiling/2015-06-29/01/01941/01941-ngc.1.tif");
    private static final File MOCK_DECOMPRESSED = new File("C:\\Users\\FOSTERL\\Documents\\mj2_test_files\\01941-ngc.0.tif");
    private static final File MOCK_COMPRESSED = new File("C:\\Users\\FOSTERL\\Documents\\mj2_test_files\\01941-ngc.0.tif_comp-10.mj2");
    //private static final File MOCK_COMPRESSED = new File("/tier2/mousebrainmicro/mousebrainmicro/data/2015-06-19/Tiling/2015-06-29/01/01941/01941-ngc.0.tif_comp-10.mj2");
    
    private Mj2ExecutableCompressionAlgorithm mj2 = new Mj2ExecutableCompressionAlgorithm();
    
    @Test
    public void canDecompress() {
        Assert.assertTrue(MOCK_COMPRESSED + " not considered de-compressible.", mj2.canDecompress(MOCK_COMPRESSED));
    }

    @Test
    public void getCompressedNameForFile() {        
        File targetFile = mj2.getCompressedNameForFile(MOCK_DECOMPRESSED);
        Assert.assertEquals("After compression, unexpected name.", MOCK_COMPRESSED, targetFile);
    }

    @Test
    public void getDecompressedNameForFile() {
        File targetFile = mj2.getDecompressedNameForFile(MOCK_COMPRESSED);
        Assert.assertEquals("After decompression, unexpected name.", MOCK_DECOMPRESSED, targetFile);
    }
    
    @Test
    public void decompressAsFile() throws Exception {
        File compressed = mj2.decompressAsFile(MOCK_COMPRESSED);
        Assert.assertNotNull("Decompression resulted in null.", compressed);
    }

}
