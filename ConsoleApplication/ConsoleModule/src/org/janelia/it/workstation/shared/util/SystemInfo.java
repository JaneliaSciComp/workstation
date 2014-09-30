package org.janelia.it.workstation.shared.util;

import de.javasoft.io.FileUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.modules.InstalledFileLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * Adapted from IDEA code base.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SystemInfo {

    private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);
    
    private static final String NETBEANS_IDE_SETTING_NAME_PREFIX = "netbeans_";
    public static final String MEMORY_SETTING_PREFIX = "-J-Xmx";
    public static final String DEFAULT_OPTIONS_PROP = "default_options";
    public static final String ETC_SUBPATH = "etc";

    public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    public static final String OS_VERSION = System.getProperty("os.version").toLowerCase();
    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");
    public static final String ARCH_DATA_MODEL = System.getProperty("sun.arch.data.model");
    public static final String SUN_DESKTOP = System.getProperty("sun.desktop");

    public static final String DOWNLOADS_FINAL_PATH_DIR = "Downloads/";
    public static final String USERHOME_SYSPROP_NAME = "user.home";

    public static final boolean isWindows = OS_NAME.startsWith("windows");
    public static final boolean isWindowsNT = OS_NAME.startsWith("windows nt");
    public static final boolean isWindows2000 = OS_NAME.startsWith("windows 2000");
    public static final boolean isWindows2003 = OS_NAME.startsWith("windows 2003");
    public static final boolean isWindowsXP = OS_NAME.startsWith("windows xp");
    public static final boolean isWindowsVista = OS_NAME.startsWith("windows vista");
    public static final boolean isWindows7 = OS_NAME.startsWith("windows 7");
    public static final boolean isWindows9x = OS_NAME.startsWith("windows 9") || OS_NAME.startsWith("windows me");
    public static final boolean isOS2 = OS_NAME.startsWith("os/2") || OS_NAME.startsWith("os2");
    public static final boolean isMac = OS_NAME.startsWith("mac");
    public static final boolean isFreeBSD = OS_NAME.startsWith("freebsd");
    public static final boolean isLinux = OS_NAME.startsWith("linux");
    public static final boolean isUnix = !isWindows && !isOS2;

    public static final boolean isKDE = SUN_DESKTOP != null && SUN_DESKTOP.toLowerCase().contains("kde");
    public static final boolean isGnome = SUN_DESKTOP != null && SUN_DESKTOP.toLowerCase().contains("gnome");

    public static final boolean isMacSystemMenu = isMac && "true".equals(System.getProperty("apple.laf.useScreenMenuBar"));

    public static final boolean isFileSystemCaseSensitive = !isWindows && !isOS2 && !isMac;
    public static final boolean areSymLinksSupported = isUnix;

    public static final boolean is32Bit = ARCH_DATA_MODEL == null || ARCH_DATA_MODEL.equals("32");
    public static final boolean is64Bit = !is32Bit;
    public static final boolean isAMD64 = "amd64".equals(OS_ARCH);

    public static final boolean isMacIntel64 = isMac && "x86_64".equals(OS_ARCH);

    public static final String nativeFileManagerName = isMac ? "Finder" : isGnome ? "Nautilus" : isKDE ? "Konqueror" : "Explorer";

    /**
     * Whether IDEA is running under MacOS X version 10.4 or later.
     *
     * @since 5.0.2
     */
    public static final boolean isMacOSTiger = isTiger();


    /**
     * Whether IDEA is running under MacOS X on an Intel Machine
     *
     * @since 5.0.2
     */
    public static final boolean isIntelMac = isIntelMac();

    /**
     * Running under MacOS X version 10.5 or later;
     *
     * @since 7.0.2
     */
    public static final boolean isMacOSLeopard = isLeopard();

    /**
     * Running under MacOS X version 10.6 or later;
     *
     * @since 9.0
     */
    public static final boolean isMacOSSnowLeopard = isSnowLeopard();

    /**
     * Operating system is supposed to have middle mouse button click occupied by paste action.
     *
     * @since 6.0
     */
    public static boolean X11PasteEnabledSystem = isUnix && !isMac;

    private static boolean isTiger() {
        return isMac && !OS_VERSION.startsWith("10.0") && !OS_VERSION.startsWith("10.1") && !OS_VERSION.startsWith("10.2") && !OS_VERSION.startsWith("10.3");
    }

    private static boolean isIntelMac() {
        return isMac && "i386".equals(OS_ARCH);
    }

    private static boolean isLeopard() {
        return isMac && isTiger() && !OS_VERSION.startsWith("10.4");
    }

    private static boolean isSnowLeopard() {
        return isMac && isLeopard() && !OS_VERSION.startsWith("10.5");
    }
    
    public static void setDownloadsDir(String downloadsDir) {
        SessionMgr.getSessionMgr().setModelProperty(SessionMgr.DOWNLOADS_DIR, downloadsDir);
    }
    
    public static File getDownloadsDir() {
        String downloadsDir = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DOWNLOADS_DIR);
        File downloadsDirFile = null;
        if (downloadsDir==null) {
            if (SystemInfo.isMac) {
                downloadsDirFile = new File(System.getProperty(USERHOME_SYSPROP_NAME), DOWNLOADS_FINAL_PATH_DIR);
            }
            else if (SystemInfo.isLinux) {
                String userHome = System.getProperty(USERHOME_SYSPROP_NAME);
                String[] userHomePathParts = userHome.split( System.getProperty("file.separator" ) );
                String usernameFromPath = "";
                if ( userHomePathParts.length > 0 ) {
                    usernameFromPath = userHomePathParts[ userHomePathParts.length - 1 ];                
                }
                else {
                    usernameFromPath += new Random( new Date().getTime() ).nextInt();
                    log.warn("Using random temp path for downloads: {}.", usernameFromPath);
                }
                downloadsDirFile = new File("/tmp/"+ usernameFromPath +"/" + DOWNLOADS_FINAL_PATH_DIR);
            }
            else if (SystemInfo.isWindows) {
                downloadsDirFile = new File(System.getProperty(USERHOME_SYSPROP_NAME), DOWNLOADS_FINAL_PATH_DIR);
            }
            else {
                throw new IllegalStateException("Operation system not supported: "+SystemInfo.OS_NAME);
            }
            setDownloadsDir(downloadsDirFile.getAbsolutePath());
        }
        else {
            downloadsDirFile = new File(downloadsDir);
        }
        return downloadsDirFile;
    }

    private static com.sun.management.OperatingSystemMXBean getOSMXBean() {
        java.lang.management.OperatingSystemMXBean mxbean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunmxbean = (com.sun.management.OperatingSystemMXBean) mxbean;
        return sunmxbean;
    }
    
    public static Long getTotalSystemMemory() {
        try {
            return getOSMXBean().getTotalPhysicalMemorySize();
        }
        catch (Throwable e) {
            log.error("Could not retrieve total system memory",e);
            return null;
        }
    }
    
    public static Long getFreeSystemMemory() {
        try {
            return getOSMXBean().getFreePhysicalMemorySize();
        }
        catch (Throwable e) {
            log.error("Could not retrieve total system memory",e);
            return null;
        }
    }
    
    /**
     * Gets the -Xmx setting in current use.
     * @return gigs being requested at launch.
     */
    public static int getMemoryAllocation() throws IOException {
        File brandingConfig = getOrCreateBrandingConfigFile();
        String[] defaultOptions = getDefaultOptions( brandingConfig );
        String javaMemoryOption = getJavaMemOption( defaultOptions );
        if ( javaMemoryOption == null ) {
            return -1;
        }
        
        final int numberEndPt = javaMemoryOption.length() - 1;
        char rangeIndicator = javaMemoryOption.charAt( numberEndPt );
        final int numberStartPt = MEMORY_SETTING_PREFIX.length();
        int rtnVal = 0;
        if ( rangeIndicator != 'm' ) {
            // Default of 4Gb to return.
            rtnVal = 4;
        }
        else {
            // Stored as megabytes. Presented to user as gigabytes.
            rtnVal = Integer.parseInt( javaMemoryOption.substring( numberStartPt, numberEndPt ) ) / 1024;        
        }
        return rtnVal;
    }
    
    /**
     * Sets the ultimate -Xmx allocation setting.
     * @param memoryInGb how many gigs to use.
     */
    public static void setMemoryAllocation( int memoryInGb ) throws IOException {
        File brandingConfig = getOrCreateBrandingConfigFile();
        Properties props = loadNbConfig( brandingConfig );
        String value = (String)props.get( DEFAULT_OPTIONS_PROP );
        int optStart = value.indexOf(MEMORY_SETTING_PREFIX) + MEMORY_SETTING_PREFIX.length();
        int optEnd = value.indexOf( " ", optStart );
        
        String newDefaultOpts = value.substring( 0, optStart ) + memoryInGb * 1024 + "m" + value.substring( optEnd );
        reWriteProperty( brandingConfig, DEFAULT_OPTIONS_PROP, newDefaultOpts );
    }
    
    /**
     * Returns the user's copy of the branding configuration file,
     * but creates one by copying it from the main one, if it does
     * not yet exist.
     * 
     * @return 
     */
    private static File getOrCreateBrandingConfigFile() throws IOException {
        String appnameToken ="JaneliaWorkstation";  //todo This needs o be programmatically set and retrieved
        File userSettingsDir = new File( System.getProperty("netbeans.user") );
        if ( ! userSettingsDir.toString().contains("testuserdir") ) {
            userSettingsDir = new File( userSettingsDir.toString(), ETC_SUBPATH );
        }
        final String configFile = appnameToken + ".conf";
        File fqBrandingConfig = new File( userSettingsDir, configFile );
        if ( ! fqBrandingConfig.exists() ) {
            // Need to create-by-copy.
            synchronized ( SystemInfo.class ) {
                // Double-check.  While this thread waited, the thing
                // could have been copied already.
                if (!fqBrandingConfig.exists()) {
                    File sysWideConfig = InstalledFileLocator.getDefault().locate(configFile, null, false);
                    if ( sysWideConfig == null ) {
                        String nbHome = System.getProperty("netbeans.home");
                        File parent = new File( nbHome ).getParentFile();
                        File containingDir = new File( parent, ETC_SUBPATH );
                        sysWideConfig = new File( containingDir, "netbeans.conf" );
                    }
                    if (sysWideConfig != null  &&  sysWideConfig.canRead()) {
                        // Do the file copy.
                        log.info("Copying " + sysWideConfig + " to " + fqBrandingConfig);
                        if ( ! fqBrandingConfig.getParentFile().exists() ) {
                            fqBrandingConfig.getParentFile().mkdir();
                        }
                        // To copy, must change settings for app use, and away
                        // from the netbeans prefixes.
                        try ( BufferedReader infileReader = new BufferedReader( new FileReader( sysWideConfig ) ) ) {
                            try ( PrintWriter outfileWriter = new PrintWriter( new FileWriter( fqBrandingConfig ) ) ) {
                                String inline = null;
                                while ( null != ( inline = infileReader.readLine() ) ) {
                                    if ( inline.startsWith( NETBEANS_IDE_SETTING_NAME_PREFIX ) ) {
                                        inline = inline.substring( NETBEANS_IDE_SETTING_NAME_PREFIX.length() );                                        
                                    }
                                    outfileWriter.println( inline );
                                }
                            }
                        }
                        
                    }
                    else {
                        log.error("Failed to save config file changes.  Config file used was {}.", sysWideConfig);
                    }
                }
            }
            
        }
        
        return fqBrandingConfig;
    }
    
    private static String getJavaMemOption( String[] defaultOptions ) {
        String rtnVal = null;
        for ( String defaultOption: defaultOptions ) {
            if ( defaultOption.startsWith( MEMORY_SETTING_PREFIX ) ) {
                rtnVal = defaultOption;
                break;
            }
        }
        return rtnVal;
    }
    
    private static String[] getDefaultOptions( File infile ) throws IOException {
        Properties props = loadNbConfig( infile );
        String value = (String)props.get( DEFAULT_OPTIONS_PROP );
        if ( value != null ) {
            return value.split( " " );
        }
        else {
            return new String[0];
        }
    }
    
    private static Properties loadNbConfig( File infile ) throws IOException {
        Properties props = new Properties();
        props.load( new FileInputStream( infile ) );
        return props;
    }
    
    private synchronized static void reWriteProperty( File outFile, String propName, String propValue ) throws IOException {
        Properties oldProps = loadNbConfig( outFile );
        oldProps.setProperty( propName, propValue );
        try ( PrintWriter pw = new PrintWriter(new FileWriter(outFile) ) ) {
            for ( Object name: oldProps.keySet () ) {
                pw.println(name + "=" + oldProps.getProperty(name.toString()));
            }
        } catch ( IOException ioe ) {
            throw ioe;
        }
    }
    
}
