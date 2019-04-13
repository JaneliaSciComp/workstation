
package org.janelia.gltools;

import java.util.Collection;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BasicObject3D;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Object3d;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BasicGL3Actor implements GL3Actor {
    private final CompositeObject3d object3d;
    protected boolean isInitialized = false;
    
    public BasicGL3Actor(Object3d parent) {
        object3d = new BasicObject3D(parent);
    }

    @Override
    public Object3d addChild(Object3d child) {
        object3d.addChild(child);
        return this;
    }

    @Override
    public Object3d getParent() {
        return object3d.getParent();
    }

    @Override
    public Collection<? extends Object3d> getChildren() {
        return object3d.getChildren();
    }

    @Override
    public Matrix4 getTransformInWorld() {
        return object3d.getTransformInWorld();
    }

    @Override
    public Matrix4 getTransformInParent() {
        return object3d.getTransformInParent();
    }

    @Override
    public boolean isVisible() {
        return object3d.isVisible();
    }

    @Override
    public Object3d setVisible(boolean isVisible) {
        object3d.setVisible(isVisible);
        return this;
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        if (! isVisible())
            return;
        if (! isInitialized) init(gl);
        if (getChildren() == null)
            return;
        if (getChildren().size() < 1)
            return;
        // Update modelview matrix
        // TODO - use cached version if nothing has changed
        Matrix4 modelViewMatrix = parentModelViewMatrix;
        if (modelViewMatrix == null)
            modelViewMatrix = camera.getViewMatrix();
        Matrix4 localMatrix = getTransformInParent();
        if (localMatrix != null)
            modelViewMatrix = new Matrix4(modelViewMatrix).multiply(localMatrix);
        for (Object3d child : getChildren()) {
            if (child instanceof GL3Actor) {
                ((GL3Actor)child).display(gl, camera, modelViewMatrix);
            }
        }
    }

    @Override
    public void dispose(GL3 gl) {
        for (Object3d child : getChildren()) {
            if (child instanceof GL3Actor)
                ((GL3Actor)child).dispose(gl);
        }
        isInitialized = false;
    }

    @Override
    public void init(GL3 gl) {
        for (Object3d child : getChildren()) {
            if (child instanceof GL3Actor)
                ((GL3Actor)child).init(gl);
        }
        isInitialized = true;
    }

    @Override
    public String getName() {
        return object3d.getName();
    }

    @Override
    public Object3d setName(String name) {
        object3d.setName(name);
        return this;
    }

    @Override
    public Object3d setParent(Object3d parent) {
        object3d.setParent(parent);
        return this;
    }
    
}
