package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public interface Object3d extends Child<Object3d>
{

    String getName();

    Matrix4 getTransformInParent();
    
    Matrix4 getTransformInWorld();

    boolean isVisible();

    Object3d setName(String name);

    Object3d setVisible(boolean isVisible);

}
