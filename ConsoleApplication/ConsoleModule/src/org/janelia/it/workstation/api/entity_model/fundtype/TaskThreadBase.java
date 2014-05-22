package org.janelia.it.workstation.api.entity_model.fundtype;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 4/2/13
 * Time: 1:28 PM
 */
public abstract class TaskThreadBase implements Runnable, java.io.Serializable {

        protected TaskRequest taskRequest;
        protected TaskRequestStatus taskRequestStatus;

        protected TaskThreadBase(TaskRequest taskRequest){
            this.taskRequest =taskRequest;
            if (taskRequest == null) {
                this.taskRequestStatus = null;
            }
            else {
                this.taskRequestStatus =taskRequest.getTaskRequestStatus();
            }
        }
    }
