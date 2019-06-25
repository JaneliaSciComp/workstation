package org.janelia.workstation.core.util;

import org.openide.modules.InstalledFileLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Adapted from IDEA code base.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SystemInfo {

    private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);

    public static final String OS_NAME = System.getProperty("os.name");
    public static final String OS_VERSION = System.getProperty("os.version");
    public static final String OS_NAME_LC = OS_NAME.toLowerCase();
    public static final String OS_VERSION_LC = OS_VERSION.toLowerCase();
    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final String JAVA_NAME = System.getProperty("java.name", "Java");
    public static final String JAVA_VERSION = System.getProperty("java.version", "unknown");
    public static final String JAVA_RUNTIME_NAME = System.getProperty("java.runtime.name", "Java");
    public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version", "unknown");
    public static final String ARCH_DATA_MODEL = System.getProperty("sun.arch.data.model");
    public static final String SUN_DESKTOP = System.getProperty("sun.desktop");

    public static final String DOWNLOADS_DIR = "Downloads";
    public static final String WORKSTATION_FILES_DIR = "Workstation";
    public static final String USERHOME_SYSPROP_NAME = "user.home";

    public static final boolean isWindows = OS_NAME_LC.startsWith("windows");
    public static final boolean isWindowsNT = OS_NAME_LC.startsWith("windows nt");
    public static final boolean isWindows2000 = OS_NAME_LC.startsWith("windows 2000");
    public static final boolean isWindows2003 = OS_NAME_LC.startsWith("windows 2003");
    public static final boolean isWindowsXP = OS_NAME_LC.startsWith("windows xp");
    public static final boolean isWindowsVista = OS_NAME_LC.startsWith("windows vista");
    public static final boolean isWindows7 = OS_NAME_LC.startsWith("windows 7");
    public static final boolean isWindows9x = OS_NAME_LC.startsWith("windows 9") || OS_NAME_LC.startsWith("windows me");
    public static final boolean isOS2 = OS_NAME_LC.startsWith("os/2") || OS_NAME_LC.startsWith("os2");
    public static final boolean isMac = OS_NAME_LC.startsWith("mac");
    public static final boolean isFreeBSD = OS_NAME_LC.startsWith("freebsd");
    public static final boolean isLinux = OS_NAME_LC.startsWith("linux");
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

    public static final String optionsMenuName = isMac ? "Preferences" : "Tools->Options";

    public static final String appName = ConsoleProperties.getString("client.Title");
    public static final String appVersion = ConsoleProperties.getString("client.versionNumber");

    public static final boolean isDev = "DEV".equals(appVersion);
    public static final boolean isTest = "TEST".equals(appVersion);
    
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
        return isMac && !OS_VERSION_LC.startsWith("10.0") && !OS_VERSION_LC.startsWith("10.1") && !OS_VERSION_LC.startsWith("10.2") && !OS_VERSION_LC.startsWith("10.3");
    }

    private static boolean isIntelMac() {
        return isMac && "i386".equals(OS_ARCH);
    }

    private static boolean isLeopard() {
        return isMac && isTiger() && !OS_VERSION_LC.startsWith("10.4");
    }

    private static boolean isSnowLeopard() {
        return isMac && isLeopard() && !OS_VERSION_LC.startsWith("10.5");
    }

    public static String getInstallDir() {

        final String configFile = "config/app.conf";
        File sysWideConfig = InstalledFileLocator.getDefault().locate(configFile, "org.janelia.workstation", false);
        if (sysWideConfig==null) {
            log.warn("Could not find app.conf. Install dir is unknown.");
            return null;
        }
        String cp = sysWideConfig.getAbsolutePath();
        log.debug("Found system config at {}", sysWideConfig);
        
        if (SystemInfo.isMac) {
            return cp.split("\\.app")[0]+".app";
        }
        
        // Windows and Linux
        return cp.substring(0,cp.indexOf("JaneliaWorkstation")+"JaneliaWorkstation".length());
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

    public static String getOSInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(OS_NAME).append(" ").append(OS_VERSION).append(" (").append(OS_ARCH).append(")");
        return sb.toString();
    }

    public static String getJavaInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(JAVA_NAME).append(" ").append(JAVA_VERSION);
        return sb.toString();
    }

    public static String getRuntimeJavaInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(JAVA_RUNTIME_NAME).append(" ").append(JAVA_RUNTIME_VERSION);
        return sb.toString();
    }
}
