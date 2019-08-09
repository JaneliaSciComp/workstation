package org.janelia.workstation.core.api;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.events.prefs.LocalPreferenceChanged;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Preferences which are stored in the special JaneliaWorkstation directory in the user's home. These properties
 * are machine-specific, but they persist across reinstallations. 
 * 
 * Most of this code was inherited from the old SessionModel/GenericModel, but it has been adapted to work with the
 * new Workstation architecture.
 * 
 * @author Todd Safford
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LocalPreferenceMgr {

    private static final Logger log = LoggerFactory.getLogger(LocalPreferenceMgr.class);
    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 10;
    public static final int DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;

    // Singleton
    private static LocalPreferenceMgr instance;
    public static synchronized LocalPreferenceMgr getInstance() {
        if (instance==null) {
            instance = new LocalPreferenceMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }
    
    private final String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private final String prefsFile = prefsDir + ".JW_Settings";
    private final File settingsFile;

    private TreeMap<Object, Object> modelProperties;
    
    private LocalPreferenceMgr() {

        this.modelProperties = new TreeMap<>();
        this.settingsFile = new File(prefsFile);
        try {
            new File(prefsDir).mkdirs(); // Ensure that the settings directory exists
            boolean success = settingsFile.createNewFile();  // only creates if does not exist
            if (success) {
                log.info("Created a new settings file in {}",settingsFile.getAbsolutePath());
                writeSettings();
            }
            else {
                // Settings file already exists, everything is good.
                log.trace("Could not create settings file in {}",settingsFile.getAbsolutePath());
            }
        }
        catch (IOException ioEx) {
            log.warn("Caught exception while creating settings file. Will recover...", ioEx);
            if (!new File(prefsDir).mkdirs()) {
                log.error("Could not create prefs dir at " + prefsDir);
            }
            try {
                boolean success = settingsFile.createNewFile();  //only creates if does not exist
                if (success) {
                    log.info("Created a new settings file in {}",settingsFile.getAbsolutePath());
                    writeSettings();
                }
                else {
                    log.error("Could not create settings file in {}",settingsFile.getAbsolutePath());
                }
            }
            catch (IOException e) {
                log.error("Cannot create settings file at: " + settingsFile, e);
            }
        }

        readSettingsFile();
    }

    @SuppressWarnings("unchecked")
    private void readSettingsFile() {
        try {
            if (!settingsFile.canRead()) {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Settings file cannot be opened.  " + "Settings were not read and recovered.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                boolean success = settingsFile.renameTo(new File(prefsFile + ".old"));
                if (success) {
                    log.info("Moved the unreadable settings file to "+settingsFile.getAbsolutePath());
                }
            }
            ObjectInputStream istream = new ObjectInputStream(new FileInputStream(settingsFile));
            switch (istream.readInt()) {
                case 1: {
                    try {
                        this.modelProperties = (TreeMap<Object, Object>) istream.readObject();
                    }
                    catch (Exception ex) {
                        log.info("Error reading settings ",ex);
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Settings were not recovered into the session.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                        File oldFile = new File(prefsFile + ".old");
                        boolean deleteSuccess = oldFile.delete();
                        if (!deleteSuccess) {
                            log.error("Could not delete the old settings: "+oldFile.getAbsolutePath());
                        }
                        boolean renameSuccess = settingsFile.renameTo(new File(prefsFile + ".old"));
                        if (!renameSuccess) {
                            log.error("Could not rename new settings file to old.");
                        }
                    }
                    finally {
                        istream.close();
                    }
                    break;
                }
                default: {
                }
            }
        }
        catch (EOFException eof) {
            log.info("No settings file found",eof);
            // Do nothing, there are no preferences
        }
        catch (Exception ioEx) {
            log.warn("Error reading settings file", ioEx);
        }
    }
    
    private void writeSettings() {
        try {
            boolean success = settingsFile.delete();
            if (!success) {
                log.error("Unable to delete old settings file.");
            }
            ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(settingsFile));
            ostream.writeInt(1);  //stream format
            ostream.writeObject(modelProperties);
            ostream.flush();
            ostream.close();
            log.debug("Saving user settings to " + settingsFile.getAbsolutePath());
        }
        catch (IOException ioEx) {
            FrameworkAccess.handleException(ioEx);
        }
    }

    public String getApplicationOutputDirectory() {
        return prefsDir;
    }
    
    public Object setModelProperty(Object key, Object newValue) {
        if (modelProperties == null) throw new IllegalStateException("Local preferences have not yet been initialized");
        Object oldValue = modelProperties.put(key, newValue);
        if (!StringUtils.areEqual(oldValue, newValue)) {
            writeSettings();
            if (!key.toString().toLowerCase().contains("password")) {
                log.info("Saved local preference {} = {} (was {})",key,newValue,oldValue);
            }
            Events.getInstance().postOnEventBus(new LocalPreferenceChanged(key, oldValue, newValue));
        }
        return oldValue;
    }

    public Object getModelProperty(Object key) {
        if (modelProperties == null) throw new IllegalStateException("Local preferences have not yet been initialized");
        return modelProperties.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getModelPropertyAs(Object key, Class<T> propertyClass) {
        if (modelProperties == null) throw new IllegalStateException("Local preferences have not yet been initialized");
        return (T) modelProperties.get(key);
    }

    public Integer getFileCacheGigabyteCapacity() {
        Integer cacheCapacityInGB = getModelPropertyAs(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, Integer.class);
        return cacheCapacityInGB != null ? cacheCapacityInGB : DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY;
    }

    public final Integer setFileCacheGigabyteCapacity(Integer gigabyteCapacity) {
        Integer cacheCapacityInGB;
        if (gigabyteCapacity == null) {
            cacheCapacityInGB = DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY;
        } else if (gigabyteCapacity < MIN_FILE_CACHE_GIGABYTE_CAPACITY) {
            cacheCapacityInGB = MIN_FILE_CACHE_GIGABYTE_CAPACITY;
        } else if (gigabyteCapacity > MAX_FILE_CACHE_GIGABYTE_CAPACITY) {
            cacheCapacityInGB = MAX_FILE_CACHE_GIGABYTE_CAPACITY;
        } else {
            cacheCapacityInGB = gigabyteCapacity;
        }
        setModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, cacheCapacityInGB);
        return cacheCapacityInGB;
    }

    Boolean getFileCacheDisabled() {
        Boolean fileCacheDisabled = getModelPropertyAs(OptionConstants.FILE_CACHE_DISABLED_PROPERTY, Boolean.class);
        return fileCacheDisabled != null && fileCacheDisabled;
    }

    public boolean isCacheAvailable() {
        return !getFileCacheDisabled();
    }

    /**
     * Enables or disables the local file cache and
     * saves the setting as a session preference.
     *
     * @param isDisabled if true, cache will be disabled;
     * otherwise cache will be enabled.
     */
    public void setFileCacheDisabled(boolean isDisabled) {
        setModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY, isDisabled);
    }

    @Subscribe
    public void systemWillExit(ApplicationClosing closingEvent) {
        writeSettings();
    }
}
