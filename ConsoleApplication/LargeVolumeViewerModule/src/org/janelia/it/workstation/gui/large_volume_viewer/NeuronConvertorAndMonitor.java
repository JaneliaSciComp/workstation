package org.janelia.it.workstation.gui.large_volume_viewer;

import java.util.ArrayList;
import java.util.HashSet;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.TmNeuronPBUpdateTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.workstation.api.entity_model.fundtype.TaskRequest;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;

/**
 * Given a workspace which is being converted, this class will start a 
 * thread to periodically check its progress, and update a progress
 * handle in NetBeans platform.
 * 
 * NOTE: this is an abandoned experiment.  It would have most likely worked,
 * but was found to be unnecessary.  In future this kind of timing may again
 * be relevant.
 * 
 * @author fosterl
 * @deprecated 
 */
public class NeuronConvertorAndMonitor {
    private Runnable updateRunnable;
    public NeuronConvertorAndMonitor(Entity workspaceEntity) {
        if (! workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            throw new IllegalArgumentException("This monitor only tracks workspaces.");
        }
        updateRunnable = new UpdateRunnable(workspaceEntity);
    }
    
    public void exec() {
        new Thread(updateRunnable).start();
    }
    
    private static final class UpdateRunnable implements Runnable {
        private Entity workspaceEntity;
        public UpdateRunnable( Entity workspaceEntity ) {
            this.workspaceEntity = workspaceEntity;
        }
        
        @Override
        public void run() {
            try {
                // NOTE: in order for task monitoring at percentage level
                // (that is, determinate) to work, I would need to ensure that
                // the task ID is passed into the "tiledMicroscopeDAO.loadWorkspace(wsID)"
                // method.  That is not yet being done.  Tabling this change
                // for now.  May adopt different/convert-all-offline approach.
                
                // Setup a task that does the conversion.
                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter(TmNeuronPBUpdateTask.PARAM_workspaceId, workspaceEntity.getId().toString(), null));
                Task task = new GenericTask(new HashSet<Node>(), "group:mouselight", new ArrayList<Event>(),
                        taskParameters, TmNeuronPBUpdateTask.PROCESS_NAME, TmNeuronPBUpdateTask.PROCESS_NAME);
                task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
                TaskRequest updateRequest = ModelMgr.getModelMgr().submitJob("Update TmNeuron to ProtoBuf", task);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
