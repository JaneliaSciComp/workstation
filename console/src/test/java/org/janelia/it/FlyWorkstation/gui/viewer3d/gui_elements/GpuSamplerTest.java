package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.junit.Assert;
import org.junit.Test;

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
        GpuSampler sampler = new GpuSampler( Color.blue );
        Future<Boolean> growsUpToBeFickle = sampler.isDepartmentStandardGraphicsMac();

        try {
            if ( growsUpToBeFickle.get() ) {
                System.out.println("Detected department-choice card.  Please run glxinfo|grep OpenGL.  Look for " + GpuSampler.STANDARD_CARD_RENDERER_STR);
            }
            else {
                System.out.println("Did not detect here.  Please run glxinfo|grep OpenGL.  Look for " + GpuSampler.STANDARD_CARD_RENDERER_STR);
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
            Assert.fail();
        }
    }
}
