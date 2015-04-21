/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.publication_quality.mesh;

import org.janelia.it.workstation.gui.viewer3d.Viewer3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.publication_quality.mesh.actor.MeshRenderer;

/**
 * Special viewer to support carrying around extra information needed for mesh.
 *
 * @author fosterl
 */
public class MeshViewer extends Viewer3d {
    private ExtendedVolumeModel volumeModel;
    
	public MeshViewer() {
        final MeshRenderer meshRenderer = new MeshRenderer();
        setActorRenderer( meshRenderer);
        volumeModel = new ExtendedVolumeModel( super.getVolumeModel() );
        meshRenderer.setVolumeModel(volumeModel);
    }
    
    @Override
    public VolumeModel getVolumeModel() {
        return volumeModel;
    }
    
    public static class ExtendedVolumeModel extends VolumeModel {
        private float[] perspectiveMatrix;
        private float[] modelViewMatrix;
        
        private VolumeModel wrapped;
        public ExtendedVolumeModel( VolumeModel wraped ) {
            this.wrapped = wrapped;
        }
        
        public float[] getPerspectiveMatrix() {
            return perspectiveMatrix;
        }

        public void setPerspectiveMatrix(float[] perspectiveMatrix) {
            this.perspectiveMatrix = perspectiveMatrix;
        }

        public float[] getModelViewMatrix() {
            return modelViewMatrix;
        }

        public void setModelViewMatrix(float[] modelViewMatrix) {
            this.modelViewMatrix = modelViewMatrix;
        }        
    }
}
