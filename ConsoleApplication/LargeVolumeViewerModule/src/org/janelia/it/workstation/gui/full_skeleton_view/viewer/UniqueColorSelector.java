/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.Color;
import java.awt.Robot;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh.PixelReadActor;

/**
 * Using color-under-click to find what was selected.
 * @author fosterl
 */
public class UniqueColorSelector implements PixelReadActor.PixelListener {
    private AnnotationSkeletonDataSourceI dataSource;
    private Robot robot;
    
    public UniqueColorSelector(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
    }

    public long select(int x, int y) {
        return 0;
    }

    protected long getAwtColor(int x, int y) {
        try {
            robot = new Robot();
            Color color = robot.getPixelColor(x, y);
            if (color.getGreen() == 255 && color.getBlue() == 255 && color.getRed() == 255) {
                System.out.println("WHITE FOUND");
            }
            else {
                System.out.println(color);
            }
            return -1;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }
    
    private void compareColor() {
        //
    }

    /**
     * What to do when the pixel has been received by the actor.
     * @param pixel 
     */
    @Override
    public void setPixel(float[] pixel) {
        System.out.println(String.format("Color: r=%f / g=%f / b=%f", pixel[0], pixel[1], pixel[2]));
    }
}
