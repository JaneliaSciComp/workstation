package org.janelia.it.workstation.gui.browser.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.prefs.LocalPreferenceChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class LocalPreferences {

    private static final Logger log = LoggerFactory.getLogger(LocalPreferences.class);
    
    private File settingsFile;
    private String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private String prefsFile = prefsDir + ".JW_Settings";

    protected TreeMap<Object, Object> modelProperties;
    
    public LocalPreferences() {

        this.modelProperties = new TreeMap<>();
        this.settingsFile = new File(prefsFile);
        try {
            boolean success = settingsFile.createNewFile();  //only creates if does not exist
            if (success) {
                log.info("Created a new settings file in "+settingsFile.getAbsolutePath());
            }
        }
        catch (IOException ioEx) {
            if (!new File(prefsDir).mkdirs()) {
                log.error("Could not create prefs dir at " + prefsDir);
            }
            try {
                boolean success = settingsFile.createNewFile();  //only creates if does not exist
                if (success) {
                    log.info("Created a new settings file in "+settingsFile.getAbsolutePath());
                }
            }
            catch (IOException e) {
                log.error("Cannot create settings file at: " + settingsFile, e);
            }
        }

        readSettingsFile();
    }

    private void readSettingsFile() {
        try {
            if (!settingsFile.canRead()) {
                JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "Settings file cannot be opened.  " + "Settings were not read and recovered.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                boolean success = settingsFile.renameTo(new File(prefsFile + ".old"));
                if (success) {
                    log.info("Moved the unreadable settings file to "+settingsFile.getAbsolutePath());
                }
            }
            ObjectInputStream istream = new ObjectInputStream(new FileInputStream(settingsFile));
            switch (istream.readInt()) {
                case 1: {
                    try {
                        this.modelProperties = (TreeMap) istream.readObject();
                    }
                    catch (Exception ex) {
                        log.info("Error reading settings ",ex);
                        JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "Settings were not recovered into the session.", "ERROR!", JOptionPane.ERROR_MESSAGE);
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
            log.info("No settings file",eof);
            // Do nothing, there are no preferences
        }
        catch (Exception ioEx) {
            log.info("Error reading settings file", ioEx);
        } //new settingsFile
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
            log.info("Saving user settings to " + settingsFile.getAbsolutePath());
        }
        catch (IOException ioEx) {
            ConsoleApp.handleException(ioEx);
        }
    }

    public String getApplicationOutputDirectory() {
        return prefsDir;
    }
    
    public Object setModelProperty(Object key, Object newValue) {
        if (modelProperties == null) modelProperties = new TreeMap<>();
        Object oldValue = modelProperties.put(key, newValue);
        writeSettings();
        Events.getInstance().postOnEventBus(new LocalPreferenceChanged(key, oldValue, newValue));
        return oldValue;
    }

    public Object getModelProperty(Object key) {
        if (modelProperties == null) return null;
        return modelProperties.get(key);
    }
}
