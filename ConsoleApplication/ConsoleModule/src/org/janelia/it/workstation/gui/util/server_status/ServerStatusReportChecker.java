package org.janelia.it.workstation.gui.util.server_status;

/**
 * Title:        Your Product Name
 * Description:  This is the main Browser in the System
 * @author Peter Davies
 * @version
 */

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ServerStatusReportChecker implements Runnable {
    private int intervalInMinutes;
    private boolean stop = false;
    private URL reportURL;
    private String lastReport = "";

    public ServerStatusReportChecker(int minutes) {
        intervalInMinutes = minutes;
    }

    public void stopChecking() {
        stop = true;
    }

    public void setInterval(int minutes) {
        intervalInMinutes = minutes;
    }

    public void run() {
        //Sleep for 5 seconds to let the user see the message pop up after the browser is up
        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException ie) {
        }

        while (!stop) {
            if (!stop) {
                checkForReport();
            }

            try {
                Thread.currentThread().sleep(intervalInMinutes * 1000 * 60);
            }
            catch (InterruptedException inEx) {
            } //do nothing here, it is expected l
        }
    }

    private void checkForReport() {
        URL url = getReportURL();
        StringBuffer buffer;
        String line;
        int responseCode;
        HttpURLConnection connection;
        InputStream input;
        BufferedReader dataInput;

        if (url == null) {
            return;
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
            responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Ignoring HTTP response code: " + String.valueOf(responseCode));

                //?? ignore error codes ??
            }

            buffer = new StringBuffer();
            input = connection.getInputStream();
            dataInput = new BufferedReader(new InputStreamReader(input));

            while ((line = dataInput.readLine()) != null) {
                buffer.append(line);
                buffer.append('\n');
            }

            String message = buffer.toString().trim();

            if ((lastReport != null) && (message.hashCode() == lastReport.hashCode() || message.length() == 0)) {
                return;
            }

            JFrame mainFrame = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame();
            JOptionPane optionPane = new JOptionPane();
            optionPane.showMessageDialog(mainFrame, message, "New Server Status Message", JOptionPane.INFORMATION_MESSAGE);
            lastReport = message;
        }
        catch (Exception ex) {
            //do nothing because it wasn't found?
        }
    }

    private URL getReportURL() {
        if (reportURL != null) {
            return reportURL;
        }

        String appServer = System.getProperty("console.HttpServer");

        if (appServer != null) {
            appServer = "http://" + appServer;

            try {
                reportURL = new URL(appServer + "/broadcast/Status.jsp");

                return reportURL;
            }
            catch (Exception ex) {
                return null;
            } //cannot determine emailServer URL
        }

        return null;
    }
}