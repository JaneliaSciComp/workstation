package org.janelia.workstation.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


import org.apache.commons.lang3.StringUtils;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetBeans provides a way for a user to keep a customized properties file which holds certain 
 * startup configuration options. By default, NetBeans will use the ${APPNAME}.conf file which is 
 * provided with the application distribution. The Workstation's version of this file is found 
 * at ConsoleApplication/harness/etc/app.conf
 * 
 * Within the Workstation, we provide a way for the user to change their max memory setting, 
 * which must be done in a customized ${APPNAME}.conf within the netbeans user directory.
 * 
 * This class is responsible for the following:
 * 
 * 1) When a user sets a custom setting override, this class will update the customized ${APPNAME}.conf
 *    file. If there is no customized file, then it copies over the system-level settings file first.
 * 2) Reads the custom settings whenever requested by the application or the user.
 * 3) Keeps the settings in sync when the system setting default are updated. During development, we might 
 *    change or add a system default. These types of changes must make it to each users' custom settings file, 
 *    without trashing their customized settings.
 *
 * This builds on earlier work by Les Foster, which lived in the SystemInfo class, and took care of 1 and 2, 
 * but not 3. This class attempts to implement 3, as well as abstract all access to the various levels of 
 * app configuration properties files. 
 * 
 * NOTE: This doesn't work as expected in development, because both system and custom files point to the same 
 * location. This only works if the application has been installed via one of the installers created by the build. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BrandingConfig {

    private static final Logger log = LoggerFactory.getLogger(BrandingConfig.class);

    // Singleton
    private static BrandingConfig instance;
    public static synchronized BrandingConfig getBrandingConfig() {
        if (instance==null) {
            instance = new BrandingConfig();
        }
        return instance;
    }

    public static final String appnameToken = "janeliaws";  // TODO: Get this from NetBeans framework somehow
    
    private static final String NETBEANS_IDE_SETTING_NAME_PREFIX = "netbeans_";
    private static final String MEMORY_SETTING_PREFIX = "-J-Xmx";
    private static final String CHECK_UPDATES_PREFIX = "-J-Dplugin.manager.check.updates=";
    private static final String DEFAULT_OPTIONS_PROP = "default_options";
    private static final String ETC_SUBPATH = "etc";

    private final Map<String,String> systemSettings = new LinkedHashMap<>();
    private final Map<String,String> brandingSettings = new LinkedHashMap<>();
    
    private final boolean devMode;
    private File fqBrandingConfig; 
    private Integer maxMemoryMB = null;
    private Boolean checkUpdates = null;
    private boolean needsRestart = false;
    private static boolean brandingValidationException = false;

    private BrandingConfig() {
        this.devMode = Places.getUserDirectory().toString().contains("target/userdir");
        if (devMode) {
            // TODO: It would be nice to get this working in development, but NetBeans does things very differently in dev mode, 
            // and it's not clear if it's even possible to have a branding config. Maybe someday we'll investigate this further. 
            log.info("Branding config is disabled in development. Memory preference will have no effect. "
                    + "To change the max memory in development, temporarily edit the nbproject/project.properties file directly.");
        }
        else {
            loadSystemConfig();
            loadBrandingConfig();
        }
    }

    public static boolean isBrandingValidationException() {
        return brandingValidationException;
    }

    /**
     * This loads the default configuration file.
     */
    private final void loadSystemConfig() {
        try {
            // Find the current bona-fide production config
            final String configFile = "config/janeliaws.conf";
            File sysWideConfig = InstalledFileLocator.getDefault().locate(configFile, "org.janelia.workstation", false);
            log.debug("Trying system config at {}", sysWideConfig);
            
            if (sysWideConfig != null && sysWideConfig.canRead()) {
                upgradeExecutable(sysWideConfig);
                loadProperties(sysWideConfig, systemSettings);
                log.info("Loaded {} properties from {}", systemSettings.size(), sysWideConfig);
            }
            else {
                log.error("Error locating system configuration in resources directory: "+configFile);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Error loading system configuration", e);
        }
    }
    
    /**
     * Attempt to upgrade Mac "executable" to use 1.8 when available.
     * 
     * @param sysWideConfig
     */
    private void upgradeExecutable(File sysWideConfig) {
//        try {
//            if (SystemInfo.isMac && SystemInfo.getJavaInfo().contains("1.7")) { 
//                
//                File resourceDir = sysWideConfig.getParentFile().getParentFile().getParentFile();
//                File executable = new File(resourceDir, "bin/"+resourceDir.getName());
//                
//                if (!executable.exists()) {
//                    log.error("Mac executable cannot be found: {}", executable);
//                    return;
//                }
//                
//                if (!executable.canWrite()) {
//                    log.error("Mac executable cannot be written: {}", executable);
//                    return;
//                }
//                
//                log.info("Attempting to upgrade Mac executable: {}", executable);
//                
//                String s1 = "/usr/libexec/java_home -v 1.7";
//                String s2 = "/usr/libexec/java_home -v 1.8";
//                if (Utils.replaceInFile(executable.getAbsolutePath(), s1, s2)) {
//                    log.info("Successfully upgraded Mac executable: {}", executable);
//                }
//            }
//        }
//        catch (Exception e) {
//            log.error("Error attempting to fix Mac executable given sysConfig="+sysWideConfig, e);
//        }
    }
    
    /**
     * This loads the user-customized configuration file.
     */
    private final void loadBrandingConfig() {
        
        try {
            File userSettingsDir = new File(Places.getUserDirectory(), ETC_SUBPATH);
            if (!userSettingsDir.exists()) {
                userSettingsDir.mkdirs();
            }
            final String configFile = appnameToken + ".conf";
            this.fqBrandingConfig = new File(userSettingsDir, configFile);
            log.debug("Trying branding config at {}", fqBrandingConfig);
    
            if (fqBrandingConfig.exists()) {
                loadProperties(fqBrandingConfig, brandingSettings);
                log.info("Loaded {} properties from {}", brandingSettings.size(), fqBrandingConfig);
                loadBrandingMemorySetting();
                loadBrandingCheckUpdatesSetting();
            }
        }
        catch (IOException e) {
            log.error("Error loading branding config",e);
        }
    }
    
    private final void loadBrandingMemorySetting() {
        String javaMemoryOption = getJavaMemOption();
        if (javaMemoryOption==null) return;
        log.info("Found existing memory option: "+javaMemoryOption);
        final int numberEndPt = javaMemoryOption.length() - 1;
        char rangeIndicator = javaMemoryOption.charAt(numberEndPt);
        final int numberStartPt = MEMORY_SETTING_PREFIX.length();
        if (rangeIndicator != 'm') {
            // Default of 8 GB 
            this.maxMemoryMB = 8 * 1024;
        }
        else {
            this.maxMemoryMB = Integer.parseInt(javaMemoryOption.substring(numberStartPt, numberEndPt));
        }
        log.info("Loaded existing branding memory setting: "+maxMemoryMB);
    }

    private final void loadBrandingCheckUpdatesSetting() {
//        String checkUpdatesOption = getCheckUpdatesOption();
//        if (checkUpdatesOption==null) return;
//        final int numberStartPt = CHECK_UPDATES_PREFIX.length();
//        String value = checkUpdatesOption.substring(numberStartPt);
//        this.checkUpdates = Boolean.parseBoolean(value);
//        log.info("Loaded existing check updates setting: "+value);
        this.checkUpdates = true;
        log.info("Forced check updates setting: "+checkUpdates);
    }
    
    public boolean isNeedsRestart() {
        return needsRestart;
    }

    private final String getJavaMemOption() {
        String defaultOptions = brandingSettings.get(DEFAULT_OPTIONS_PROP);
        String[] defaultOptionsArr = defaultOptions == null ? new String[0] : defaultOptions.split(" ");
        for (String defaultOption : defaultOptionsArr) {
            if (defaultOption.startsWith(MEMORY_SETTING_PREFIX)) {
                return defaultOption;
            }
        }
        return null;
    }

    private final String getCheckUpdatesOption() {
        String defaultOptions = brandingSettings.get(DEFAULT_OPTIONS_PROP);
        String[] defaultOptionsArr = defaultOptions == null ? new String[0] : defaultOptions.split(" ");
        for (String defaultOption : defaultOptionsArr) {
            if (defaultOption.startsWith(CHECK_UPDATES_PREFIX)) {
                return defaultOption;
            }
        }
        return null;
    }
    
    /**
     * This method should be called when the application starts in order to ensure that the branding configuration is
     * valid and synchronized. 
     */
    public void validateBrandingConfig() {

        if (devMode) return;
        
        log.info("Validating branding configuration...");
        
        try {
            boolean dirty = false;
            
            for(String systemKey : systemSettings.keySet()) {
                
                String brandingKey = systemKey;
                if (brandingKey.startsWith(NETBEANS_IDE_SETTING_NAME_PREFIX)) {
                    brandingKey = brandingKey.substring(NETBEANS_IDE_SETTING_NAME_PREFIX.length());
                }
                
                String systemValue = systemSettings.get(systemKey);
                String brandingValue = brandingSettings.get(brandingKey);
                
                if (DEFAULT_OPTIONS_PROP.equals(systemKey)) {
                    // Default options are treated differently than most. We take the system setting for everything except the 
                    // max memory and check updates, which can be customized by the user. 
                    // TODO: In the future, it would be nice to support customization of any property, but it requires rather 
                    // complicated command-line option parsing.
                    if (syncDefaultOptions(maxMemoryMB, checkUpdates)) {
                        dirty = true;
                    }
                }
                else {
                    if (brandingValue==null) {
                        // For most options, we just ensure they're present.
                        log.info("Updating branding config for {}={} to {}", brandingKey, brandingValue, systemValue);
                        brandingSettings.put(brandingKey, systemValue);
                        dirty = true;
                    }   
                    else if (!StringUtilsExtra.areEqual(brandingValue, systemValue)) {
                        // We allow customized options. 
                        // TODO: Perhaps it would make sense to track if the default changes?
                        log.info("Branding config has customized value for {}={} (default: {})", brandingKey, brandingValue, systemValue);
                    }
                }   
            }
            
            if (dirty) {
                saveBrandingConfig();
            }

            log.info("Branding configuration validated.");
        }
        catch (Exception e) {
            log.error("Error validating branding config",e);
            // Save this error state so that it can be shown to the user later, once the MainWindow is visible.
            brandingValidationException = true;
        }
    }

    private boolean syncDefaultOptions(Integer maxMemoryMB, Boolean checkUpdates) {

        String systemValue = systemSettings.get(DEFAULT_OPTIONS_PROP);
        String brandingValue = brandingSettings.get(DEFAULT_OPTIONS_PROP);
        String customDefaultOpts = systemValue;
        log.info("Original systemValue="+systemValue);
        
        // What should the default options be?
        if (maxMemoryMB != null) {
            int optStart = customDefaultOpts.indexOf(MEMORY_SETTING_PREFIX) + MEMORY_SETTING_PREFIX.length();
            int optEnd = customDefaultOpts.indexOf(" ", optStart);
            if (optEnd<0) {
                optEnd = customDefaultOpts.indexOf("\"", optStart);
                if (optEnd<0) {
                    optEnd = customDefaultOpts.length();
                }
            }
            customDefaultOpts = customDefaultOpts.substring(0, optStart) + maxMemoryMB + "m" + customDefaultOpts.substring(optEnd);
            log.info("After applying maxMemoryMB, customDefaultOpts="+customDefaultOpts);
        }
        
        if (checkUpdates != null) {
            int optStart = customDefaultOpts.indexOf(CHECK_UPDATES_PREFIX) + CHECK_UPDATES_PREFIX.length();
            int optEnd = customDefaultOpts.indexOf(" ", optStart);
            if (optEnd<0) {
                optEnd = customDefaultOpts.indexOf("\"", optStart);
                if (optEnd<0) {
                    optEnd = customDefaultOpts.length();
                }
            }
            customDefaultOpts = customDefaultOpts.substring(0, optStart) + checkUpdates + customDefaultOpts.substring(optEnd);
            log.info("After applying checkUpdates, customDefaultOpts="+customDefaultOpts);
        }
        
        if (!StringUtilsExtra.areEqual(brandingValue, customDefaultOpts)) {
            log.info("Updating branding config for {}\nfrom {}\n  to {}", DEFAULT_OPTIONS_PROP, brandingValue, customDefaultOpts);
            brandingSettings.put(DEFAULT_OPTIONS_PROP, customDefaultOpts);
            // If the branding value has changed, we'll need to restart to pick it up.
            if (brandingValue!=null) {
                needsRestart = true;
            }
            return true;
        }
        else {
            log.info("Branding config already has correct default options");
            return false;
        }
    }

    public Integer getMemoryAllocationGB() {
        // Stored as megabytes. Presented to user as gigabytes.
        if (maxMemoryMB == null) return null;
        log.debug("Got memory allocation = {} MB", maxMemoryMB);
        return maxMemoryMB / 1024;
    }
    
    public void setMemoryAllocationGB(Integer maxMemoryGB) throws IOException {
        Integer maxMemoryMB = maxMemoryGB == null ? null : maxMemoryGB * 1024;
        if (fqBrandingConfig!=null) {
            if (syncDefaultOptions(maxMemoryMB, checkUpdates)) {
                saveBrandingConfig();
            }
        }
        this.maxMemoryMB = maxMemoryMB;
        log.info("Set memory allocation = {} MB", maxMemoryMB);
    }

    //-J-Dplugin.manager.check.updates=false
    public boolean getCheckUpdates() {
        log.debug("Got check updates = {}", checkUpdates);
        return checkUpdates;
    }
    
    public void setCheckUpdates(boolean checkUpdates) throws IOException {
        if (fqBrandingConfig!=null) {
            if (syncDefaultOptions(maxMemoryMB, checkUpdates)) {
                saveBrandingConfig();
            }
        }
        this.checkUpdates = checkUpdates;
        log.info("Set check updates = {}", checkUpdates);
    }
    
    private void saveBrandingConfig() throws IOException {

        if (!fqBrandingConfig.getParentFile().exists()) {
            fqBrandingConfig.getParentFile().mkdir();
        }

        try (PrintWriter outfileWriter = new PrintWriter(new FileWriter(fqBrandingConfig))) {
            for(String brandingKey : brandingSettings.keySet()) {
                String brandingValue = brandingSettings.get(brandingKey);
                outfileWriter.print(brandingKey);
                outfileWriter.print("=");
                outfileWriter.println(brandingValue);
            }
        }
        
        log.info("Wrote updated branding config to {}", fqBrandingConfig);
    }
    
    private void loadProperties(File infile, Map<String,String> map) throws IOException {
        Properties props = new Properties();
        if (infile.exists()) {
            props.load(new FileInputStream(infile));
        }
        for (final String name: props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
    }
}
