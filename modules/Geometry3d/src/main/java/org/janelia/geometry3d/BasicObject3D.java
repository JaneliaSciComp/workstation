
package org.janelia.geometry3d;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BasicObject3D implements CompositeObject3d {
    private Object3d parent;
    private final List<Object3d> children;
    private final Matrix4 transformInWorld;
    private final Matrix4 transformInParent;
    private boolean isVisible = true;
    private String name = "Object3D";
    
    public BasicObject3D(Object3d parent) {
        this.parent = parent;
        children = new LinkedList<Object3d>();
        transformInWorld = new Matrix4();
        transformInParent = new Matrix4();
    }
    
    @Override
    public CompositeObject3d addChild(Object3d child) {
        children.add(child);
        return this;
    }

    @Override
    public Object3d getParent() {
        return parent;
    }

    @Override
    public Collection<? extends Object3d> getChildren() {
        return children;
    }

    @Override
    public Matrix4 getTransformInWorld() {
        return transformInWorld;
    }

    @Override
    public Matrix4 getTransformInParent() {
        return transformInParent;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public Object3d setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        return this;
    }

    @Override
    public Object3d setName(String name) {
        this.name = name;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object3d setParent(Object3d parent) {
        this.parent = parent;
        return this;
    }

}
