package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Annotation controller for Basic Tracing sessions (i.e. Workspaces.)
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkspaceAnnotationManager extends BasicAnnotationManager {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAnnotationManager.class);
    
    private TmWorkspace initialObject;
    
    public WorkspaceAnnotationManager(TmWorkspace workspace) {
        this.initialObject = workspace;
    }
    
    @Override
    public TmWorkspace getInitialObject() {
        return initialObject;
    }
    
    @Override
    public void load(final SimpleWorker volumeLoader) {
                
        log.info("loadDomainObject({})", initialObject);
        
        SimpleWorker worker = new SimpleWorker() {

            private TmSample sliceSample;
            
            @Override
            protected void doStuff() throws Exception {
                try {
                    sliceSample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(initialObject);
                    log.info("Found sample {}", sliceSample.getId());
                }
                catch (Exception e) {
                    log.error("Error getting sample for "+initialObject, e);
                }
            }

            @Override
            protected void hadSuccess() {
                
                if (sliceSample == null) {
                    JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                            "Could not find the supporting sample",
                            "Could not open workspace",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                HttpDataSource.setMouseLightCurrentSampleId(sliceSample.getId());

                final ProgressHandle progress2 = ProgressHandleFactory.createHandle("Loading metadata...");
                progress2.start();
                progress2.setDisplayName("Loading metadata");
                progress2.switchToIndeterminate();
                
                SimpleWorker workspaceLoader = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        annotationModel.loadWorkspace(initialObject);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Metadata loading completed");
                        progress2.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress2.finish();
                        ConsoleApp.handleException(error);
                    }
                };

                // Execute both futures
                SimpleListenableFuture future1 = volumeLoader.executeWithFuture();
                SimpleListenableFuture future2 = workspaceLoader.executeWithFuture();
                
                // Join the two futures
                ListenableFuture<List<Boolean>> combinedFuture = Futures.allAsList(Arrays.asList(future1, future2));
                Futures.addCallback(combinedFuture, new FutureCallback<List<Boolean>>() {
                    public void onSuccess(List<Boolean> result) {
                        // If both loads succeeded
                        log.info("Loading completed");
                        annotationModel.loadComplete();
                    }
                    public void onFailure(Throwable t) {
                        loadFailed = true;
                        // If either load failed
                        log.error("LVVV load failed", t);
                        try {
                            if (annotationModel!=null) {
                                annotationModel.clear();
                                annotationModel.loadComplete();
                            }
                        }
                        catch (Exception e) {
                            log.error("Error loading empty workspace",e);
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
    
    //-------------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoaded(URL url) {
        activityLog.setTileFormat(tileServer.getLoadAdapter().getTileFormat(), getSampleID());
    }
}
