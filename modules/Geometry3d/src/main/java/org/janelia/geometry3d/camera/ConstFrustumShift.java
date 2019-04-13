package org.janelia.geometry3d.camera;

/**
 * For use in stereoscopic and lenticular viewpoints
 * @author brunsc
 */
public interface ConstFrustumShift {
    float getShiftXPixels(); // signed horizontal shift, in real-world your-monitor not-scene pixels
    float getShiftYPixels(); // signed vertical shift, in real-world your-monitor not-scene pixels
}
