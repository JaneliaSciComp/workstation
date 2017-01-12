 package org.janelia.it.workstation.gui.large_volume_viewer;

 import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.neuron_api.NeuronSetAdapter;
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

    private final Logger logger = LoggerFactory.getLogger(LargeVolumeViewViewer.class);

    private TmSample sliceSample;
    private DomainObject initialObject;
    private AnnotationModel annotationModel;
    private QuadViewUi viewUI;
    private final NeuronSetAdapter neuronSetAdapter = new NeuronSetAdapter(); // For communicating annotations to Horta

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
        // NOTE: there must be a better way to handle the tasks in and out of
        //  the UI thread; this version is the result of fixing what
        //  we had w/o serious rewriting

    	logger.info("loadDomainObject({})", domainObject);
    	
        //  I have found that with very large numbers of
        //  neurons in the neurons table, not reloading
        //  causes GUI lockup.
        deleteAll();
    	
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                initialObject = domainObject;

                // initial rooted entity should be a brain sample or a workspace; the QuadViewUI wants
                //  the initial entity, but we need the sample either way to be able to open it:
                if (initialObject instanceof TmSample) {
                    sliceSample = (TmSample) initialObject;
                }
                else if (initialObject instanceof TmWorkspace) {
                    TmWorkspace workspace = (TmWorkspace) initialObject;
                    try {
                        sliceSample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(workspace);
                    }
                    catch (Exception e) {
                        logger.error("Error getting sample for "+workspace, e);
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

                // but now we have to do the load in another thread, so we don't lock the UI:
                final ProgressHandle progress = ProgressHandleFactory.createHandle("Loading image data...");
                progress.start();
                progress.setDisplayName("Loading image data");
                progress.switchToIndeterminate();

                SimpleWorker volumeLoader = new SimpleWorker() {
                    boolean success = false;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        success = viewUI.loadFile(sliceSample.getFilepath());
                    }

                    @Override
                    protected void hadSuccess() {
                        if (success) {
                            logger.info("Image data loading completed");
                        }
                        else {
                            logger.info("Image data loading failed");
                            JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                                    "Could not open sample entity for this workspace!",
                                    "Could not open workspace",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                        progress.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress.finish();
                        ConsoleApp.handleException(error);
                    }
                };
                
                SimpleListenableFuture future1 = volumeLoader.executeWithFuture();

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
                        }
                        else if (initialObject instanceof TmSample) {
                            annotationModel.loadSample((TmSample)initialObject);
                        }
                        else if (initialObject instanceof TmWorkspace) {
                            annotationModel.loadWorkspace((TmWorkspace)initialObject);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        logger.info("Metadata loading completed");
                        progress2.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress2.finish();
                        ConsoleApp.handleException(error);
                    }
                };
                
                SimpleListenableFuture future2 = workspaceLoader.executeWithFuture();
                
                // Join the two futures
                ListenableFuture<List<Boolean>> combinedFuture = Futures.allAsList(Arrays.asList(future1, future2));
                Futures.addCallback(combinedFuture, new FutureCallback<List<Boolean>>() {
                    public void onSuccess(List<Boolean> result) {
                        // If both loads succeeded
                        logger.info("Loading completed");
                        annotationModel.loadComplete();
                    }
                    public void onFailure(Throwable t) {
                        // If either load failed
                        logger.error("LVVV load failed", t);
                        try {
                            if (annotationModel!=null) {
                                annotationModel.clear();
                                annotationModel.loadComplete();
                            }
                        }
                        catch (Exception e) {
                            logger.error("Error loading empty workspace",e);
                        }
                    }
                });
                
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }

        };
        worker.execute();

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
        if (!hasQuadViewUi()) {
            refresh();
        }
        return viewUI;
    }
    
    public void close() {
        logger.info("Closing");
        deleteAll();
    }

    public void refresh() {
        logger.info("Refreshing");

        if (sliceSample != null) {
            showLoadingIndicator();

            if ( viewUI == null ) {
                annotationModel = new AnnotationModel();
                Events.getInstance().registerOnEventBus(annotationModel);
                viewUI = new QuadViewUi(ConsoleApp.getMainFrame(), initialObject, false, annotationModel);
                neuronSetAdapter.observe(annotationModel);
            }
            removeAll();
            viewUI.setVisible(true);
            add(viewUI);
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

    public void totalRefresh() {
        refresh();
    }
    
    //------------------------------Private Methods

    private void deleteAll() {
        sliceSample = null;
        initialObject = null;
        removeAll();
        if (viewUI != null)
        	viewUI.clearCache();
        viewUI = null;
        if (annotationModel!=null) {
            Events.getInstance().unregisterOnEventBus(annotationModel);
            annotationModel = null;
        }
    }

    public NeuronSet getNeuronSetAdapter() {
        return neuronSetAdapter;
    }

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
    	// Tm objects do not currently respect the domain object cache invalidation scheme, but we can at least reload the UI
        if (event.isTotalInvalidation()) {
            refresh();
        }
        else {
            for(DomainObject domainObject : event.getDomainObjects()) {
                if (DomainUtils.equals(domainObject, sliceSample)) {
                    refresh();
                }
            }
        }
    }
}
