package org.janelia.it.FlyWorkstation.gui.viewer3d.channel;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderablesChannelsBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;

//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/14/13
 * Time: 4:47 PM
 *
 * This test will check the efficacy of the renderables mask builder.
 */
public class ChannelReadTest {

    private static final String MASK_CHAN_LOC = "/Users/fosterl/Documents/alignment_board/Mask_Chan/";

    private static final String MASK_FILE_NAME = "prefix_1.mask";
    private static final String LOCAL_MASK_FILE_PATH = MASK_CHAN_LOC + MASK_FILE_NAME;

    private static final String CHAN_FILE_NAME = "prefix_1.chan";
    private static final String LOCAL_CHAN_FILE_PATH = MASK_CHAN_LOC + CHAN_FILE_NAME;

    private InputStream testStream;
    private RandomAccessFile testRAF;

    // The input data is known to be little-endian or LSB.
    private byte[] intArray = new byte[ 4 ];
    private byte[] longArray = new byte[ 8 ];

    private Logger logger = LoggerFactory.getLogger( ChannelReadTest.class );

    private ByteBuffer intBuffer = ByteBuffer.wrap( intArray );
    {
        intBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

//    @Before
    public void setUp() throws  Exception {
        testStream = this.getClass().getResourceAsStream(MASK_FILE_NAME);
        if ( testStream == null ) {
            testStream = new FileInputStream(LOCAL_MASK_FILE_PATH);
            logger.warn("Resorting to hardcoded mask path.");
        }

        URL rafResource = this.getClass().getResource(CHAN_FILE_NAME);
        if ( rafResource != null ) {
            String rafLoc = rafResource.getFile();
            testRAF = new RandomAccessFile( rafLoc, "r" );
        }
        else {
            testRAF = new RandomAccessFile( LOCAL_CHAN_FILE_PATH, "r" );
            logger.warn("Resorting to hardcoded channel path.");
        }
    }

//    @After
    public void tearDown() throws Exception {
        testStream.close();
    }

//    @Test
    public void testReadOneFile() throws Exception {
        // Time-of-writing: only thing bean is used for is its tanslated number.
        RenderableBean bean = new RenderableBean();
        bean.setTranslatedNum( 1 );

        MaskChanFileLoader loader = new MaskChanFileLoader();
        loader.setByteCount( 2 );
        //loader.setRenderableBeans( Arrays.asList( bean ) );

        RenderablesChannelsBuilder builder = new RenderablesChannelsBuilder();
        builder.init();
        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList( builder ) );

        loader.read( bean, testStream, testRAF );

    }

}
