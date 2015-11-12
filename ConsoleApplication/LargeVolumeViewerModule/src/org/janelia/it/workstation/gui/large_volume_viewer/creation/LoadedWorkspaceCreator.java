package org.janelia.it.workstation.gui.large_volume_viewer.creation;

import java.awt.Component;
import java.io.File;
import javax.swing.JOptionPane;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.nb_action.EntityWrapperCreator;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with a known sample, to create a tiled microscope workspace, and load it with
 * SWC input.
 * 
 * @author fosterl
 */
@ServiceProvider(service=EntityWrapperCreator.class,path=EntityWrapperCreator.LOOKUP_PATH)
public class LoadedWorkspaceCreator implements EntityWrapperCreator {
    
    private static final Logger log = LoggerFactory.getLogger(LoadedWorkspaceCreator.class);
    
    private RootedEntity rootedEntity;
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                // Simple dialog: just enter the path.  Should be a server-known path.
                final String userInput = JOptionPane.showInputDialog(
                        mainFrame,
                        "Enter Full Path to Input Folder", "Input Folder",
                        JOptionPane.PLAIN_MESSAGE
                );
                if (userInput != null) {
                    BackgroundWorker importer = new BackgroundWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            String ownerKey = SessionMgr.getSessionMgr().getSubject().getKey();
                            // Expect the sample to be the 'main entity' of the LVV, if there is
                            // no workspace.
                            ModelMgr.getModelMgr().submitSwcImportFolder(
                                    userInput,
                                    ownerKey,
                                    rootedEntity.getEntityId()
                            );
                        }

                        @Override
                        public String getName() {
                            if (userInput != null) {
                                File uiFile = new File(userInput);
                                return "import all SWCs in " + uiFile.getName();
                            } else {
                                return "import SWC for sample";
                            }
                        }

                        @Override
                        protected void hadSuccess() {
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    importer.executeWithEvents();

                }
            }
            
            @Override
            protected void hadSuccess() {
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating SWC-loaded workspace...", ""));
        worker.execute();
    }

    @Override
    public void wrapEntity(RootedEntity e) {
        this.rootedEntity = (RootedEntity)e;
        execute();
    }

    @Override
    public boolean isCompatible(RootedEntity e) {
        setRootedEntity(e);
        if ( e == null ) {
            log.debug("Just nulled-out the rooted entity to LoadedWorkspaceCreator");
            return true;
        }
        else {
            log.debug("Just UN-Nulled rooted entity in LoadedWorkspaceCreator");            
            // Caching the test sampleEntity, for use in action label.
            final String entityTypeName = e.getEntity().getEntityTypeName();
            return entityTypeName.equals( EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE );
        }
    }

    @Override
    public String getActionLabel() {
        return "   Load SWC file on server, and build workspace on sample.";
    }

    /**
     * @param rootedEntity the rootedEntity to set
     */
    private void setRootedEntity(RootedEntity rootedEntity) {
        this.rootedEntity = rootedEntity;
    }

    /**
     * @return the rootedEntity
     */
    private RootedEntity getRootedEntity() {
        return rootedEntity;
    }

}
