package org.janelia.it.workstation.browser.api;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationClosing;
import org.janelia.it.workstation.browser.events.prefs.LocalPreferenceChanged;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
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

    protected TreeMap<Object, Object> modelProperties;
    
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
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "Settings file cannot be opened.  " + "Settings were not read and recovered.", "ERROR!", JOptionPane.ERROR_MESSAGE);
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
                        JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "Settings were not recovered into the session.", "ERROR!", JOptionPane.ERROR_MESSAGE);
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
    
    public void writeSettings() {
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
            FrameworkImplProvider.handleException(ioEx);
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
    
    @Subscribe
    public void systemWillExit(ApplicationClosing closingEvent) {
        writeSettings();
    }
}
