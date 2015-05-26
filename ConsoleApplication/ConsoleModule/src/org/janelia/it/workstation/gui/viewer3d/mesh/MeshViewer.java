/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.mesh;

import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.Viewer3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.mesh.MeshRenderer;

/**
 * Special viewer to support carrying around extra information needed for mesh.
 *
 * @author fosterl
 */
public class MeshViewer extends Viewer3d {
    private MeshViewContext context;
    
	public MeshViewer() {
        final MeshRenderer meshRenderer = new MeshRenderer();
        setActorRenderer( meshRenderer);
        context = new MeshViewContext();
        super.setVolumeModel(context);
        meshRenderer.setMeshViewContext(context);
    }
    
    @Override
    public VolumeModel getVolumeModel() {
        return context;
    }
    
    public MeshViewContext getMeshViewContext() {
        return context;
    }
}
