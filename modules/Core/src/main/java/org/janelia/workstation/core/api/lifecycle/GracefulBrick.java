package org.janelia.workstation.core.api.lifecycle;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.MissingResourceException;

import javax.swing.JOptionPane;

import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.workstation.core.api.ConnectionMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.api.http.HttpClientManager;
import org.openide.LifecycleManager;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the ability to "brick" a Workstation installation, either through a local configuration option, 
 * or globally, through the update center. This is necessary because we don't support backwards compatibility for 
 * older clients, so every user should always be using the latest client version.   
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GracefulBrick {

    private static final Logger log = LoggerFactory.getLogger(GracefulBrick.class);
    
    public void brickAndUninstall() throws Exception {

        log.info("Places.getUserDirectory: "+Places.getUserDirectory());
        log.info("user.dir: "+System.getProperty("user.dir"));
        
        final String configFile = "config/app.conf";
        File sysWideConfig = InstalledFileLocator.getDefault().locate(configFile, "org.janelia.workstation", false);
        log.info("Found system config at {}", sysWideConfig);
        
        if (!isBricked()) {
            return;
        }
        
        log.info("THIS CLIENT IS BRICKED. PROCEEDING TO FORCED UNINSTALL.");

        String helpPage;
        File uninstaller = null;
        if (SystemInfo.isMac) {
            helpPage = "manual/macosx_upgrade.html";
            if (sysWideConfig!=null) {
                String installDir = sysWideConfig.getAbsolutePath().split("\\.app")[0]+".app";
                uninstaller = new File(installDir, "uninstall.command");
            }
        }
        else if (SystemInfo.isWindows) {
            helpPage = "manual/windows_upgrade.html";
            if (sysWideConfig!=null) {
                String cp = sysWideConfig.getAbsolutePath();
                String installDir = cp.substring(0, cp.indexOf("JaneliaWorkstation") + "JaneliaWorkstation".length());
                uninstaller = new File(installDir, "uninstall.exe");
            }
        }
        else if (SystemInfo.isLinux) {
            helpPage = "manual/linux_upgrade.html";
            if (sysWideConfig!=null) {
                String cp = sysWideConfig.getAbsolutePath();
                String installDir = cp.substring(0, cp.indexOf("JaneliaWorkstation") + "JaneliaWorkstation".length());
                uninstaller = new File(installDir, "uninstall.sh");
            }
        }
        else {
            log.error("Unknown system: "+SystemInfo.OS_NAME);
            helpPage = "upgrade";
        }


        String apiGateway = ConnectionMgr.getConnectionMgr().getConnectionString();

        final String helpUrl = String.format("%s/%s", apiGateway, helpPage);
        final String simpleHelpUrl = String.format("%s/upgrade", apiGateway);
        
        String html = "<html><body width='420'>" +
        "<p>This version of the Workstation is no longer supported and must be manually upgraded to the latest release.</p>" +
        "<br>" +
        "<p>When you press the Continue button below, the Workstation will exit and you will be taken to the following web page, which describes how to install the new version: " + simpleHelpUrl + 
        "</p>" +
        "</body></html>";
      
        String[] buttons = { "Continue" };
        JOptionPane.showOptionDialog(FrameworkAccess.getMainFrame(), html,
              "Manual Update Required", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, buttons, buttons[0]);
        
        // Delete any logs to ensure that the install directory is properly cleaned up
        if (uninstaller!=null) {
            log.info("Deleting logs in install directory: "+uninstaller.getParentFile());
            deleteLogs(uninstaller.getParentFile());
        }
        
//        final File uninstallerFile = uninstaller;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {

//                log.info("Executing uninstaller: "+uninstallerFile);
//                if (uninstallerFile!=null && uninstallerFile.exists()) {
//                    // Execute the uninstaller
//                    try {
//                        Runtime.getRuntime().exec(uninstallerFile.getAbsolutePath());
//                    }
//                    catch (IOException e) {
//                        // Ignore. It's too late now!
//                    }
//                }

                Utils.openUrlInBrowser(helpUrl);
            }
        });
        
        LifecycleManager.getDefault().exit(0);
    }

    private static boolean deleteLogs(File dir) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isFile()) {
                    if (dirFile.getName().toLowerCase().endsWith(".log")) {
                        log.info("Delete on exit: "+dirFile);
                        dirFile.deleteOnExit();
                    }
                }
            }
        }
        return true;
    }
    
    private static boolean deleteDirectory(File dir) {
        dir.deleteOnExit();
        log.info("Deleting on exit: "+dir);
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isFile()) {
                    log.info("Delete on exit: "+dirFile);
                    dirFile.deleteOnExit();
                }
                else if (dirFile.isDirectory()) {
                    deleteDirectory(dirFile);
                }
            }
        }
        return true;
    }
    
    private boolean isBricked() {
        
        if (SystemInfo.isDev) return false;
        
        String brickedProp = System.getProperty("brick");
        if ("true".equals(brickedProp)) {
            log.info("Client bricked by system property");
            return true;
        }
        else if ("false".equals(brickedProp)) {
            log.info("Client unbricked by system property");
            return false;
        }
        
        String brickUrl = null;

        try {
            String updateCenterUrl = AutoUpdater.getUpdateCenterURL();
            brickUrl = updateCenterUrl.replace("updates.xml", "brick.xml");
        }
        catch (MissingResourceException e) {
            log.warn("Error finding update center URL", e);
        }
        
        if (brickUrl==null) return false;
        log.info("Checking for brick at {}", brickUrl);
        
        GetMethod method = new GetMethod(brickUrl);
        
        try {
            int responseCode = HttpClientManager.getHttpClient().executeMethod(method);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (method.getResponseBodyAsString().contains("brick")) {
                    log.info("Client bricked by remote brick");
                    return true;
                }
            }
        }
        catch (Exception e) {
            log.error("Error checking brick status", e);
        }
        finally {
            method.releaseConnection();
        }
        
        return false;
    }
    
}
