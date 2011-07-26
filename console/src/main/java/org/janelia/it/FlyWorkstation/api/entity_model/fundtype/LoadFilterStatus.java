package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

/**
 * @author Peter Davies
 */

public class LoadFilterStatus implements java.io.Serializable {

  private static final long serialVersionUID=1;
  private LoadFilter loadFilter;
  private boolean completelyLoaded;

  LoadFilterStatus(LoadFilter loadFilter) {
     this.loadFilter=loadFilter;
  }

  public boolean isCompletelyLoaded() {
     return completelyLoaded;
  }

  protected void setCompletelyLoaded(boolean completelyLoaded){
     this.completelyLoaded=completelyLoaded;
  }

  /**
   * This gets called by the LoadRequestStatus when the state changes to Complete
   */
  void requestCompleted(LoadRequest request){
//     if (request.isBinRequest() || request.isRangeRequest()) throw
//        new IllegalStateException("The request made with the filter is either a"+
//          " range or binned request, but the LoadFilterStatus is not the right class for these." );
     completelyLoaded=true;
  }


}