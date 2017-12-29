package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.ab2.event.AB2SampleAddedEvent;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.loader.AB2Sample3DImageLoader;
import org.janelia.it.workstation.ab2.renderer.AB2Main3DRenderer;
import org.janelia.it.workstation.ab2.view.AB2SampleRegionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleBasicMode extends AB2View3DMode {

    Logger logger= LoggerFactory.getLogger(AB2SampleBasicMode.class);

    private AB2SampleRegionManager sampleRegionManager=new AB2SampleRegionManager();

    private AB2Main3DRenderer sampleRenderer;


    public AB2SampleBasicMode(AB2Controller controller) {
        super(controller);
        sampleRenderer=sampleRegionManager.getMainRegion().getSampleRenderer();
        logger.info("AB2SampleBasicMode() constructor finished");
    }

    @Override
    public GLRegion getRegionAtPosition(Point point) {
        return null;
    }


    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.init(glAutoDrawable);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.dispose(glAutoDrawable);
        super.dispose(glAutoDrawable);
    }

    @Override
    public void modeDisplay(GLAutoDrawable glAutoDrawable) {
        super.display(glAutoDrawable);
        sampleRegionManager.display(glAutoDrawable);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        super.reshape(glAutoDrawable, x, y, width, height);
        sampleRegionManager.reshape(glAutoDrawable, x, y, width, height);
    }

    @Override
    public void processEvent(AB2Event event) {
        super.processEvent(event);
        if (event instanceof AB2SampleAddedEvent) {
            AB2SampleAddedEvent sampleAddedEvent=(AB2SampleAddedEvent)event;
            sampleRenderer.clearActors();
            AB2Sample3DImageLoader sample3DImageLoader=new AB2Sample3DImageLoader(sampleAddedEvent.getSample());
            sample3DImageLoader.execute();
        } else if  (event instanceof AB2Sample3DImageLoadedEvent) {
            AB2Sample3DImageLoadedEvent sample3DImageLoadedEvent=(AB2Sample3DImageLoadedEvent)event;
            sampleRenderer.addSample3DImage(sample3DImageLoadedEvent.getData());
            sample3DImageLoadedEvent.clearData();
            controller.repaint();
        }
    }

}
