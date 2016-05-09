package org.janelia.it.workstation.gui.large_volume_viewer.style;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.*;

/**
 * This class encapsulates the visual draw style for a particular neuron;
 * we'll start with color and visibility, and perhaps later add other
 * things like line width, etc.
 */
public class NeuronStyle {

    private Color color;
    private boolean visible = true;
    float[] cachedColorArray=new float[3]; // needed for performance

    // constants, because I already used "visible" instead of "visibility" once...
    private static final String COLOR_KEY = "color";
    private static final String VISIBILITY_KEY = "visibility";

    // default colors; we index into this list with neuron ID;
    //  note that our neuron IDs are all of the form 8*n+4,
    //  so make sure the length of this list is mutually prime,
    //  so we can maximize the color distribution
    private static Color[] neuronColors = {
        Color.red,
        Color.blue,
        Color.green,
        Color.magenta,
        Color.cyan,
        Color.yellow,
        Color.white,
        // I need more colors!  (1, 0.5, 0) and permutations:
        new Color(1.0f, 0.5f, 0.0f),
        new Color(0.0f, 0.5f, 1.0f),
        new Color(0.0f, 1.0f, 0.5f),
        new Color(1.0f, 0.0f, 0.5f),
        new Color(0.5f, 0.0f, 1.0f),
        new Color(0.5f, 1.0f, 0.0f)
    };

    /**
     * get a default style for a neuron
     */
    public static NeuronStyle getStyleForNeuron(Long neuronID) {
        return new NeuronStyle(neuronColors[(int) (neuronID % neuronColors.length)], true);
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
        JsonNode colorNode = rootNode.path(COLOR_KEY);
        if (colorNode.isMissingNode() || !colorNode.isArray()) {
            return null;
        }
        Color color = new Color(colorNode.get(0).asInt(), colorNode.get(1).asInt(),
                colorNode.get(2).asInt());

        JsonNode visibilityNode = rootNode.path(VISIBILITY_KEY);
        if (visibilityNode.isMissingNode() || !visibilityNode.isBoolean()) {
            return null;
        }
        boolean visibility = visibilityNode.asBoolean();

        return new NeuronStyle(color, visibility);
    }

    public NeuronStyle(Color color, boolean visible) {
        setColor(color);
        this.visible = visible;
    }

    public float getRedAsFloat() {
        return cachedColorArray[0];
    }

    public float getGreenAsFloat() {
        return cachedColorArray[1];
    }

    public float getBlueAsFloat() {
        return cachedColorArray[2];
    }

    public Color getColor() {
        return color;
    }

    public float[] getColorAsFloatArray() {
        return cachedColorArray;
    }

    public void setColor(Color color) {
        cachedColorArray[0]=color.getRed() / 255.0f;
        cachedColorArray[1]=color.getGreen() / 255.0f;
        cachedColorArray[2]=color.getBlue() / 255.0f;
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * returns a json node object, to be used in persisting styles; the
     * node will be aggregated with others before converting to string
     */
    public ObjectNode asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        ArrayNode colors = mapper.createArrayNode();
        colors.add(getColor().getRed());
        colors.add(getColor().getGreen());
        colors.add(getColor().getBlue());
        rootNode.put(COLOR_KEY, colors);

        rootNode.put(VISIBILITY_KEY, isVisible());

        return rootNode;
    }

    @Override
    public String toString() {
        return "NeuronStyle(" + color + ", visibility: " + visible + ")";
    }
}
