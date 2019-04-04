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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openide.util.Exceptions;
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
            try {
                if (sampleLocation == null) {
                    throw new IllegalStateException("Null sample location from " + locationProvider.getClass().getName() + ", sent to " + locationAcceptor.getClass().getName());
                }
                locationAcceptor.acceptLocation(sampleLocation);
            } 
            catch (Exception ioe) {
                Exceptions.printStackTrace(ioe);
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
                logger.error(ioe.getMessage());
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
        DefaultViewerLocationAcceptor(Tiled3dSampleLocationProviderAcceptor provider) {
            this.provider = provider;
        }

        @Override
        public void acceptLocation(SampleLocation sampleLocation) throws Exception {
            provider.setSampleLocation(sampleLocation);
        }        
    }
    
}
