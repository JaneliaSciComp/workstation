package org.janelia.it.workstation.api.entity_model.fundtype;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:00 PM
 */
public class TaskRequest implements Serializable {

    //  private RangeSet ranges;
//  private Bin bin;
    private TaskFilter taskFilter;
    private TaskRequestStatus taskRequestStatus;
    private boolean isUnloadRequest;


    public TaskRequest(TaskFilter taskFilter) {
        this.taskFilter = taskFilter;
        taskRequestStatus = new TaskRequestStatus(taskFilter);
    }

//  public TaskRequest(Range range,TaskFilter taskFilter) {
//     this(range,taskFilter,false);
//  }
//
//  public TaskRequest(Range range,TaskFilter taskFilter,boolean isUnloadRequest) {
//     this(taskFilter);
//     this.isUnloadRequest=isUnloadRequest;
//     TaskFilterStatus lfStatus=taskFilter.getTaskFilterStatus();
//     if (!isUnloadRequest && lfStatus instanceof RangeLoadFilterStatus) {
//           RangeLoadFilterStatus rlfStatus=(RangeLoadFilterStatus)lfStatus;
//           ranges=rlfStatus.processRangeRequest(range);
//     }
//     else {
//       ranges=new RangeSet();
//       ranges.add(range);
//     }
//  }
//
//  public TaskRequest(Bin bin,TaskFilter taskFilter) {
//     this(taskFilter);
//     this.bin=bin;
//  }
//
//  public boolean isRangeRequest() {
//     return ranges!=null;
//  }

    public boolean isUnloadRequest() {
        return isUnloadRequest;
    }

//  public boolean isBinRequest() {
//     return bin!=null;
//  }
//
//  public Bin getBin() {
//     return bin;
//  }
//
//  public Set getRequestedRanges() {
//    if (ranges==null) return new TreeSet();
//    return Collections.unmodifiableSet(ranges);
//  }

    public TaskFilter getTaskFilter() {
        return taskFilter;
    }

    public TaskRequestStatus getTaskRequestStatus() {
        return taskRequestStatus;
    }
}