 package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.neuron_api.NeuronSetAdapter;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl migrated from older implementation by olbrisd and bruns
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows Confocal Blocks data.
 */
public class LargeVolumeViewViewer extends JPanel {
    private Entity sliceSample;
    private Entity initialEntity;

    private RootedEntity slcRootedEntity;

    private QuadViewUi viewUI;
    private ModelMgrObserver modelMgrObserver;
    private final NeuronSetAdapter neuronSetAdapter = new NeuronSetAdapter(); // For communicating annotations to Horta
    private final Logger logger = LoggerFactory.getLogger(LargeVolumeViewViewer.class);

    private static long currentSampleId=0L;

    public static long getCurrentSampleId() {
        return currentSampleId;
    }

    public LargeVolumeViewViewer() {
        super();
        setLayout(new BorderLayout());
    }

    public void clear() {
        clearObserver();
    }

    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void loadEntity(final RootedEntity rootedEntity) {
        // NOTE: there must be a better way to handle the tasks in and out of
        //  the UI thread; this version is the result of fixing what
        //  we had w/o serious rewriting

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                //  I have found that with very large numbers of
                //  neurons in the neurons table, not reloading
                //  causes GUI lockup.
                //                if (initialEntity != null && rootedEntity.getEntity().getId() != initialEntity.getId()) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        deleteAll();
                    }
                });
                //                }
                initialEntity = rootedEntity.getEntity();

                // intial rooted entity should be a brain sample or a workspace; the QuadViewUI wants
                //  the intial entity, but we need the sample either way to be able to open it:
                if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                    sliceSample = initialEntity;
                } else if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                    // Which version of workspace?  Can it be handled, here?
                    boolean usableVersion = false;
                    TmWorkspace.Version version = AnnotationManager.getWorkspaceVersion(initialEntity);
                    if (version == TmWorkspace.Version.PB_1) {
                        usableVersion = true;
                    }
                    
                    if (! usableVersion) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(
                                    LargeVolumeViewViewer.this,
                                    "The workspace version is being converted for this version of Large Volume Viewer.  Several minutes' delay may ensue.",
                                    "Must Convert Workspace",
                                    JOptionPane.INFORMATION_MESSAGE
                                );
                            }
                        });
                    }
                    
                    String sampleID = initialEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
                    try {
                        sliceSample = ModelMgr.getModelMgr().getEntityById(sampleID);
                        currentSampleId=sliceSample.getId(); // to support HttpDataSource performance
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (sliceSample == null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                                        "Could not find sample entity for this workspace!",
                                        "Could not open workspace",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                // refresh is a UI action, has to happen here
                refresh();

                // but now we have to do the load in another thread, so we don't lock the UI:
                final ProgressHandle progress = ProgressHandleFactory.createHandle("Loading workspace...");
                progress.start();
                progress.setDisplayName("Loading workspace");
                progress.switchToIndeterminate();

                SimpleWorker opener = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // be sure we've successfully gotten the sample before loading it!
                        if (sliceSample != null && sliceSample.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                            if (!viewUI.loadFile(sliceSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH))) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                                                "Could not open sample entity for this workspace!",
                                                "Could not open workspace",
                                                JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        // Listen for further changes, so can refresh again later.
                        establishObserver();
                        progress.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress.finish();
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                opener.execute();

            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }

        };
        worker.execute();

    }

    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {
        loadEntity(rootedEntity);
        try {
            if ( success != null )
                success.call();
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

	public RootedEntity getContextRootedEntity() {
		return slcRootedEntity;
	}
    
    public SampleLocation getSampleLocation() {
        return viewUI.getSampleLocation();
    }
    
    public void setLocation(SampleLocation sampleLocation) {
        viewUI.setSampleLocation(sampleLocation);
    }
    
    public QuadViewUi getQuadViewUi() {
        if (viewUI == null) {
            refresh();
        }
        return viewUI;
    }
    
    public void close() {
        logger.info("Closing");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
        deleteAll();
    }

    public void refresh() {
        // logger.info("Refresh called.");

        if (sliceSample != null) {
            showLoadingIndicator();

            if ( viewUI == null ) {
                viewUI = new QuadViewUi(SessionMgr.getMainFrame(), initialEntity, false);
                neuronSetAdapter.observe(viewUI.getAnnotationModel());
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
    private void establishObserver() {
        modelMgrObserver = new ModelMgrListener( this, sliceSample);
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
    }

    private void deleteAll() {
        clearObserver();
        sliceSample = null;
        initialEntity = null;
        slcRootedEntity = null;
        removeAll();
        if (viewUI != null)
        	viewUI.clearCache();
        viewUI = null;
    }

    private void clearObserver() {
        if ( modelMgrObserver != null ) {
            ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        }
    }

    public NeuronSet getNeuronSetAdapter()
    {
        return neuronSetAdapter;
    }

    //------------------------------Inner Classes
    /** Listens for changes to the child-set of the heard-entity. */
    public static class ModelMgrListener extends ModelMgrAdapter {
        private Entity heardEntity;
        private LargeVolumeViewViewer viewer;
        ModelMgrListener( LargeVolumeViewViewer viewer, Entity e ) {
            heardEntity = e;
            this.viewer = viewer;
        }

        @Override
        public void entityChildrenChanged(long entityId) {
            if (heardEntity.getId() == entityId) {
                viewer.refresh();
            }
        }
    }

    public Entity getSliceSample() { return sliceSample; }
    
}
