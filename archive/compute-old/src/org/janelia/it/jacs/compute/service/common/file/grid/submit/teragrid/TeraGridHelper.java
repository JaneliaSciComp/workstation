
package src.org.janelia.it.jacs.compute.service.common.file.grid.submit.teragrid;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 11, 2009
 * Time: 4:25:31 PM
 */
public class TeraGridHelper {

    public static void runTgCmd(String cmd, File captureFile, Logger logger, int retries) throws Exception {
        runTgCmd(cmd + " > " + captureFile.getAbsolutePath(), logger, retries);
    }

    public static void runTgCmd(String cmd, Logger logger, int retries) throws Exception {
        String tgCmd = "";
        int i;
        for (i = 0; i < retries; i++) {
            try {
                SystemCall sc = new SystemCall(null, getScratchDir(), logger);
                tgCmd = "ssh " + getTgSshPath() + " " + cmd + "\n";
                int ev = sc.execute(tgCmd, false);
                if (ev != 0) {
                    throw new Exception("SystemCall produced non-zero exit value=" + tgCmd);
                }
                sc.cleanup();
                break;
            }
            catch (Exception ex) {
                logger.info("Exception during tgCmd=" + tgCmd + " retry=" + i + " of " + retries);
                Thread.sleep(5000);
            }
        }
        if (i >= retries) {
            throw new Exception("Exhausted all retries for tgCmd=" + tgCmd);
        }
    }

    public static void copyFileToTg(File source, String targetName, Logger logger) throws Exception {
        SystemCall sc = new SystemCall(null, getScratchDir(), logger);
        String copyCmd = "scp " + source.getAbsolutePath() + " " + getTgSshPath() + ":" + targetName + "\n";
        int ev = sc.execute(copyCmd, false);
        if (ev != 0) {
            throw new Exception("Copy file to TG failed.  Exit value=" + copyCmd);
        }
        sc.cleanup();
    }

    public static void copyFileToTg(File source, String targetName, Logger logger, int retries) throws Exception {
        int i;
        for (i = 0; i < retries; i++) {
            try {
                copyFileToTg(source, targetName, logger);
                // assume success
                break;
            }
            catch (Exception ex) {
                logger.info("Exception during try=" + i + " of " + retries + " to copy file to tg");
                Thread.sleep(5000);
            }
        }
        if (i >= retries) {
            throw new Exception("Exhausted all retries in attempt to copy file to tg for source=" + source.getAbsolutePath() + " target=" + targetName);
        }
    }

    public static void copyFileFromTg(String sourceFilename, File targetFile, Logger logger, int retries) throws Exception {
        int i;
        String copyCmd = "";
        for (i = 0; i < retries; i++) {
            try {
                SystemCall sc = new SystemCall(null, getScratchDir(), logger);
                copyCmd = "scp " + getTgSshPath() + ":" + sourceFilename + " " + targetFile.getAbsolutePath() + "\n";
                int ev = sc.execute(copyCmd, false);
                if (ev != 0) {
                    throw new Exception("SystemCall produced non-zero exit value=" + copyCmd);
                }
                sc.cleanup();
                break;
            }
            catch (Exception ex) {
                logger.info("Exception during cmd=" + copyCmd + " retry=" + i + " of " + retries);
                Thread.sleep(5000);
            }
        }
        if (i >= retries) {
            throw new Exception("Exhausted all retries in attempt to copy file from teragrid cmd=" + copyCmd);
        }
    }

    public static boolean tgFileExists(String filePath, Logger logger, int retries) throws Exception {
        int i;
        String tgCmd = "";
        for (i = 0; i < retries; i++) {
            try {
                SystemCall sc = new SystemCall(null, getScratchDir(), logger);
                tgCmd = "ssh " + getTgSshPath() + " ls " + filePath + "\n";
                int ev = sc.execute(tgCmd, false);
                sc.cleanup();
                return ev == 0;
            }
            catch (Exception ex) {
                logger.info("Exception during cmd=" + tgCmd + " retry=" + i + " of " + retries);
                Thread.sleep(5000);
            }
        }
        if (i >= retries) {
            throw new Exception("Exhausted all retries in attempt to see if file exists cmd=" + tgCmd);
        }
        return false;
    }

    private static File getScratchDir() {
        return new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir"));
    }

    private static String getTgSshPath() {
        return SystemConfigurationProperties.getString("TeraGrid.SshPath");

    }
}
