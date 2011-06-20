/**
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/17/11
 * Time: 12:47 PM
 */
package org.janelia.it.FlyWorkstation.gui.util;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * Dynamic properties loaded from console.properties.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConsoleProperties {

    private static Logger log = Logger.getLogger(ConsoleProperties.class);

    private static final String propertiesFilename = "console.properties";

    private static Properties properties;

    static {
    	try {
	        InputStream is = null;
	        properties = new Properties();

	        try {
	            log.info("Loading properties from "+propertiesFilename);

	            is = Thread.currentThread().getContextClassLoader().
	            		getResourceAsStream(propertiesFilename);
	            properties.load(is);
	        }
	        finally {
	            if (is != null) is.close();
	        }
    	}
    	catch (Exception e) {
    		log.error("Error loading properties from "+propertiesFilename,e);
    	}
    }

    public static String getProperty(String name) {
        return properties.getProperty(name);
    }

}
