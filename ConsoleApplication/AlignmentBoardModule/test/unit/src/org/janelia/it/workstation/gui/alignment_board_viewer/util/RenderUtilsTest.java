/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer.util;

import java.awt.Color;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.junit.Test;

/**
 * Tests for the render utils.
 * @author fosterl
 */
public class RenderUtilsTest {
    @Test
    public void rgbFromColorTest() {
       System.out.println(RenderUtils.getRGBStrFromColor(Color.yellow)); 
    }
    
    @Test
    public void colorFromRgbTest() {
        final Color color = RenderUtils.getColorFromRGBStr("A0B0C0");
        System.out.println(color.getRed() + " " + color.getGreen() + " " + color.getBlue());
    }
    
    @Test
    public void viewableTypeTest() throws Exception {
        System.out.println(RenderUtils.getViewableClassName(RenderUtilsTest.class.getName()));
    }
}
