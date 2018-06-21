package org.janelia.it.workstation.browser.gui.options;

import static org.janelia.it.workstation.browser.gui.options.OptionConstants.NUM_CONCURRENT_DOWNLOADS_DEFAULT;
import static org.janelia.it.workstation.browser.gui.options.OptionConstants.NUM_CONCURRENT_DOWNLOADS_PROPERTY;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Getters and setters for browser options.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BrowserOptions {

    private static final Logger log = LoggerFactory.getLogger(BrowserOptions.class);
    
    private static BrowserOptions instance;

    public static synchronized BrowserOptions getInstance() {
        if (null == instance) {
            instance = new BrowserOptions();
        }
        return instance;
    }

    private PropertyChangeSupport propSupport;
    
    private BrowserOptions() {
    }

    public int getNumConcurrentDownloads() {
        Integer value = (Integer) FrameworkImplProvider.getModelProperty(NUM_CONCURRENT_DOWNLOADS_PROPERTY);
        if (value==null) {
            value = NUM_CONCURRENT_DOWNLOADS_DEFAULT;
        }
        return value;
    }
    
    public void setNumConcurrentDownloads(int value) {
        Integer oldVal = (Integer) FrameworkImplProvider.getModelProperty(NUM_CONCURRENT_DOWNLOADS_PROPERTY);
        if (StringUtils.areEqual(value, oldVal)) {
            return;
        }

        FrameworkImplProvider.setModelProperty(NUM_CONCURRENT_DOWNLOADS_PROPERTY, value);
        log.info("Set num concurrent downloads = {}", value);

        if (null != propSupport)
            propSupport.firePropertyChange(NUM_CONCURRENT_DOWNLOADS_PROPERTY, (Object)oldVal, (Object)value);
    }
    

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (null == propSupport)
            propSupport = new PropertyChangeSupport(this);
        propSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (null == propSupport)
            return;
        propSupport.removePropertyChangeListener(l);
    }
    
    
}
