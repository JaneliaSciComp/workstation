package org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/18/13
 * Time: 12:27 PM
 *
 * Encapsulates the information needed for a volume saveback.
 */
public class SavebackEvent {
    private Collection<float[]> absoluteCoords;
    private CompletionListener completionListener;
    private ControlsListener.ExportMethod method;
    private double gammaFactor;

    public SavebackEvent() {
    }


    public Collection<float[]> getAbsoluteCoords() {
        return absoluteCoords;
    }

    public void setAbsoluteCoords(Collection<float[]> absoluteCoords) {
        this.absoluteCoords = absoluteCoords;
    }

    public CompletionListener getCompletionListener() {
        return completionListener;
    }

    public void setCompletionListener(CompletionListener completionListener) {
        this.completionListener = completionListener;
    }

    public ControlsListener.ExportMethod getMethod() {
        return method;
    }

    public void setMethod(ControlsListener.ExportMethod method) {
        this.method = method;
    }

    public double getGammaFactor() {
        return gammaFactor;
    }

    public void setGammaFactor(double gammaFactor) {
        this.gammaFactor = gammaFactor;
    }
}
