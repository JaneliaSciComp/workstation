package org.janelia.it.workstation.browser.util;

import java.util.Properties;
import org.janelia.configutils.ConfigValueResolver;

/**
 * A subclass of Properties that allows recursive references for property
 * values. For example,
 *
 * <pre>
 * <code>
 * A=12345
 * B={A}567890
 * C={B} plus more
 * </code>
 * </pre>
 *
 * will result in <code>getProperty("C")</code> returning the value
 * "1234567890 plus more".
 */
public class ConfigProperties extends Properties {

    private final ConfigValueResolver configValueResolver = new ConfigValueResolver();

    /**
     * Creates an empty property list with no default values.
     */
    public ConfigProperties() {
        super();
    }

    /**
     * Creates an empty property list with the specified defaults.
     * 
     * @param defaults
     */
    public ConfigProperties(Properties defaults) {
        super(defaults);
    }

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in this property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns
     * <code>null</code> if the property is not found.
     *
     * @param key
     *            the property key.
     * @return the value in this property list with the specified key value.
     */
    @Override
    public String getProperty(String key) {
        String value = super.getProperty(key);
        return configValueResolver.resolve(value, (String k) -> super.getProperty(k));
    }

}
