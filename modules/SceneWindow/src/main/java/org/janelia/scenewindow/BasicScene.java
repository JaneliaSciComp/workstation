
package org.janelia.scenewindow;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import org.janelia.geometry3d.BasicObject3D;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Object3d;
import org.janelia.geometry3d.Vantage;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BasicScene implements Scene {
    private static int count = 0;
    
    private final int index;
    private CompositeObject3d object3d = new BasicObject3D(null);
    private List<Light> lights = new Vector<Light>();
    private List<Vantage> cameras = new Vector<Vantage>();
    
    public BasicScene() {
        index = count++;
    }

    public BasicScene(Vantage vantage) {
        add(vantage);
        if (vantage.getParent() == null)
            vantage.setParent(this);
        index = count++;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Vantage getVantage() {
        return cameras.get(0);
    }

    @Override
    public Collection<? extends Light> getLights() {
        return lights;
    }

    @Override
    public Collection<? extends Vantage> getCameras() {
        return cameras;
    }

    @Override
    public Scene add(Light light) {
        lights.add(light);
        return this;
    }

    @Override
    public Scene add(Vantage camera) {
        cameras.add(camera);
        return this;
    }

    @Override
    public CompositeObject3d addChild(Object3d child) {
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
