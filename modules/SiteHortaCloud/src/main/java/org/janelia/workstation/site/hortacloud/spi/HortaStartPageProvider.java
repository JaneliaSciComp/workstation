package org.janelia.workstation.site.hortacloud.spi;

import org.janelia.workstation.integration.spi.gui.StartPageProvider;
import org.janelia.workstation.site.hortacloud.gui.StartPage;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * Provides a start page for the HortaCloud application.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = StartPageProvider.class, path=StartPageProvider.LOOKUP_PATH)
public class HortaStartPageProvider implements StartPageProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends JPanel & PropertyChangeListener> T getStartPage() {
        return (T) new StartPage();
    }
}
