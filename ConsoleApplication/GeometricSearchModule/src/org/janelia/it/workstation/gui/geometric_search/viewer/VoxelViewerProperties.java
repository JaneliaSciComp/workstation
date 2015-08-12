package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderProperties;

import java.util.HashMap;

/**
 * Created by murphys on 7/29/2015.
 */
public class VoxelViewerProperties implements GL4ShaderProperties {

    private HashMap<String, Object> propertyMap = new HashMap<>();

    public static final String GL_VIEWER_WIDTH_INT = "GL_VIEWER_WIDTH_INT";
    public static final int GL_VIEWER_WIDTH_INT_VALUE = 1200;

    public static final String GL_VIEWER_HEIGHT_INT = "GL_VIEWER_HEIGHT_INT";
    public static final int GL_VIEWER_HEIGHT_INT_VALUE = 800;

    public static final String GL_TRANSPARENCY_QUARTERDEPTH_INT = "GL_TRANSPARENCY_QUARTERDEPTH_INT";
    public static final int GL_TRANSPARENCY_QUARTERDEPTH_INT_VALUE = 135;

    public static final String GL_TRANSPARENCY_MAXDEPTH_INT = "GL_TRANSPARENCY_MAXDEPTH_INT";
    public static final int GL_TRANSPARENCY_MAXDEPTH_INT_VALUE = 540;

    public VoxelViewerProperties() {
        propertyMap.put(GL_VIEWER_HEIGHT_INT, new Integer(GL_VIEWER_HEIGHT_INT_VALUE));
        propertyMap.put(GL_VIEWER_WIDTH_INT, new Integer(GL_VIEWER_WIDTH_INT_VALUE));
        propertyMap.put(GL_TRANSPARENCY_QUARTERDEPTH_INT, new Integer(GL_TRANSPARENCY_QUARTERDEPTH_INT_VALUE));
        propertyMap.put(GL_TRANSPARENCY_MAXDEPTH_INT, new Integer(GL_TRANSPARENCY_MAXDEPTH_INT_VALUE));
    }

    public int getInteger(String key) throws Exception {
        return (Integer)propertyMap.get(key);
    }

    public float getFloat(String key) throws Exception {
        return (Float)propertyMap.get(key);
    }

    public String getString(String key) throws Exception {
        return (String)propertyMap.get(key);
    }

    public Long getLong(String key) throws Exception {
        return (Long)propertyMap.get(key);
    }

    public Double getDouble(String key) throws Exception {
        return (Double)propertyMap.get(key);
    }

    public void setInteger(String key, int value) {
        propertyMap.put(key, new Integer(value));
    }

    public void setFloat(String key, float value) {
        propertyMap.put(key, new Float(value));
    }

    public void setString(String key, String value) {
        propertyMap.put(key, value);
    }

    public void setLong(String key, long value) {
        propertyMap.put(key, new Long(value));
    }

    public void setDouble(String key, double value) {
        propertyMap.put(key, new Double(value));
    }

    public void setValue(String key, String value) throws Exception {
        if (key.endsWith("INT")) {
            setInteger(key, new Integer(value));
        } else if (key.endsWith("FLOAT")) {
            setFloat(key, new Float(value));
        } else if (key.endsWith("LONG")) {
            setLong(key, new Long(value));
        } else if (key.endsWith("DOUBLE")) {
            setDouble(key, new Double(value));
        } else {
            setString(key, value);
        }
    }

    public String getStringValue(String key) {
        Object o = propertyMap.get(key);
        if (o!=null) {
            return o.toString();
        } else {
            return null;
        }
    }

}
