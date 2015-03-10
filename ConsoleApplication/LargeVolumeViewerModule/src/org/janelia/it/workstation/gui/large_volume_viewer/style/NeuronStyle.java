package org.janelia.it.workstation.gui.large_volume_viewer.style;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

import java.awt.*;

/**
 * This class encapsulates the visual draw style for a particular neuron;
 * we'll start with color and visibility, and perhaps later add other
 * things like line width, etc.
 */
public class NeuronStyle {

    private Color color;
    private boolean visible = true;

    // some nice colors we can use for testing or defaults;
    //  avoid having 8 in the list, as our neuron IDs are
    //  apparently all of the form 8*n+4 (neuronID % 8 == 4)
    private static Color[] neuronColors = {
        Color.red,
        Color.blue,
        Color.green,
        Color.magenta,
        Color.cyan,
        Color.yellow,
        Color.pink
    };

    public NeuronStyle(Color color, boolean visible) {
        this.color = color;
        this.visible = visible;
    }

    /**
     * test method: I need to set some styles w/o a UI, but also
     * w/o them being strictly hard-coded
     */
    public static NeuronStyle getStyleForNeuron(Long neuronID) {
        return new NeuronStyle(neuronColors[(int) (neuronID % neuronColors.length)], true);
    }

    public float getRedAsFloat() {
        return getColor().getRed() / 255.0f;
    }

    public float getGreenAsFloat() {
        return getColor().getGreen() / 255.0f;
    }

    public float getBlueAsFloat() {
        return getColor().getBlue() / 255.0f;
    }

    public Color getColor() {
        return color;
    }

    public float[] getColorAsFloats() {
        return new float[] {getRedAsFloat(), getGreenAsFloat(), getBlueAsFloat()};
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
