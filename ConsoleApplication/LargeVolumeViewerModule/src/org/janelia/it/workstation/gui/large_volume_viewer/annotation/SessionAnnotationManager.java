package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.net.URL;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSession;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.workers.ResultWorker;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotation controller implementation for Directed Tracing Workflow sessions.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionAnnotationManager extends BasicAnnotationManager {

    private static final Logger log = LoggerFactory.getLogger(SessionAnnotationManager.class);
    
    private final TmSession initialObject;
    
    public SessionAnnotationManager(TmSession session) {
        this.initialObject = session;
    }
    
    @Override
    public TmSession getInitialObject() {
        return initialObject;
    }
    
    @Override
    public SimpleListenableFuture<TmSample> loadSample() {
        
        ResultWorker<TmSample> worker = new ResultWorker<TmSample>() {
            @Override
            protected TmSample createResult() throws Exception {
                TmSample sample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(initialObject);
                annotationModel.setSample(sample);
                return sample;
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
    public SimpleListenableFuture<Void> load() {

        log.info("loadDomainObject({})", initialObject);
                
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.loadSession(initialObject);
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
        // TODO: implement this to set the tile format
    }

    @Override
    public void createWorkspace() {
        
    }
}
