/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.FlyWorkstation.gui.dataview;

import javax.swing.*;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewApp {

    private static DataviewFrame mainFrame;

    public static DataviewFrame getMainFrame() {
        return mainFrame;
    }

    public static void main(final String[] args) throws Exception {
        newDataviewer();
    }

    private static void newDataviewer() throws Exception {

        mainFrame = new DataviewFrame();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setTitle("JACS Data Viewer");
        mainFrame.pack();
        mainFrame.setVisible(true);
        //Thread.sleep(3000);
    }
}
