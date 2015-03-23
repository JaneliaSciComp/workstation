package org.janelia.it.workstation.gui.large_volume_viewer.style;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

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
    // possibly just for testing...
    private static int defaultColorIndex = 0;

    /**
     * test method: I need to set some styles w/o a UI, but also
     * w/o them being strictly hard-coded
     */
    public static NeuronStyle getStyleForNeuron(Long neuronID) {
        return new NeuronStyle(neuronColors[(int) (neuronID % neuronColors.length)], true);
    }

    public static NeuronStyle getDefaultStyle() {
        return new NeuronStyle(neuronColors[defaultColorIndex], true);
    }

    /**
     * given a json object, return a NeuronStyle; expected to
     * be in form {"color", [R, G, B in 0-255], "visibility": true/false}
     *
     * returns null if it can't parse the input JSON node
     *
     * @param rootNode
     * @return
     */
    public static NeuronStyle fromJSON(ObjectNode rootNode) {
        JsonNode colorNode = rootNode.path("color");
        if (colorNode.isMissingNode() || !colorNode.isArray()) {
            return null;
        }
        Color color = new Color(colorNode.get(0).asInt(), colorNode.get(1).asInt(),
                colorNode.get(2).asInt());

        JsonNode visibilityNode = rootNode.path("visible");
        if (visibilityNode.isMissingNode() || !visibilityNode.isBoolean()) {
            return null;
        }
        boolean visibility = visibilityNode.asBoolean();

        return new NeuronStyle(color, visibility);
    }

    public NeuronStyle(Color color, boolean visible) {
        this.color = color;
        this.visible = visible;
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

    public float[] getColorAsFloatArray() {
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

    /**
     * returns a json node object, to be used in persisting styles
     *
     * @return
     */
    public ObjectNode asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        ArrayNode colors = mapper.createArrayNode();
        colors.add(getColor().getRed());
        colors.add(getColor().getGreen());
        colors.add(getColor().getBlue());
        rootNode.put("color", colors);

        rootNode.put("visibility", isVisible());

        return rootNode;
    }
}
