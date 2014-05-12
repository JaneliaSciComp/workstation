package org.janelia.it.FlyWorkstation.gui.util.server_status;

/**
 * Title:        Your Product Name
 * Description:  This is the main Browser in the System
 *
 * @author Peter Davies
 */

public class ServerStatusReportManager {

    private static ServerStatusReportManager serverStatusReportManager = new ServerStatusReportManager();
    private ServerStatusReportChecker checker;
    private int interval = 5;

    private ServerStatusReportManager() {
    }

    public static ServerStatusReportManager getReportManager() {
        return serverStatusReportManager;
    }

    public void startCheckingForReport() {
        if (isCheckingForReport()) return;
        checker = new ServerStatusReportChecker(interval);
        Thread rptCheckingThread = new Thread(checker);
        rptCheckingThread.setDaemon(true);
        rptCheckingThread.setPriority(Thread.MIN_PRIORITY);
        rptCheckingThread.start();
    }

    public void stopCheckingForReport() {
        if (!isCheckingForReport()) return;
        checker.stopChecking();
        checker = null;
    }

    public boolean isCheckingForReport() {
        return checker != null;
    }

    public void setCheckInterval(int minutes) {
        interval = minutes;
        if (isCheckingForReport()) checker.setInterval(interval);
    }


}