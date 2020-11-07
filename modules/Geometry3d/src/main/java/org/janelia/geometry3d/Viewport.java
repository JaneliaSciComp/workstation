
package org.janelia.geometry3d;

import org.janelia.geometry3d.camera.ConstViewport;

/**
 * Rectangular viewing window region where scene rendering occurs
 * 
 * @author brunsc
 */
public class Viewport implements ViewSlab, ConstViewport
{
    private int originXPixels = 0;
    private int originYPixels = 0;
    private int widthPixels = 0;
    private int heightPixels = 0;
    private float zNearRelative = 0.5f;
    private float zFarRelative = 10.0f;
    private final ComposableObservable changeObservable = new ComposableObservable();

    public float getAspect() {
        if (heightPixels <= 0)
            return 1.0f;
        if (widthPixels <= 0)
            return 1.0f;
        return widthPixels/(float)heightPixels;
    }

    @Override
    public ComposableObservable getChangeObservable() {
        return changeObservable;
    }

    public int getOriginXPixels() {
        return originXPixels;
    }

    public int getOriginYPixels() {
        return originYPixels;
    }

    public void setOriginXPixels(int originXPixels) {
        if (originXPixels == this.originXPixels)
            return;
        this.originXPixels = originXPixels;
        changeObservable.setChanged();
    }

    public void setOriginYPixels(int originYPixels) {
        if (originYPixels == this.originYPixels)
            return;
        this.originYPixels = originYPixels;
        changeObservable.setChanged();
    }

    public int getWidthPixels() {
        return widthPixels;
    }

    public void setWidthPixels(int widthPixels) {
        if (widthPixels == this.widthPixels)
            return;
        this.widthPixels = widthPixels;
        changeObservable.setChanged();
    }

    public int getHeightPixels() {
        return heightPixels;
    }

    public void setHeightPixels(int heightPixels) {
        if (heightPixels == this.heightPixels)
            return;
        this.heightPixels = heightPixels;
        changeObservable.setChanged();
    }

    @Override
    public float getzNearRelative() {
        return zNearRelative;
    }

    @Override
    public void setzNearRelative(float zNearRelative) {
        if (zNearRelative == this.zNearRelative)
            return;
        this.zNearRelative = zNearRelative;
        changeObservable.setChanged();
    }

    @Override
    public float getzFarRelative() {
        return zFarRelative;
    }

    @Override
    public void setzFarRelative(float zFarRelative) {
        if (zFarRelative == this.zFarRelative)
            return;
        this.zFarRelative = zFarRelative;
        changeObservable.setChanged();
    }
    
}
