package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/27/13
 * Time: 3:54 PM
 *
 * Test how well the sampler detects GPUs.
 */
public class GpuSamplerTest {
    @Test
    public void isItStandard() {
        Logger logger = LoggerFactory.getLogger( GpuSamplerTest.class );
        GpuSampler sampler = new GpuSampler( Color.blue );
        Future<Boolean> growsUpToBeFickle = sampler.isDepartmentStandardGraphicsMac();
        logger.info("This 'test' really requires knowledge of your system to be effective.  Watch log output.");
        try {
            if ( growsUpToBeFickle.get() ) {
                logger.info("Detected department-choice card.  Please run glxinfo|grep OpenGL.  Look for " + GpuSampler.STANDARD_CARD_RENDERER_STR);
            }
            else {
                logger.info("Did not detect here.  Please run glxinfo|grep OpenGL.  Look for " + GpuSampler.STANDARD_CARD_RENDERER_STR);
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
            Assert.fail();
        }
    }
}
