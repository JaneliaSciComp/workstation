 package org.janelia.it.workstation.gui.large_volume_viewer;

 import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SampleAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SessionAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

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

        showLoadingIndicator();
        
        SimpleWorker volumeLoader = new SimpleWorker() {
            final ProgressHandle progress = ProgressHandleFactory.createHandle("Loading image data...");
            Boolean success = null;
            
            @Override
            protected void doStuff() throws Exception {
                
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                        progress.start();
                        progress.setDisplayName("Loading image data");
                        progress.switchToIndeterminate();
                    }
                });
                
                TmSample currentSample = annotationMgr.getCurrentSample();
                if (currentSample!=null) {
                    success = viewUI.loadFile(currentSample.getFilepath());
                }
            }

            @Override
            protected void hadSuccess() {
                if (success!=null) {
                    if (success) {
                        logger.info("Image data loading completed");
                        synchronized(this) {
                            if (initialViewFocus!=null) {
                                logger.info("Setting intial camera focus: {}", initialViewFocus);
                                viewUI.setCameraFocus(initialViewFocus);
                                initialViewFocus = null;
                            }
                            if (initialZoom!=null) {
                                logger.info("Setting intial zoom: {}", initialZoom);
                                viewUI.setPixelsPerSceneUnit(initialZoom);
                                initialZoom = null;
                            }
                        }
                    }
                    else {
                        logger.info("Image data loading failed");
                        JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                                "Could not load image data for this sample!",
                                "Could not load image data",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                progress.finish();
            }

            @Override
            protected void hadError(Throwable error) {
                progress.finish();
                ConsoleApp.handleException(error);
            }
        };

        close();
        
        this.annotationMgr = getAnnotationManagerImpl(domainObject);
        annotationMgr.load(volumeLoader);
    }
        
    public AnnotationManager getAnnotationManagerImpl(DomainObject domainObject) {
        if (domainObject instanceof TmSample) {
            return new SampleAnnotationManager((TmSample)domainObject);
        }
        if (domainObject instanceof TmWorkspace) {
            return new WorkspaceAnnotationManager((TmWorkspace)domainObject);
        }
        if (domainObject instanceof TmSession) {
            return new SessionAnnotationManager((TmSession)domainObject);
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
        if (!hasQuadViewUi()) {
            refresh();
        }
        return viewUI;
    }
    
    public void close() {
        logger.info("Closing");
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
                    ConsoleApp.handleException(error);
                }
            };
            worker.execute();
        }
        
        if (annotationMgr!=null) {
            annotationMgr.close();
        }
    }
    
    private void refresh() {
        logger.info("Refreshing");

        showLoadingIndicator();

        if (viewUI == null) {
            viewUI = new QuadViewUi(ConsoleApp.getMainFrame(), false, annotationMgr);
        }
        
        if (annotationMgr != null) {
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
                if (DomainUtils.equals(domainObject, annotationMgr.getInitialObject())) {
                    // We don't do anything here because we assume that the LVV manages any updates to the workspace out-of-band. 
                    // There are some edge cases we could support here (e.g. if the user renames the workspace 
                    // from the Domain Explorer) but they're generally not worth the effort right now.
                    logger.info("Invalidated initial object: {}",domainObject.getName());
                }
            }
        }
    }
}
