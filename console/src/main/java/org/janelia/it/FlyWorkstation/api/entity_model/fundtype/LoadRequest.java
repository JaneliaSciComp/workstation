package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:00 PM
 */
public class LoadRequest implements java.io.Serializable{

//  private RangeSet ranges;
//  private Bin bin;
  private LoadFilter loadFilter;
  private LoadRequestStatus loadRequestStatus;
  private boolean isUnloadRequest;


  public LoadRequest(LoadFilter loadFilter) {
    this.loadFilter=loadFilter;
    loadRequestStatus=new LoadRequestStatus(loadFilter);
  }

//  public LoadRequest(Range range,LoadFilter loadFilter) {
//     this(range,loadFilter,false);
//  }
//
//  public LoadRequest(Range range,LoadFilter loadFilter,boolean isUnloadRequest) {
//     this(loadFilter);
//     this.isUnloadRequest=isUnloadRequest;
//     LoadFilterStatus lfStatus=loadFilter.getLoadFilterStatus();
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
//  public LoadRequest(Bin bin,LoadFilter loadFilter) {
//     this(loadFilter);
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

  public LoadFilter getLoadFilter() {
     return loadFilter;
  }

  public LoadRequestStatus getLoadRequestStatus() {
     return loadRequestStatus;
  }
}