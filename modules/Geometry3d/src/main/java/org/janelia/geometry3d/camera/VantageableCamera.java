package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public interface VantageableCamera {
    ConstVantage getVantage();
    void setVantage(ConstVantage vantage);
}
