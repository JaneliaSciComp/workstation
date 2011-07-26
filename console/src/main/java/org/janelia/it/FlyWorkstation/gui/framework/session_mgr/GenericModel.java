package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.FlyWorkstation.shared.util.EmptyIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
* The GenericModel is a generic observer model for the components of the browser.
* Changes to any of these elements will broadcast events notifing all listeners of
* the change.
*
* Initially written by: Todd Safford
*/

public abstract class GenericModel {
  protected ArrayList<GenericModelListener> modelListeners=new ArrayList<GenericModelListener>();
  protected TreeMap modelProperties;

  public GenericModel(){
      modelProperties= new TreeMap();
      modelListeners = new ArrayList<GenericModelListener>();
  }  //Constructor can only be called within the package



  void addModelListener(GenericModelListener modelListener) {
    if (!modelListeners.contains(modelListener)) modelListeners.add(modelListener);
  }

  void removeModelListener(GenericModelListener modelListener) {
    modelListeners.remove(modelListener);
  }

  /**
   * @return The previous value of the this key or null
   */
  public Object setModelProperty(Object key, Object newValue) {
     if (modelProperties==null) modelProperties=new TreeMap();
     Object oldValue = modelProperties.put(key,newValue);
     fireModelPropertyChangeEvent(key, oldValue, newValue);
     return oldValue;
  }

  public Object getModelProperty (Object key) {
    if (modelProperties==null) return null;
    return modelProperties.get(key);
  }

  int sizeofProperties() {
    if (!modelProperties.isEmpty()) return modelProperties.size();
    else return 0;
  }

  public Iterator getModelPropertyKeys() {
    if (modelProperties==null) return new EmptyIterator();
    return modelProperties.keySet().iterator();
  }

  protected TreeMap getModelProperties() { return modelProperties; }

  protected void setModelProperties(TreeMap modelProperties) {
    this.modelProperties = modelProperties;
  }

  private void fireModelPropertyChangeEvent(Object key, Object oldValue, Object newValue) {
        GenericModelListener modelListener;
      for (Object modelListener1 : modelListeners) {
          modelListener = (GenericModelListener) modelListener1;
          modelListener.modelPropertyChanged(key, oldValue, newValue);
      }
  }

}
