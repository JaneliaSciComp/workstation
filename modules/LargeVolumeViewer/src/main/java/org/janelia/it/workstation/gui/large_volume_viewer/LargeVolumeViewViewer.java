package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.google.common.util.concurrent.MoreExecutors;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl migrated from older implementation by olbrisd and bruns
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows Confocal Blocks data.
 */
public class LargeVolumeViewViewer extends JPanel {

    private final Logger logger = LoggerFactory.getLogger(LargeVolumeViewViewer.class);

    private DomainObject initialObject;
    private TmSample sliceSample;
    private TmWorkspace currentWorkspace;
    private Vec3 initialViewFocus;
    private Double initialZoom;
    private AnnotationModel annotationModel;
    private QuadViewUi viewUI;

    public LargeVolumeViewViewer() {
        super();
        setLayout(new BorderLayout());
    }

    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    public void loadDomainObject(final DomainObject domainObject) {
    	logger.info("loadDomainObject({})", domainObject);

        // Clear existing UI state
        if (annotationModel!=null) {
            annotationModel.clear();
        }
        close();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                initialObject = domainObject;
                // initial rooted entity should be a brain sample or a workspace; the QuadViewUI wants
                // the initial entity, but we need the sample either way to be able to open it:
                if (initialObject instanceof TmSample) {
                    sliceSample = (TmSample) initialObject;
                } else if (initialObject instanceof TmWorkspace) {
                    currentWorkspace = (TmWorkspace) initialObject;
                    try {
                        sliceSample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(currentWorkspace);
                    } catch (Exception e) {
                        logger.error("Error getting sample for {}", currentWorkspace, e);
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                if (sliceSample == null) {
                    JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                            "Could not find sample entity for this workspace!",
                            "Could not open workspace",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                HttpDataSource.setMouseLightCurrentSampleId(sliceSample.getId());
                
                // refresh is a UI action, has to happen here
                refresh();
                
            	logger.info("Found sample {}", sliceSample.getId());

                // track whether volume load succeeded; see note later
                AtomicBoolean volumeLoaded = new AtomicBoolean(false);

                // Join the two futures
                ListenableFuture<List<Void>> combinedFuture = Futures.allAsList(Arrays.asList(loadWorkspace(), loadVolume(volumeLoaded)));
                Futures.addCallback(combinedFuture, new FutureCallback<List<Void>>() {
                    public void onSuccess(List<Void> result) {
                        // check if the volume actually loaded; we handle exceptions, so
                        //  not all failures end up in onFailure()!
                        if (volumeLoaded.get()) {
                            logger.info("Loading completed");
                            if (annotationModel == null) {
                                // trying to diagnose how this could happen...
                                logger.info("found null annotationModel");
                            }
                            annotationModel.loadComplete();
                        } else {
                            // same as onFailure() (code copied):
                            logger.error("LVVV load failed; volume loader failed");
                            try {
                                if (annotationModel != null) {
                                    annotationModel.clear();
                                    annotationModel.loadComplete();
                                }
                            }
                            catch (Exception e) {
                                logger.error("Error loading empty workspace after failed workspace load",e);
                            }
                        }
                    }

                    public void onFailure(Throwable t) {
                        // If either load failed
                        logger.error("LVVV load failed; combined future failed", t);
                        try {
                            if (annotationModel!=null) {
                                annotationModel.clear();
                                annotationModel.loadComplete();
                            }
                        }
                        catch (Exception e) {
                            logger.error("Error loading empty workspace after failed workspace load",e);
                        }
                    }
                }, MoreExecutors.directExecutor());
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }

            private SimpleListenableFuture<Void> loadWorkspace() {
                // independently of the image volume load, we load the annotation data:
                final ProgressHandle progress2 = ProgressHandleFactory.createHandle("Loading metadata...");
                progress2.start();
                progress2.setDisplayName("Loading metadata");
                progress2.switchToIndeterminate();

                SimpleWorker workspaceLoader = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        if (initialObject == null) {
                            // this is a request to clear the workspace
                            annotationModel.clear();
                        } else if (initialObject instanceof TmSample) {
                            annotationModel.loadSample((TmSample) initialObject);
                        } else if (initialObject instanceof TmWorkspace) {
                            annotationModel.loadWorkspace((TmWorkspace) initialObject);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        logger.info("Metadata loading completed");
                        progress2.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        logger.error("workspace loader failed", error);
                        progress2.finish();
                        FrameworkImplProvider.handleException(error);
                    }
                };
                return workspaceLoader.executeWithFuture();
            }
            
            private SimpleListenableFuture<Void> loadVolume(AtomicBoolean volumeLoaded) {
                // but now we have to do the loads in other threads, so we don't lock the UI;
                //  first start up the image volume load
                final ProgressHandle progress = ProgressHandleFactory.createHandle("Loading image data...");
                progress.start();
                progress.setDisplayName("Loading image data");
                progress.switchToIndeterminate();

                SimpleWorker volumeLoader = new SimpleWorker() {
                    boolean success = false;

                    @Override
                    protected void doStuff() throws Exception {
                        success = viewUI.loadData(sliceSample);
                        volumeLoaded.set(success);
                    }

                    @Override
                    protected void hadSuccess() {
                        if (success) {
                            logger.info("Image data loading completed");
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
                        else {
                            logger.error("Image data loading failed");
                            JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                                    "Could not open volume for this workspace!",
                                    "Could not open volume",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                        progress.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress.finish();
                        FrameworkImplProvider.handleException(error);
                    }
                };
                return volumeLoader.executeWithFuture();
            }
        };
        worker.execute();

    }
    
    public void setInitialViewFocus(Vec3 initialViewFocus, Double initialZoom) {
        this.initialViewFocus = initialViewFocus;
        this.initialZoom = initialZoom;
    }

    public SampleLocation getSampleLocation() {
        return viewUI.getSampleLocation();
    }
    
    public void setSampleLocation(SampleLocation sampleLocation) {
        viewUI.setSampleLocation(sampleLocation);
    }
    
    public boolean hasQuadViewUi() {
        return viewUI != null;
    }
    
    public QuadViewUi getQuadViewUi() {
        if (!hasQuadViewUi()) {
            refresh();
        }
        return viewUI;
    }
    
    public void close() {
        logger.info("Closing");
        sliceSample = null;
        initialObject = null;
        removeAll();

        if (viewUI != null) {

            final QuadViewUi oldQuadView = viewUI;
            viewUI = null;

            SimpleWorker worker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    logger.info("Clearing cache...");
                    oldQuadView.clearCache();
                }

                @Override
                protected void hadSuccess() {
                    logger.info("Cache cleared");
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkImplProvider.handleException(error);
                }
            };
            worker.execute();
        }
        
        if (annotationModel!=null) {
            Events.getInstance().unregisterOnEventBus(annotationModel);
            // trying to diagnose a later null:
            logger.info("setting annotationModel to null");
            annotationModel = null;
        }
    }
    
    public void refresh() {
        logger.info("Refreshing");

        if (sliceSample != null) {
            showLoadingIndicator();

            if ( viewUI == null ) {
                // trying to diagnost how this can be null later
                logger.info("instantiating AnnotationModel");
                annotationModel = new AnnotationModel(sliceSample, currentWorkspace);
                Events.getInstance().registerOnEventBus(annotationModel);
                viewUI =  QuadViewUiProvider.createQuadViewUi(FrameworkImplProvider.getMainFrame(), initialObject, false, annotationModel);
            }
            
            removeAll();
            viewUI.setVisible(true);
            add((Component) viewUI);

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
    }    
    
    //------------------------------Private Methods

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
    	// Tm objects do not currently respect the domain object cache invalidation scheme, but we can at least reload the UI
        if (event.isTotalInvalidation()) {
            refresh();
        }
        else {
            for(DomainObject domainObject : event.getDomainObjects()) {
                if (DomainUtils.equals(domainObject, initialObject)) {
                    refresh();
                }
            }
        }
    }
}
