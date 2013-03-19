package org.janelia.it.FlyWorkstation.gui.viewer3d.channel;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderablesMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/15/13
 * Time: 11:19 AM
 *
 * This tests the read efficacy of channel files.
 */
public class MaskReadTest {

    ///Users/fosterl/Documents/alignment_board/Mask_Chan
    private static final String TEST_FILE_NAME = "prefix_1.mask";
    private static final String LOCAL_FILE_PATH = "/Users/fosterl/Documents/alignment_board/Mask_Chan/prefix_1.mask";

    private InputStream testStream;

    private Logger logger = LoggerFactory.getLogger( MaskReadTest.class );

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

//    @Before
    public void setUp() throws  Exception {
        testStream = this.getClass().getResourceAsStream( TEST_FILE_NAME );
        if ( testStream == null ) {
            testStream = new FileInputStream( LOCAL_FILE_PATH );
        }
    }

//    @After
    public void tearDown() throws Exception {
        testStream.close();
    }

    /*
  Format for mask and channel files.
  Mask files:
  long xsize; // space
  long ysize; // space
  long zsize; // space
  long x0; // bounding box
  long x1; // bounding box, such that x0 is inclusive, x1 exclusive, etc
  long y0; // bb
  long y1; // bb
  long z0; // bb
  long z1; // bb
  long totalVoxels;
  unsigned char axis; // 0=yz(x), 1=xz(y), 2=xy(z)
  { // For each ray
    long skip;
    long pairs;
    { // For each pair
        long start;
        long end; // such that end-start is length, i.e., end is exclusive
    }
  }
  Channel files:
  long totalVoxels;
  unsigned char channels; // number of channels
  unsigned char recommendedRedChannel;
  unsigned char recommendedGreenChannel;
  unsigned char recommendedBlueChannel;
  unsigned char bytesPerChannel; // 1=8-bit, 2=16-bit
  { // For each channel
    { // For each voxel
        B value;
    }
  }
    */
//    @Test
    public void testReadChannelData() throws Exception {
        logger.info( "Reading channel data." );
        RenderablesMaskBuilder renderablesMaskBuilder = new RenderablesMaskBuilder();
        Collection<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();
        acceptors.add( renderablesMaskBuilder );

        MaskChanFileLoader loader = new MaskChanFileLoader();
        loader.setAcceptors(acceptors);
        RenderableBean bean = new RenderableBean();
        //loader.setRenderableBeans(Arrays.asList( bean ) );

        // TODO make an alternative that takes the right stream:  see also RenMaskBldrTest.
        loader.read( bean, new BufferedInputStream( testStream ) );
        logger.info( "Completed read-channel data." );
    }

}
