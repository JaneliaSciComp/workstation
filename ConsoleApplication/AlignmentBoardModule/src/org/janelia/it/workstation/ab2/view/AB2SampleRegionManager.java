package org.janelia.it.workstation.ab2.view;

import java.awt.Point;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLRegionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleRegionManager extends GLRegionManager {

    Logger logger = LoggerFactory.getLogger(AB2SampleRegionManager.class);

    AB2Controller controller;

    AB2SampleTopRegion topRegion;
    AB2SampleBottomRegion bottomRegion;
    AB2SampleLeftRegion leftRegion;
    AB2SampleRightRegion rightRegion;
    AB2SampleMainRegion mainRegion;

    public AB2SampleRegionManager() {}

    @Override
    public void init(GLAutoDrawable drawable) {
        this.controller=AB2Controller.getController();

        int x=0;
        int y=0;
        int width=controller.getGlWidth();
        int height=controller.getGlHeight();

        logger.info("init() glWidth="+width+" glHeight="+height);

        int mainHeight=(int)(0.9*(1.0*height));
        int topHeight=(int)(0.05*(1.0*height));
        int bottomHeight=(int)(0.05*(1.0*height));
        int rightWidth=(int)(0.05*(1.0*width));
        int leftWidth=(int)(0.05*(1.0*width));

        topRegion=new AB2SampleTopRegion(0, height-topHeight-1, width, topHeight, width, height);
        bottomRegion=new AB2SampleBottomRegion(leftWidth, 0, width-(leftWidth+rightWidth), bottomHeight, width, height);
        leftRegion=new AB2SampleLeftRegion();
        rightRegion=new AB2SampleRightRegion();
        mainRegion=new AB2SampleMainRegion(leftWidth, bottomHeight, width-(leftWidth+rightWidth), mainHeight, width, height);

        regions.add(topRegion);
        regions.add(bottomRegion);
        regions.add(leftRegion);
        regions.add(rightRegion);
        regions.add(mainRegion);

        super.init(drawable);
    }

    public AB2SampleMainRegion getMainRegion() {
        return mainRegion;
    }

    public GLRegion getRegionAtPosition(Point point) {
        if (mainRegion.containsPointUsingYFlip(point)) {
            return mainRegion;
        }
        else if (bottomRegion.containsPointUsingYFlip(point)) {
            return bottomRegion;
        }
        else if (rightRegion.containsPointUsingYFlip(point)) {
            return rightRegion;
        }
        else if (leftRegion.containsPointUsingYFlip(point)) {
            return leftRegion;
        }
        else if (topRegion.containsPointUsingYFlip(point)) {
            return topRegion;
        }
        return null;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        int leftWidth=AB2SampleLeftRegion.CLOSED_WIDTH;
        if (leftRegion.isOpen()) {
            leftWidth=AB2SampleLeftRegion.OPEN_WIDTH;
        }

        int rightWidth=AB2SampleRightRegion.CLOSED_WIDTH;
        if (rightRegion.isOpen()) {
            rightWidth=AB2SampleRightRegion.OPEN_WIDTH;
        }

        int topHeight= AB2Properties.TOP_MENU_CLOSED_HEIGHT;
        if (topRegion.isOpen()) {
            topHeight=AB2Properties.TOP_MENU_OPEN_HEIGHT;
        }

//        int bottomHeight=AB2SampleBottomRegion.CLOSED_HEIGHT;
//        if (bottomRegion.isOpen()) {
//            bottomHeight=AB2SampleBottomRegion.OPEN_HEIGHT;
//        }

        int bottomHeight=AB2Properties.IMAGE_CONTROL_PANEL_CLOSED_HEIGHT;
        if (bottomRegion.isOpen()) {
            bottomHeight=AB2Properties.IMAGE_CONTROL_PANEL_OPEN_HEIGHT;
        }

        int mainHeight=height-(topHeight+bottomHeight);
        int mainWidth=width-(leftWidth+rightWidth);
        mainRegion.reshape(drawable, leftWidth, bottomHeight, mainWidth, mainHeight, width, height);
        bottomRegion.reshape(drawable, leftWidth, 0, width-(leftWidth+rightWidth), bottomHeight, width, height);
        topRegion.reshape(drawable, 0, bottomHeight+mainHeight, width, topHeight, width, height);
        leftRegion.reshape(drawable, 0, 0, leftWidth, height-topHeight, width, height);
        rightRegion.reshape(drawable, width-rightWidth, 0, rightWidth, height-topHeight, width, height);
    }
}
