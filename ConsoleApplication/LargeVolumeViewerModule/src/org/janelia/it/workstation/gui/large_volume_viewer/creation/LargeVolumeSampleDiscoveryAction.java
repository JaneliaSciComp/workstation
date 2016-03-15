package org.janelia.it.workstation.gui.large_volume_viewer.creation;

import java.util.HashSet;
import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.LargeVolumeDiscoveryTask;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.nb_action.EntityWrapperCreator;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=EntityWrapperCreator.class,path=EntityWrapperCreator.LOOKUP_PATH)
public class LargeVolumeSampleDiscoveryAction implements EntityWrapperCreator {
	private static final long serialVersionUID = 1L;
    
    private RootedEntity rootedEntity;

	public LargeVolumeSampleDiscoveryAction() {
	}
	
	public void execute() {
        // Let user decide if it's a go.
        int optionSelected = JOptionPane.showConfirmDialog(
                SessionMgr.getMainFrame(),
                "Launch Discovery Process Now?",
                "Launch Large Volume Sample Discovery",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (optionSelected == JOptionPane.OK_OPTION) {
            SimpleWorker simpleWorker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    String displayName = "Update the set of known Large Volume Samples";
                    try {
                        final Task task = ModelMgr.getModelMgr().submitJob(LargeVolumeDiscoveryTask.PROCESS_NAME, displayName, taskParameters);
                        // Launch another thread/worker to monitor the remote-running task.
                        TaskMonitoringWorker tmw = new TaskMonitoringWorker(task.getObjectId()) {
                            @Override
                            public void doStuff() throws Exception {
                                super.doStuff();
                            }

                            @Override
                            public String getName() {
                                return "Discover Large Volume Samples";
                            }

                        };
                        tmw.executeWithEvents();
                    } catch (Exception ex) {
                        SessionMgr.getSessionMgr().handleException(ex);
                        ex.printStackTrace();
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
            simpleWorker.execute();
        }
	}

    @Override
    public void wrapEntity(RootedEntity e) {
        this.rootedEntity = e;
        execute();
    }

    @Override
    public boolean isCompatible(RootedEntity e) {
        this.rootedEntity = e;
        if (e == null) {
            return true;
        }
        final String entityTypeName = e.getEntity().getEntityTypeName();
        return entityTypeName.equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE);
    }

    @Override
    public String getActionLabel() {
        return "  Discover Large Volume Samples";
    }
}
