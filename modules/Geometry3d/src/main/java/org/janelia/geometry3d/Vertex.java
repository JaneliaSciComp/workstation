
package org.janelia.geometry3d;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Vertex {
    private final Map<String, ConstVector> vectorAttributes = new LinkedHashMap<>();
    // private final Map<String, Vector4> vector4Attributes = new HashMap<String, Vector4>();
    private final Map<String, Float> floatAttributes = new LinkedHashMap<>();
    
    public Vertex(ConstVector3 position) {
        setAttribute("position", position);
    }
    
    public float getFloatAttribute(String attribute) {
        return floatAttributes.get(attribute);
    }
    
    public ConstVector getVectorAttribute(String attribute) {
        return vectorAttributes.get(attribute);
    }
    
    public Vector3 getPosition() {
        return (Vector3)getVectorAttribute("position");
    }
    
    public boolean hasAttribute(String attribute) {
        return vectorAttributes.containsKey(attribute)
                || floatAttributes.containsKey(attribute);
    }
    
    public Vertex setAttribute(String attributeName, float value) {
        floatAttributes.put(attributeName, value);
        return this;
    }
    
    public final Vertex setAttribute(String attributeName, ConstVector value) {
        vectorAttributes.put(attributeName, value);
        return this;
    }

    public Iterable<String> getVectorAttributeNames() {
        return vectorAttributes.keySet();
    }

    public Iterable<String> getFloatAttributeNames() {
        return floatAttributes.keySet();
    }
}
