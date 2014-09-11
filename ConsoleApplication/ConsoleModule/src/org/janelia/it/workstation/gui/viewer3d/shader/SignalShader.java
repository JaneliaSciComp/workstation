/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.shader;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;

/**
 * This type of shader takes a signal texture mediator.
 * 
 * @author fosterl
 */
public abstract class SignalShader extends AbstractShader {
    public abstract void setSignalTextureMediator( TextureMediator textureMediator );
}
