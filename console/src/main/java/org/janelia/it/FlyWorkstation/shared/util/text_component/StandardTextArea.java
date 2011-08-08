package org.janelia.it.FlyWorkstation.shared.util.text_component;

import javax.swing.*;
import javax.swing.text.Document;

/**
 * Title:        Standard Text Area
 * Description:  use this instead of JTextArea
 */

public class StandardTextArea extends JTextArea {

    //---------------------------------------UNIT-TEST CODE

    /**
     * Presents an instance of this widget to play with.
     */
    public static void main(String[] lArgs) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex) {
        }
        JTextArea lArea = new StandardTextArea();
        lArea.setText("Much ado about nothing");
        lArea.setEditable(false);
        JFrame lFrame = new JFrame("Try Right-Click");
        lFrame.setSize(400, 400);
        lFrame.setLocation(100, 100);
        lFrame.getContentPane().add(lArea);
        lFrame.setVisible(true);
        lFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent we) {
                System.exit(0);
            }
        });
    } // End main

    //---------------------------------------CONSTRUCTORS

    /**
     * Constructors simply call standard initialization.
     */
    public StandardTextArea() {
        super();
        commonInitializer();
    } // End constructor

    public StandardTextArea(String lText) {
        super(lText);
        commonInitializer();
    } // End constructor

    public StandardTextArea(Document lDoc) {
        super(lDoc);
        commonInitializer();
    } // End constructor

    public StandardTextArea(Document lDoc, String lText, int lRows, int lColumns) {
        super(lDoc, lText, lRows, lColumns);
        commonInitializer();
    } // End constructor

    public StandardTextArea(int lRows, int lColumns) {
        super(lRows, lColumns);
        commonInitializer();
    } // End constructor

    public StandardTextArea(String lText, int lRows, int lColumns) {
        super(lText, lRows, lColumns);
        commonInitializer();
    } // End constructor

    //---------------------------------------HELPERS

    /**
     * Initialization steps common to all constructors.
     */
    private void commonInitializer() {
        new DataTransferMouseListener(this);
    } // End method: commonInitializer

} // End class: StandardTextArea
