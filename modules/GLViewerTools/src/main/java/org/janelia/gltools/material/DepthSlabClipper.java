package org.janelia.gltools.material;

import org.janelia.gltools.texture.Texture2d;

/**
 * OpenGL rendering shader/material/actor/renderer that explicitly acquires
 * Z-clip slab parameters from a separate opaque render pass.
 * @author brunsc
 */
public interface DepthSlabClipper {
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture);
    public void setRelativeSlabThickness(float zNear, float zFar);
}
