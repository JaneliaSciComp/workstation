package org.janelia.it.FlyWorkstation.shared.util.text_component;

import javax.swing.*;
import javax.swing.text.Document;


/**
 * Title:        Standard Text Field to be used
 * Description:  Wherever in the tool you wish to use a text field widget,
 *               use this instead of JTextField.
 */
public class StandardTextField extends JTextField {
    //---------------------------------------CONSTRUCTORS

    /** Constructors merely pass through all params to super, and run init steps. */
    public StandardTextField() {
        super();
        commonInitializer();
    } // End constructor

    public StandardTextField(Document lDoc, String lText, int lColumns) {
        super(lDoc, lText, lColumns);
        commonInitializer();
    } // End constructor

    public StandardTextField(String lText, int lColumns) {
        super(lText, lColumns);
        commonInitializer();
    } // End constructor

    public StandardTextField(String lText) {
        super(lText);
        commonInitializer();
    } // End constructor

    public StandardTextField(int lColumns) {
        super(lColumns);
        commonInitializer();
    } // End constructor

    //---------------------------------------HELPERS

    /** Initialization steps common to all constructors. */
    private void commonInitializer() {
        new DataTransferMouseListener(this);
    } // End method: commonInitializer
} // End class: StandardTextField
