package org.janelia.console.viewerapi;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds menus suitable for relocating a viewer to synchronize with another.
 * @author fosterl
 */
public class RelocationMenuBuilder {
    // NOTE: This may be switched to Slf4j or Log4j at CB's discretion.
    private Logger logger = LoggerFactory.getLogger(RelocationMenuBuilder.class);
            
    /**
     * Helper to build up a menu of other-side targets for sychronization.
     * @param locationProviderAcceptors list of all "other" providers (not I).
     * @param originator I, as a location acceptor.
     * @param viewerLocationAcceptor externally-created acceptor.  Probably 'I'.
     * @return the menu items thus produced.
     */
    public List<JMenuItem> buildSyncMenu(Collection<Tiled3dSampleLocationProviderAcceptor> locationProviderAcceptors, Tiled3dSampleLocationProviderAcceptor originator, ViewerLocationAcceptor viewerLocationAcceptor) {
        List<JMenuItem> rtnVal = new ArrayList<>();
        for (final Tiled3dSampleLocationProviderAcceptor providerAcceptor : locationProviderAcceptors) {
            final String description = providerAcceptor.getProviderDescription();
            // kludge to get around syncing issues
            if (description.equals("Horta - Focus On Location")) {
                continue;
            }
            
            // Here, the provider is being inverted, to accept the new location.
            if (providerAcceptor.getParticipantType().equals(Tiled3dSampleLocationProviderAcceptor.ParticipantType.both)  ||
                providerAcceptor.getParticipantType().equals(Tiled3dSampleLocationProviderAcceptor.ParticipantType.acceptor)) {
                rtnVal.add(new JMenuItem(new PushSampleLocationAction(description, providerAcceptor, originator)));
            }
        }
        return rtnVal;
    }

    private class PushSampleLocationAction extends AbstractAction {
        private String description;
        private ViewerLocationAcceptor locationAcceptor;
        private Tiled3dSampleLocationProviderAcceptor locationProvider;

        public PushSampleLocationAction(String description, Tiled3dSampleLocationProviderAcceptor locationAcceptor, Tiled3dSampleLocationProviderAcceptor locationProvider) {
            super("Navigate to This Location in " + description);
            this.description = description;
            this.locationAcceptor = new DefaultViewerLocationAcceptor(locationAcceptor);  
            this.locationProvider = locationProvider;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SampleLocation sampleLocation = locationProvider.getSampleLocation();
            try {
                if (sampleLocation == null) {
                    throw new IllegalStateException("Null sample location from " + locationProvider.getClass().getName() + ", sent to " + locationAcceptor.getClass().getName());
                }
                locationAcceptor.acceptLocation(sampleLocation);
            } 
            catch (Exception ioe) {
                logger.warn("Failed to navigate to location in "+description, ioe);
                JOptionPane.showMessageDialog(
                        null,
                        "Navigation failed. Check that required files exist in synch source.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private class AcceptSampleLocationAction extends AbstractAction {

        private String description;
        private Tiled3dSampleLocationProviderAcceptor provider;
        private ViewerLocationAcceptor acceptor;

        public AcceptSampleLocationAction(String description, Tiled3dSampleLocationProviderAcceptor provider, ViewerLocationAcceptor acceptor) {
            super("Accept location from " + description + " now");
            this.description = description;
            this.provider = provider;
            this.acceptor = acceptor;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SampleLocation sampleLocation = provider.getSampleLocation();
            if (sampleLocation == null) {
                JOptionPane.showMessageDialog(null, "No sample location available. Please check that " + provider.getProviderDescription() + " is available.");
                return;
            }
            double[] focusCoords = new double[] {
                sampleLocation.getFocusXUm(),
                sampleLocation.getFocusYUm(),
                sampleLocation.getFocusZUm()
            };
           try {
                acceptor.acceptLocation(sampleLocation);
            } catch (Exception ioe) {
                logger.warn("Failed to accept location from "+description, ioe);
                JOptionPane.showMessageDialog(
                        null,
                        "Check that required files exist in synch source"
                );
            }
        }
    }
    
    private class DefaultViewerLocationAcceptor implements ViewerLocationAcceptor {
        private Tiled3dSampleLocationProviderAcceptor provider;
        DefaultViewerLocationAcceptor(Tiled3dSampleLocationProviderAcceptor provider) {
            this.provider = provider;
        }

        @Override
        public void acceptLocation(SampleLocation sampleLocation) throws Exception {
            provider.setSampleLocation(sampleLocation);
        }        
    }
    
}
