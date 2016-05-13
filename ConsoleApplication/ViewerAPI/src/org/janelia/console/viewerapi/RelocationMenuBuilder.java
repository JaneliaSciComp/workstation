/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.console.viewerapi;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.openide.util.Exceptions;

/**
 * Builds menus suitable for relocating a viewer to synchronize with another.
 * @author fosterl
 */
public class RelocationMenuBuilder {
    // NOTE: This may be switched to Slf4j or Log4j at CB's discretion.
    private Logger logger = Logger.getLogger(RelocationMenuBuilder.class.getSimpleName());
            
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
//            if (logger.isLoggable(Level.INFO))
//                logger.info("Adding menu item for " + providerAcceptor.getProviderUniqueName());
            final String description = providerAcceptor.getProviderDescription();
            /*
            JMenu synchronizeMenu = new JMenu("Synchronize with " + description);
            rtnVal.add(synchronizeMenu);
            if (providerAcceptor.getParticipantType().equals(Tiled3dSampleLocationProviderAcceptor.ParticipantType.both)  ||
                providerAcceptor.getParticipantType().equals(Tiled3dSampleLocationProviderAcceptor.ParticipantType.provider)) {
                synchronizeMenu.add(new AcceptSampleLocationAction(description, providerAcceptor, viewerLocationAcceptor));
            }
            /*
            synchronizeMenu.add(new AbstractAction("Synchronize with " + description + " always") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
             */
            // Here, the provider is being inverted, to accept the new location.
            if (providerAcceptor.getParticipantType().equals(Tiled3dSampleLocationProviderAcceptor.ParticipantType.both)  ||
                providerAcceptor.getParticipantType().equals(Tiled3dSampleLocationProviderAcceptor.ParticipantType.acceptor)) {
                rtnVal.add(new JMenuItem(new PushSampleLocationAction(description, providerAcceptor, originator)));
                
                // Tantalize user with future option to continuously synchronize
                /*
                JCheckBoxMenuItem synchronizeMenu = new JCheckBoxMenuItem("Continuously Track Location in "+description, false);
                synchronizeMenu.setEnabled(false); // grayed out for now
                rtnVal.add(synchronizeMenu);
                */
            }
        }
        
        /*
        rtnVal.add(new JMenuItem(new AbstractAction("Desynchronize") {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
               
        })); */
        
        return rtnVal;
    }

    private class PushSampleLocationAction extends AbstractAction {
        private ViewerLocationAcceptor locationAcceptor;
        private Tiled3dSampleLocationProviderAcceptor locationProvider;

        public PushSampleLocationAction(String description, Tiled3dSampleLocationProviderAcceptor locationAcceptor, Tiled3dSampleLocationProviderAcceptor locationProvider) {
            super("Navigate to This Location in " + description);
            this.locationAcceptor = new DefaultViewerLocationAcceptor(locationAcceptor);  
            this.locationProvider = locationProvider;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SampleLocation sampleLocation = locationProvider.getSampleLocation();
            double[] focusCoords = new double[] {
                sampleLocation.getFocusXUm(),
                sampleLocation.getFocusYUm(),
                sampleLocation.getFocusZUm()
            };
            try {
                locationAcceptor.acceptLocation(sampleLocation);
                if (focusCoords == null) {
                    logger.info("Null Coords from " + locationProvider.getClass().getName() + ", sent to " + locationAcceptor.getClass().getName());
                }
            } catch (Exception ioe) {
                logger.severe(ioe.getMessage());
                Exceptions.printStackTrace(ioe);
                JOptionPane.showMessageDialog(
                        null,
                        "Check that required files exist in synch source"
                );
            }
        }
    }
    
    private class AcceptSampleLocationAction extends AbstractAction {

        private Tiled3dSampleLocationProviderAcceptor provider;
        private ViewerLocationAcceptor acceptor;

        public AcceptSampleLocationAction(String description, Tiled3dSampleLocationProviderAcceptor provider, ViewerLocationAcceptor acceptor) {
            super("Accept location from " + description + " now");
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
                logger.severe(ioe.getMessage());
                Exceptions.printStackTrace(ioe);
                JOptionPane.showMessageDialog(
                        null,
                        "Check that required files exist in synch source"
                );
            }
        }
    }
    
    private class DefaultViewerLocationAcceptor implements ViewerLocationAcceptor {
        private Tiled3dSampleLocationProviderAcceptor provider;
        public DefaultViewerLocationAcceptor(Tiled3dSampleLocationProviderAcceptor provider) {
            this.provider = provider;
        }

        @Override
        public void acceptLocation(SampleLocation sampleLocation) throws Exception {
            provider.setSampleLocation(sampleLocation);
        }        
    }
    
}
