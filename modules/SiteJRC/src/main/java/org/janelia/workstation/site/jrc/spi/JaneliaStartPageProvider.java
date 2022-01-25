package org.janelia.workstation.site.jrc.spi;

import org.janelia.workstation.integration.spi.gui.StartPageProvider;
import org.janelia.workstation.site.jrc.gui.StartPage;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * Provides a start page for the internal JRC application.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = StartPageProvider.class, path=StartPageProvider.LOOKUP_PATH)
public class JaneliaStartPageProvider implements StartPageProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends JPanel & PropertyChangeListener> T getStartPage() {
        return (T) new StartPage();
    }
}
