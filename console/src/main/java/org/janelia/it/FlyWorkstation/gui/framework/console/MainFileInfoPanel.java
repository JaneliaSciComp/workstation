package org.janelia.it.FlyWorkstation.gui.framework.console;

import sun.awt.VerticalBagLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/5/11
 * Time: 4:15 PM
 */
public class MainFileInfoPanel extends JPanel {
    GridLayout experimentLayout = new GridLayout(0,2);
    JPanel compsToExperiment = new JPanel();
    // todo Need a default image here
    public static final String DEFAULT_IMAGE_PATH = "/Users/saffordt/Dev/FlyWorkstation/console/target/classes/org/janelia/it/flyscope.jpg";

    public MainFileInfoPanel() {
        super(new VerticalBagLayout());
        compsToExperiment.setLayout(experimentLayout);
        add(compsToExperiment, BorderLayout.NORTH);
        reloadData(DEFAULT_IMAGE_PATH);
    }

    public void reloadData(String pathToData) {
        try {
            System.out.println("reload");
            if (null==pathToData) {
                // Do nothing
                return;
            }
            File tmpFile = new File(pathToData);
            if (tmpFile.isDirectory()) {
                File[] childImageFiles = tmpFile.listFiles(new FilenameFilter(){
                    public boolean accept(File file, String s) {
                        // todo Need a whole mechanism to categorize the files and editors used for them.
                        return s.endsWith(".tif");
                    }
                });
                displayImages(childImageFiles);
            }
            else if (tmpFile.isFile()) {
                displayImages(new File[]{tmpFile});
            }
            // else do nothing
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void displayImages(File[] childImageFiles) throws MalformedURLException {
        compsToExperiment.removeAll();
        for (File childImageFile : childImageFiles) {
            JPanel tmpPanel = new JPanel();
            JToggleButton tmpButton = new JToggleButton(childImageFile.getName(), new ImageIcon(new URL(childImageFile.toURL().toString())));
            JLabel tmpButtonLabel = new JLabel(childImageFile.getName(), new ImageIcon(new URL(childImageFile.toURL().toString()))
                    , JLabel.CENTER);
            tmpPanel.add(tmpButtonLabel);
            tmpPanel.add(tmpButton);
            compsToExperiment.add(tmpPanel);
        }
        compsToExperiment.getParent().repaint();
    }

}
