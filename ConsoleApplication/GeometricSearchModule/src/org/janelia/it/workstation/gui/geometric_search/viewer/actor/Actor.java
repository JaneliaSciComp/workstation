package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;

/**
 * Created by murphys on 8/6/2015.
 */
public abstract class Actor {

    String name;
    GL4SimpleActor glActor;
    Vector4 color;
    float brightness=1.0f;
    float transparency=1.0f;

    public Vector4 getColor() {
        return color;
    }

    public void setColor(Vector4 color) {
        this.color = color;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        this.transparency = transparency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract GL4SimpleActor createAndSetGLActor();

    public void dispose() {}

    public boolean isVisible() {
        return glActor.isVisible();
    }

    public void setIsVisible(boolean isVisible) {
        glActor.setIsVisible(isVisible);
    }

}
