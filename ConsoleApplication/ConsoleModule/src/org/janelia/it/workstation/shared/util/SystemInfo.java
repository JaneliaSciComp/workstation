package org.janelia.it.workstation.shared.util;

import com.sun.management.OperatingSystemMXBean;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Date;
import java.util.Random;

/**
 * Adapted from IDEA code base.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SystemInfo {

    private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);
    
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
    
    private static OperatingSystemMXBean getOSMXBean() {
        OperatingSystemMXBean mxbean = ManagementFactory.getOperatingSystemMXBean();
        OperatingSystemMXBean sunmxbean = (OperatingSystemMXBean) mxbean;
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
}
