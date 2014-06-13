/**
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/17/11
 * Time: 12:47 PM
 */
package org.janelia.it.workstation.shared.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic properties loaded from console.properties.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConsoleProperties extends Properties {

	private static final Logger log = LoggerFactory.getLogger(ConsoleProperties.class);
	
    private static ConsoleProperties me;
    private static Set<String> fileNames = new HashSet<String>();

    static {
        fileNames.add("console.properties");
    }

    private static ConsoleProperties load(boolean reload) {
        if (me == null || reload) {
            me = new ConsoleProperties();
            // Needed to prevent getters in other threads from accessing properties while
            // properties are being loaded.  This is especially an issue during
            // property file reloads
            synchronized (me) {
                for (String fileName : fileNames) {
                    load(fileName, me);
                }
            }
        }
        return me;
    }

    public static ConsoleProperties getInstance() {
        load(false);
        return me;
    }

    /**
     * Used by Spring to inject the property file name at startup
     *
     * @param filename - property file name to inject into Spring
     */
    public static void setFileName(String filename) {
        fileNames.clear();
        fileNames.add(filename);
    }

    /**
     * Can be used to reload a property file at runtime
     */
    public static void reload() {
        load(true);
    }

    /**
     * Loads properties if they've haven't been loaded already
     */
    public static void load() {
        load(false);
    }

    /**
     * Used by the getXXX methods to retrieve the property value
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the value of the property if it exists or defaultValue if it doesn't
     */
    private static String getPropertyValue(String name, String defaultValue) {
        return load(false).getProperty(name, defaultValue);
    }

    /**
     * This method returns a property value if it exists and throws an exception
     * if it does not
     *
     * @param name - property key desired
     * @return the property value
     */
    private static String getNotNullPropValue(String name) {
        return ensureNotNull(name, getPropertyValue(name, null));
    }

    /**
     * Same as getNotNullPropValue(name) method
     *
     * @param name - property key desired
     * @return the property value
     */
    public static String getString(String name) {
        return getNotNullPropValue(name);
    }

    /**
     * Same as getPropertyValue(name) method
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value
     */
    public static String getString(String name, String defaultValue) {
        return getPropertyValue(name, defaultValue);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then false is returned
     *
     * @param name - property key desired
     * @return the property value as a boolean.
     */
    public static boolean getBoolean(String name) {
        return getBooleanValue(getNotNullPropValue(name), false);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then defaultValue is
     * returned
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as a boolean.
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        return getBooleanValue(getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property does not exist or if it's not an int, then an exception
     * is thrown.
     *
     * @param name - property key desired
     * @return the property value as an int.
     */
    public static int getInt(String name) {
        return getIntValue(name, getNotNullPropValue(name), -1);
    }

    /**
     * This method returns the property value as a primitive int value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an int, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an int.
     */
    public static int getInt(String name, int defaultValue) {
        return getIntValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist or if it's not an long,
     * then an exception is thrown.
     *
     * @param name - property key desired
     * @return the property value as an long.
     */
    public static long getLong(String name) {
        return getLongValue(name, getNotNullPropValue(name), -1);
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a long, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an long.
     */
    public static long getLong(String name, long defaultValue) {
        return getLongValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist or if it's not an float,
     * then an exception is thrown.
     *
     * @param name - property key desired
     * @return the property value as an float.
     */
    public static float getFloat(String name) {
        return getFloatValue(name, getNotNullPropValue(name), -1.0f);
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a float, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an float.
     */
    public static float getFloat(String name, float defaultValue) {
        return getFloatValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist or if it's not an double,
     * then an exception is thrown.
     *
     * @param name - property key desired
     * @return the property value as an double.
     */
    public static double getDouble(String name) {
        return getDoubleValue(name, getNotNullPropValue(name), -1.0);
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an double, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an double.
     */
    public static double getDouble(String name, float defaultValue) {
        return getDoubleValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method is called by subclasses of to return file system path constructed
     * of 2 properties -
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an double.
     *
     * @param pathProperty     - name of path property
     * @param filenameProperty - name of filename property
     * @return constructed property value as String
     */
    public static String getFilePath(String pathProperty, String filenameProperty) {
        String path = getString(pathProperty);
        String filename = getString(filenameProperty);
        // concatinate
        if (path.endsWith(File.separator)) return path + filename;
        else return path + File.separator + filename;
    }


    /**
     * This method is responsible for loading the property file represented by propertiesFileName
     * into properties passed in.
     *
     * @param propertiesFileName - property file to load
     * @param properties         - ConsoleProperties object to load props into
     * @return the BaseProperties instance loaded with properties
     */
    protected static ConsoleProperties load(String propertiesFileName, ConsoleProperties properties) {
        InputStream in;
        ClassLoader cl = ConsoleProperties.class.getClassLoader();
        if (cl == null) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(new File(propertiesFileName));
            }
            catch (Exception ex) {
                throw new RuntimeException("ClassLoader returned null and could not find file=" + propertiesFileName);
            }
            in = fis;
        }
        else {
            in = cl.getResourceAsStream(propertiesFileName);
        }
        if (in == null) {
             log.error("Could not read properties file: " + propertiesFileName);
        }
        try {
            properties.load(in);
            if (null != in) {
                in.close();
            }
        }
        catch (Exception e) {
        	log.error("Could not read properties file: " + propertiesFileName);
        }
        return properties;
    }


    /**
     * This method is called by subclasses of BaseProperties to return defaultValue
     * if property value is null or the boolean representation of value
     *
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return true if value is "true", false otherwise
     */
    protected static boolean getBooleanValue(String value, boolean defaultValue) {
        return (value == null ? defaultValue : Boolean.valueOf(value));
    }

    /**
     * This method is called by subclasses of BaseProperties to return the int
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an int.
     *
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as int
     */
    protected static int getIntValue(String name, String value, int defaultValue) {
        try {
            return (value == null ? defaultValue : Integer.parseInt(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an int");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to return the long
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an long.
     *
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as long
     */
    protected static long getLongValue(String name, String value, long defaultValue) {
        try {
            return (value == null ? defaultValue : Long.parseLong(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an long");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to return the float
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an float.
     *
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as float
     */
    protected static float getFloatValue(String name, String value, float defaultValue) {
        try {
            return (value == null ? defaultValue : Float.parseFloat(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an float");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to return the double
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an double.
     *
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as double
     */
    protected static double getDoubleValue(String name, String value, double defaultValue) {
        try {
            return (value == null ? defaultValue : Double.parseDouble(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an float");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to ensure that a property
     * exists in the property file.  If it doesn't, an exception is thrown.
     *
     * @param name  - property key desired
     * @param value - value string to test null for
     * @return value - returns the value if not null or throws an exception
     */
    protected static String ensureNotNull(String name, String value) {
        if (value != null) {
            return value;
        }
        else {
            throw new IllegalArgumentException(name + " property not found");
        }
    }
}
