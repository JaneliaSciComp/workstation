package org.janelia.workstation.integration.spi.gui;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public interface StartPageProvider {

    public static final String LOOKUP_PATH = "Providers/StartPageProvider";

    /**
     * Implement this method to provide a start page.
     */
    <T extends JPanel & PropertyChangeListener> T getStartPage();
}
