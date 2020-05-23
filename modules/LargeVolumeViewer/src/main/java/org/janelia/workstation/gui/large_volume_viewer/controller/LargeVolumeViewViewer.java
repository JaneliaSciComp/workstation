package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.eventbus.Subscribe;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.workstation.geom.Vec3;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.workstation.gui.large_volume_viewer.QuadViewUiProvider;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonController;
import org.janelia.workstation.integration.util.FrameworkAccess;
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
    private NeuronManager annotationModel;
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

    public void initialize() {
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

    public void setInitialViewFocus(Vec3 initialViewFocus, Double initialZoom) {
        this.initialViewFocus = initialViewFocus;
        this.initialZoom = initialZoom;
    }

    /*public SampleLocation getSampleLocation() {
        return viewUI.getSampleLocation();
    }
    
    public void setSampleLocation(SampleLocation sampleLocation) {
        viewUI.setSampleLocation(sampleLocation);
    }
    */
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
                    logger.info("Exporting any out-of-sync neurons...");
                    //oldQuadView.getAnnotationModel().exportOutOfSyncNeurons();

                    logger.info("Clearing cache...");
                    oldQuadView.clearCache();
                    oldQuadView.clear();
                }

                @Override
                protected void hadSuccess() {
                    logger.info("Cache cleared");
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            worker.execute();
        }
        
        if (annotationModel != null) {
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
                logger.info("instantiating NeuronManager");
               // annotationModel = new NeuronManager(sliceSample, currentWorkspace);
                Events.getInstance().registerOnEventBus(annotationModel);
                JadeServiceClient jadeServiceClient = new JadeServiceClient(
                        ConsoleProperties.getString("jadestorage.rest.url"),
                        () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false)
                );
                viewUI =  QuadViewUiProvider.createQuadViewUi(FrameworkAccess.getMainFrame(), initialObject, false, annotationModel, jadeServiceClient);
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
