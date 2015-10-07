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
    Actor proxyActor;
    boolean masked=false;
    boolean visible=true;

    public Actor getProxyActor() {
        return proxyActor;
    }

    public void setProxyActor(Actor proxyActor) {
        this.proxyActor = proxyActor;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        if (masked) {
            glActor.setIsVisible(false);
        } else {
            glActor.setIsVisible(this.visible);
        }
        this.masked = masked;
    }

    public Vector4 getColor() {
        if (proxyActor!=null) {
            return proxyActor.getColor();
        }
        return color;
    }

    public void setColor(Vector4 color) {
        this.color = color;
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
        return visible;
    }

    public void setIsVisible(boolean isVisible) {
        this.visible=isVisible;
        if (!masked) {
            glActor.setIsVisible(isVisible);
        }
    }

    public GL4SimpleActor getGlActor() { return glActor; }

}
