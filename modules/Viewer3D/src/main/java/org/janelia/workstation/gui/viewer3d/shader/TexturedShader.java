package org.janelia.workstation.gui.viewer3d.shader;

import org.janelia.workstation.gui.viewer3d.texture.TextureMediator;

/**
 * This type of shader takes at least one texture mediator.
 * 
 * @author fosterl
 */
public abstract class TexturedShader extends AbstractShader {
    public static final String SIGNAL_TEXTURE_NAME = "signalTexture";
    public abstract void addTextureMediator(TextureMediator textureMediator, String name );
}
