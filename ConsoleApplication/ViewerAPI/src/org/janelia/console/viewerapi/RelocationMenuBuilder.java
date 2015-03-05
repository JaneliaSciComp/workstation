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
            
    public List<JMenuItem> buildSyncMenu(Collection<Tiled3dSampleLocationProvider> locationProviders, ViewerLocationAcceptor acceptor) {
        List<JMenuItem> rtnVal = new ArrayList<>();
        for (final Tiled3dSampleLocationProvider provider : locationProviders) {
            if (logger.isLoggable(Level.INFO))
                logger.info("Adding menu item for " + provider.getProviderUniqueName());
            final String description = provider.getProviderDescription();
            JMenu synchronizeMenu = new JMenu("Synchronize with " + description);
            rtnVal.add(synchronizeMenu);
            synchronizeMenu.add(new SampleRelocateAction(description, provider, acceptor));
            synchronizeMenu.add(new AbstractAction("Synchronize with " + description + " always") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
        }
        rtnVal.add(new JMenuItem(new AbstractAction("Desynchronize") {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }));
        
        return rtnVal;
    }

    private class SampleRelocateAction extends AbstractAction {

        private Tiled3dSampleLocationProvider provider;
        private ViewerLocationAcceptor acceptor;

        public SampleRelocateAction(String description, Tiled3dSampleLocationProvider provider, ViewerLocationAcceptor acceptor) {
            super("Synchronize with " + description + " now");
            this.provider = provider;
            this.acceptor = acceptor;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            double[] focusCoords = provider.getCoords();
            URL focusUrl = provider.getSampleUrl();
            try {
                acceptor.acceptLocation(focusUrl, focusCoords);
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

}
