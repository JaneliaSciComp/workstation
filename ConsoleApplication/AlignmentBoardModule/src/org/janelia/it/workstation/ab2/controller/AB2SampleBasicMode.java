package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.actor.HorizontalDualSliderActor;
import org.janelia.it.workstation.ab2.event.AB2BlackModeRequestEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2Main3DRendererSetRangeEvent;
import org.janelia.it.workstation.ab2.event.AB2MainMessageEvent;
import org.janelia.it.workstation.ab2.event.AB2MainMessageHideEvent;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.ab2.event.AB2SampleAddedEvent;
import org.janelia.it.workstation.ab2.event.AB2WhiteModeRequestEvent;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLRegionManager;
import org.janelia.it.workstation.ab2.loader.AB2Sample3DImageLoader;
import org.janelia.it.workstation.ab2.renderer.AB2ImageControlPanelRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2Main2DRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2Main3DRenderer;
import org.janelia.it.workstation.ab2.view.AB2SampleBottomRegion;
import org.janelia.it.workstation.ab2.view.AB2SampleMainRegion;
import org.janelia.it.workstation.ab2.view.AB2SampleRegionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleBasicMode extends AB2View3DMode {

    Logger logger= LoggerFactory.getLogger(AB2SampleBasicMode.class);

    private AB2SampleRegionManager sampleRegionManager=new AB2SampleRegionManager();

    private AB2Main3DRenderer sampleRenderer;


    public AB2SampleBasicMode(AB2Controller controller) {
        super(controller);
        //logger.info("AB2SampleBasicMode() constructor finished");
    }

    @Override
    public GLRegion getRegionAtPosition(Point point) {
        return sampleRegionManager.getRegionAtPosition(point);
    }

    @Override
    public GLRegionManager getRegionManager() { return sampleRegionManager; }


    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.init(glAutoDrawable);
        sampleRenderer=sampleRegionManager.getMainRegion().getSampleRenderer();
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.dispose(glAutoDrawable);
        super.dispose(glAutoDrawable);
    }

    @Override
    public void modeDisplay(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.display(glAutoDrawable);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        super.reshape(glAutoDrawable, x, y, width, height);
        sampleRegionManager.reshape(glAutoDrawable, x, y, width, height);
    }

    @Override
    public void processEvent(AB2Event event) {
        //logger.info("processEvent() - type="+event.getClass().getName());
        super.processEvent(event);
        if (event instanceof AB2MainMessageEvent) {
            AB2SampleRegionManager regionManager=(AB2SampleRegionManager)getRegionManager();
            AB2SampleMainRegion mainRegion=regionManager.getMainRegion();
            AB2Main2DRenderer messageRenderer=mainRegion.getMessageRenderer();
            messageRenderer.updateTextMessage(((AB2MainMessageEvent) event).getMessage());
        }
        else if (event instanceof AB2MainMessageHideEvent) {
            AB2SampleRegionManager regionManager=(AB2SampleRegionManager)getRegionManager();
            AB2SampleMainRegion mainRegion=regionManager.getMainRegion();
            AB2Main2DRenderer messageRenderer=mainRegion.getMessageRenderer();
            messageRenderer.hideTextMessage();
        }
        else if (event instanceof AB2SampleAddedEvent) {
            logger.info("processing AB2SampleAddedEvent");
            AB2SampleAddedEvent sampleAddedEvent=(AB2SampleAddedEvent)event;
            sampleRenderer.clearActors();
            logger.info("creating AB2Sample3DImageLoader");
            AB2Sample3DImageLoader sample3DImageLoader=new AB2Sample3DImageLoader(sampleAddedEvent.getSample());
            logger.info("executing sample3DImageLoader");
            sample3DImageLoader.execute();
            controller.processEvent(new AB2MainMessageEvent("Loading Sample "+sampleAddedEvent.getSample().getName()+" ..."));
        }
        else if  (event instanceof AB2Sample3DImageLoadedEvent) {
            logger.info("processing AB2Sample3DImageLoadedEvent");
            AB2Sample3DImageLoadedEvent sample3DImageLoadedEvent=(AB2Sample3DImageLoadedEvent)event;
            logger.info("calling sampleRenderer.addSample3DImage()");
            sampleRenderer.addSample3DImage(sample3DImageLoadedEvent.getData());
            sample3DImageLoadedEvent.clearData();

            AB2SampleRegionManager regionManager=(AB2SampleRegionManager)getRegionManager();
            AB2SampleBottomRegion bottomRegion=regionManager.getBottomRegion();
            AB2ImageControlPanelRenderer imageControlPanelRenderer=bottomRegion.getImageControlPanelRenderer();
            HorizontalDualSliderActor rangeSlider=imageControlPanelRenderer.getRangeSlider();
            AB2Main3DRendererSetRangeEvent rangeEvent=new AB2Main3DRendererSetRangeEvent(rangeSlider.getSlider1Position(),
                    rangeSlider.getSlider2Position());
            controller.processEvent(rangeEvent);

            controller.processEvent(new AB2MainMessageHideEvent());

            logger.info("calling controller.repaint after sampleRenderer.addSample3DImage");
            controller.setNeedsRepaint(true);
        }
        else if (event instanceof AB2Main3DRendererSetRangeEvent) {
            AB2SampleRegionManager regionManager=(AB2SampleRegionManager)getRegionManager();
            AB2SampleMainRegion mainRegion=regionManager.getMainRegion();
            mainRegion.processEvent(event);
            controller.setNeedsRepaint(true);
        }
        else if (event instanceof AB2BlackModeRequestEvent) {
            logger.info("Received request for AB2BlackModeRequestEvent");
        }
        else if (event instanceof AB2WhiteModeRequestEvent) {
            logger.info("Received request for AB2WhiteModeRequestEvent");
        }
    }

}
