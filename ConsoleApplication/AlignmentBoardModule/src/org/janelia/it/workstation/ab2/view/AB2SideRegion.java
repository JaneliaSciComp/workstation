package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLRegion;

public class AB2SideRegion extends GLRegion {
    private boolean isOpen=false;

    public boolean isOpen() { return isOpen; }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {}

}
