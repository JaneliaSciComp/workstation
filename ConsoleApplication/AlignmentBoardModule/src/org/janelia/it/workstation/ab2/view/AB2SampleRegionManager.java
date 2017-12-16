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
        mainRegion.reshape(drawable, 50, 50, 300, 300, width, height);
    }
}
