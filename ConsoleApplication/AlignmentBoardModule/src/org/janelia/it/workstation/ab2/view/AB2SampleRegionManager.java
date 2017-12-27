package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLRegionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleRegionManager extends GLRegionManager {

    Logger logger = LoggerFactory.getLogger(AB2SampleRegionManager.class);

    AB2SampleTopRegion topRegion=new AB2SampleTopRegion();
    AB2SampleBottomRegion bottomRegion=new AB2SampleBottomRegion();
    AB2SampleLeftRegion leftRegion=new AB2SampleLeftRegion();
    AB2SampleRightRegion rightRegion=new AB2SampleRightRegion();
    AB2SampleMainRegion mainRegion=new AB2SampleMainRegion();

    public AB2SampleRegionManager() {
        regions.add(topRegion);
        regions.add(bottomRegion);
        regions.add(leftRegion);
        regions.add(rightRegion);
        regions.add(mainRegion);
    }

    public AB2SampleMainRegion getMainRegion() {
        return mainRegion;
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

        int topHeight=AB2SampleTopRegion.CLOSED_HEIGHT;
        if (topRegion.isOpen()) {
            topHeight=AB2SampleTopRegion.OPEN_HEIGHT;
        }

        int bottomHeight=AB2SampleBottomRegion.CLOSED_HEIGHT;
        if (bottomRegion.isOpen()) {
            bottomHeight=AB2SampleBottomRegion.OPEN_HEIGHT;
        }

        int mainHeight=height-(topHeight+bottomHeight);
        int mainWidth=width-(leftWidth+rightWidth);
        mainRegion.reshape(drawable, leftWidth, bottomHeight, mainWidth, mainHeight, width, height);
        bottomRegion.reshape(drawable, leftWidth, 0, width-rightWidth, bottomHeight, width, height);
        topRegion.reshape(drawable, 0, bottomHeight+mainHeight, width, topHeight, width, height);
        leftRegion.reshape(drawable, 0, 0, leftWidth, height-topHeight, width, height);
        rightRegion.reshape(drawable, width-rightWidth, 0, rightWidth, height-topHeight, width, height);
    }
}
