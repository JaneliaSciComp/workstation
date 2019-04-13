package org.janelia.scenewindow;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 *
 * @author brunsc
 */
public class MyJXTaskPaneBeanInfo extends SimpleBeanInfo implements BeanInfo 
{
    private BeanDescriptor bd = new BeanDescriptor(MyJXTaskPane.class);

    public MyJXTaskPaneBeanInfo() {
        bd.setName("MyJXTaskPane");
        bd.setShortDescription("Copy of JXTaskPane, a collapsible container for tasks and other components");
        bd.setValue("isContainer", Boolean.TRUE);
        bd.setValue("containerDelegate", "getContentPane");
    }

    @Override
    public BeanDescriptor getBeanDescriptor()
    {
        return bd;
    }
    
}
