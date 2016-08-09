 package org.janelia.it.workstation.gui.large_volume_viewer;

 import java.awt.BorderLayout;
 import java.util.concurrent.Callable;

 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.SwingUtilities;

 import com.google.common.eventbus.Subscribe;
 import org.janelia.console.viewerapi.SampleLocation;
 import org.janelia.console.viewerapi.model.NeuronSet;
 import org.janelia.it.jacs.model.domain.DomainObject;
 import org.janelia.it.jacs.model.domain.support.DomainUtils;
 import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
 import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
 import org.janelia.it.jacs.shared.lvv.HttpDataSource;
 import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
 import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
 import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
 import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
 import org.janelia.it.workstation.gui.large_volume_viewer.neuron_api.NeuronSetAdapter;
 import org.janelia.it.workstation.gui.util.Icons;
 import org.janelia.it.workstation.gui.util.WindowLocator;
 import org.janelia.it.workstation.shared.workers.SimpleWorker;
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

    private TmSample sliceSample;
    private DomainObject initialObject;
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

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                //  I have found that with very large numbers of
                //  neurons in the neurons table, not reloading
                //  causes GUI lockup.
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        deleteAll();
                    }
                });
                initialObject = domainObject;

                // intial rooted entity should be a brain sample or a workspace; the QuadViewUI wants
                //  the intial entity, but we need the sample either way to be able to open it:
                if (initialObject instanceof TmSample) {
                    sliceSample = (TmSample) initialObject;
                    HttpDataSource.setMouseLightCurrentSampleId(sliceSample.getId());
                }
                else if (initialObject instanceof org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace) {
                    TmWorkspace workspace = (TmWorkspace) initialObject;
                    try {
                        sliceSample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(workspace);
                        HttpDataSource.setMouseLightCurrentSampleId(sliceSample.getId());
                    }
                    catch (Exception e) {
                        logger.error("Error getting sample",e);
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
                        if (sliceSample != null) {
                            if (!viewUI.loadFile(sliceSample.getFilepath())) {
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

    public void loadDomainObject(DomainObject domainObject, Callable<Void> success) {
        loadDomainObject(domainObject);
        try {
            if ( success != null )
                success.call();
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
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
        deleteAll();
    }

    public void refresh() {
        // logger.info("Refresh called.");

        if (sliceSample != null) {
            showLoadingIndicator();

            if ( viewUI == null ) {
                viewUI = new QuadViewUi(SessionMgr.getMainFrame(), initialObject, false);
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

    private void deleteAll() {
        sliceSample = null;
        initialObject = null;
        removeAll();
        if (viewUI != null)
        	viewUI.clearCache();
        viewUI = null;
    }

    public NeuronSet getNeuronSetAdapter() {
        return neuronSetAdapter;
    }

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            // Ignore this for now because it's annoying to reload the LVV each time, 
            // but we really should figure out how to handle it. 
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
