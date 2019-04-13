package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public interface SlabbableCamera {
    // "Nominal" view slab as the viewer sees and understands it
    // Changes to nominal view slab ARE communicated to camera listeners
    ConstViewSlab getNominalViewSlab();
    void setNominalViewSlab(ConstViewSlab slab);
    
    // "Internal" view slab, which gets incorporated into the computed projectionMatrix,
    // for use by clever imposter meshes.
    // Changes to internal view slab are NOT communicated to camera listeners
    void pushInternalViewSlab(ConstViewSlab slab);
    ConstViewSlab popInternalViewSlab();
}
