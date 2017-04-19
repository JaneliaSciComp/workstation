 package org.janelia.it.workstation.gui.large_volume_viewer;

 import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SampleAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.DirectedSessionAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl migrated from older implementation by olbrisd and bruns
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows Confocal Blocks data.
 */
public class LargeVolumeViewViewer extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(LargeVolumeViewViewer.class);

    private Vec3 initialViewFocus;
    private Double initialZoom;
    private AnnotationManager annotationMgr;
    private QuadViewUi viewUI;

    public LargeVolumeViewViewer() {
        super();
        setLayout(new BorderLayout());
    }

    private void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void loadDomainObject(final DomainObject domainObject) {
        logger.info("loadDomainObject({})", domainObject);

        close();
        showLoadingIndicator();
        
        // First load the related sample
        this.annotationMgr = getAnnotationManagerImpl(domainObject);
        
        Futures.addCallback(annotationMgr.loadSample(), new FutureCallback<TmSample>() {
            public void onSuccess(TmSample sample) {
                
                // Prepare the view for loading volume data
                HttpDataSource.setMouseLightCurrentSampleId(sample.getId());
                SkeletonController.refreshInstance();
                refresh();

                // Load image data in one thread
                final ProgressHandle progress = ProgressHandleFactory.createHandle("Loading image data...");
                progress.start();
                progress.setDisplayName("Loading image data");
                progress.switchToIndeterminate();
                
                // Load the volume in the background
                SimpleListenableFuture<Void> future1 = loadVolume(sample);
                Futures.addCallback(future1, new FutureCallback<Void>() {
                    
                    public void onSuccess(Void success) {
                        progress.finish();
                        logger.info("Image data loading completed");
                    }
                    
                    public void onFailure(Throwable t) {
                        progress.finish();
                        logger.error("Image data loading failed");
                    }
                    
                });

                final ProgressHandle progress2 = ProgressHandleFactory.createHandle("Loading metadata...");
                progress2.start();
                progress2.setDisplayName("Loading metadata");
                progress2.switchToIndeterminate();
                
                // Load metadata in another thread
                SimpleListenableFuture<Void> future2 = annotationMgr.load();
                Futures.addCallback(future2, new FutureCallback<Void>() {
                    
                    public void onSuccess(Void success) {
                        progress2.finish();
                        logger.info("Metadata loading completed");
                    }
                    
                    public void onFailure(Throwable t) {
                        progress2.finish();
                        logger.error("Metadata loading failed");
                    }
                    
                });
                
                // Join the two futures
                // TODO: In the future (heh), the hope is to decouple these two loads, so that they can succeed or fail independently. 
                // However, there is a lot of coupling in both directions, so loadComplete can only be called once both are done.
                ListenableFuture<List<Void>> combinedFuture = Futures.allAsList(Arrays.asList(future1, future2));
                Futures.addCallback(combinedFuture, new FutureCallback<List<Void>>() {
                    
                    public void onSuccess(List<Void> result) {
                        // If both loads succeeded
                        logger.info("Loading completed");
                        annotationMgr.loadComplete();
                    
                        // Initialize the camera if this is the first time loading
                        synchronized(this) {
                            if (initialViewFocus!=null) {
                                logger.info("Setting initial camera focus: {}", initialViewFocus);
                                viewUI.setCameraFocus(initialViewFocus);
                                initialViewFocus = null;
                            }
                            if (initialZoom!=null) {
                                logger.info("Setting initial zoom: {}", initialZoom);
                                viewUI.setPixelsPerSceneUnit(initialZoom);
                                initialZoom = null;
                            }
                        }
                    }
                    
                    public void onFailure(Throwable t) {
                        // If either load failed
                        logger.error("LVVV load failed", t);
                        close();
                    }
                    
                });
                
            }
            
            public void onFailure(Throwable t) {
                FrameworkImplProvider.handleException(t);
                close();
            }
        });
    }
    
    private SimpleListenableFuture<Void> loadVolume(final TmSample sample) {

        final SimpleWorker volumeLoader = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                // TODO: In the future, when the TileFormat gets loaded from the sample 
                // instead of from disk, we can use this commented code instead, and
                // still load annotations even if the sample gets moved. For now, we need to 
                // treat errors as fatal.
//                try {
//                    if (!viewUI.loadFile(sample.getFilepath())) {
//                        logger.error("Volume load failed");    
//                    }
//                }
//                catch (Exception e) {
//                    logger.error("Volume load failed", e);
//                }
                viewUI.loadFile(sample.getFilepath());
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                // If the data fails to load, pop up an error, but keep going
                ConsoleApp.handleException(error);
            }
        };
        
        return volumeLoader.executeWithFuture();
    }
        
    /**
     * Instantiate and return the appropriate controller for the given domain object. 
     * @param domainObject
     * @return
     */
    private AnnotationManager getAnnotationManagerImpl(DomainObject domainObject) {
        if (domainObject instanceof TmSample) {
            return new SampleAnnotationManager((TmSample)domainObject);
        }
        if (domainObject instanceof TmWorkspace) {
            return new WorkspaceAnnotationManager((TmWorkspace)domainObject);
        }
        if (domainObject instanceof TmDirectedSession) {
            return new DirectedSessionAnnotationManager((TmDirectedSession)domainObject);
        }
        else {
            throw new IllegalArgumentException("Can't handle objects of type "+domainObject.getType());
        }   
    }
    
    public void setInitialViewFocus(Vec3 initialViewFocus, Double initialZoom) {
        this.initialViewFocus = initialViewFocus;
        this.initialZoom = initialZoom;
    }

    public SampleLocation getSampleLocation() {
        return viewUI.getSampleLocation();
    }
    
    public void setLocation(SampleLocation sampleLocation) {
        viewUI.setSampleLocation(sampleLocation);
    }
    
    public boolean hasQuadViewUi() {
        return viewUI != null;
    }
    
    public QuadViewUi getQuadViewUi() {
        return viewUI;
    }
    
    public void close() {
        logger.info("Closing");
        removeAll();

        if (viewUI != null) {
            
            final QuadViewUi oldQuadView = viewUI;
            viewUI = null;
            
            SimpleWorker.runInBackground(new Runnable() {
                @Override
                public void run() {
                    logger.info("Clearing cache...");
                    oldQuadView.clearCache();
                    logger.info("Cache cleared");
                }
            });
        }
        
        if (annotationMgr!=null) {
            annotationMgr.close();
            annotationMgr = null;
        }
        
        revalidate();
        repaint();
    }
    
    private void refresh() {
        logger.info("Refreshing");

        showLoadingIndicator();

        if (annotationMgr != null) {
            if (viewUI == null) {
                viewUI = new QuadViewUi(ConsoleApp.getMainFrame(), false, annotationMgr);
            }
            annotationMgr.setQuadViewUi(viewUI);
        }
        
        removeAll();
        viewUI.setVisible(true);
        add(viewUI);

        // Repaint the skeleton
        SkeletonController.getInstance().skeletonChanged(true);
        
        revalidate();
        repaint();
        
        // Need to popup the skeletal viewer.
        AnnotationSkeletalViewTopComponent asvtc =
                (AnnotationSkeletalViewTopComponent)WindowLocator.getByName(
                        AnnotationSkeletalViewTopComponent.PREFERRED_ID
                );
        if (asvtc != null) {
            asvtc.revalidate();
            asvtc.repaint();
        }
    }    

    private void refreshInitialObject() {
        try {
            DomainObject domainObject = DomainMgr.getDomainMgr().getModel().getDomainObject(annotationMgr.getInitialObject());
            loadDomainObject(domainObject);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException("Error refreshing initial object", e);
        }
    }
    
    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            logger.debug("Total invalidation detected, refreshing...");
            refreshInitialObject();
        }
        else {
            for(DomainObject domainObject : event.getDomainObjects()) {
                if (annotationMgr!=null && DomainUtils.equals(domainObject, annotationMgr.getInitialObject())) {
                    // We don't do anything here because we assume that the LVV manages any updates to the workspace out-of-band. 
                    // There are some edge cases we could support here (e.g. if the user renames the workspace 
                    // from the Domain Explorer) but they're generally not worth the effort right now.
                    logger.info("Invalidated initial object: {}",domainObject.getName());
                }
            }
        }
    }
}
