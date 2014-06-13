package org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements;

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
    void updateCropCoords();
    void exportSelection( SavebackEvent savebackEvent );
    void setCropBlackout( boolean blackout );
    void setConnectEditEvents( boolean connectEditEvents );
    void forceRebuild();
}


