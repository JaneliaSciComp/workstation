package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.net.URL;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotation controller for loading Samples.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleAnnotationManager extends BasicAnnotationManager {

    private static final Logger log = LoggerFactory.getLogger(SampleAnnotationManager.class);

    private final TmSample initialObject;
    
    public SampleAnnotationManager(TmSample sample) {
        this.initialObject = sample;
    }
    
    @Override
    public TmSample getInitialObject() {
        return initialObject;
    }

    @Override
    public SimpleListenableFuture<TmSample> loadSample() {
        return new SimpleListenableFuture<TmSample>() {
            {
                annotationModel.setSample(initialObject);
                set(initialObject);
            }
        };
    }
    
    @Override
    public SimpleListenableFuture<Void> load() {
        
        log.info("loadDomainObject({})", initialObject);
        final TmSample sliceSample = initialObject;

        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.loadSample(sliceSample);
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        return worker.executeWithFuture();
    }

    @Override
    public void loadComplete() {
        annotationModel.loadComplete();
    }

    //-------------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoadStarted(URL vol) {
    }
    @Override
    public void volumeLoaded(URL url) {
        activityLog.setTileFormat(getTileFormat(), initialObject.getId());
    }
}
