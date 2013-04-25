package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/25/13
 * Time: 2:21 PM
 *
 * Callers should implement this to observe the input settings provided by uesr.
 */
public interface ControlsListener {
    public enum ExportMethod { binary, color, mip }
    void setBrightness( double brightness );
    void updateSettings();
    void setSelectedCoords( float[] normalizedCoords );
    void exportSelection(float[] absoluteCoords, CompletionListener completionListener, ExportMethod method );
    void setCropBlackout( boolean blackout );
}


