package org.janelia.it.workstation.gui.browser.gui.hud;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.gui.framework.viewer.Load3dSwingWorker;

/**
 * Created by IntelliJ IDEA.
 * User: fosterl
 * Date: 11/13/12
 * Time: 5:18 PM
 *
 * A kind of controller class. We use this for setting up HUD's 3D with new info.
 */
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Special swing-worker for HUD 3D image load. */
public class Hud3DController implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(Hud3DController.class);
    
    private final Hud hud;
    private final Mip3d mip3d;

    private String filename;
    private JLabel busyLabel;
    private Load3dSwingWorker load3dSwingWorker;

    public Hud3DController(Hud hud, Mip3d mip3d) {
        this.hud = hud;
        this.mip3d = mip3d;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractButton aButton = (AbstractButton)e.getSource();
        if ( aButton.getActionCommand().equals( Hud.THREE_D_CONTROL ) ) {
            hud.handleRenderSelection();
        }
    }

    public void set3dWidget() {
        hud.add( mip3d, BorderLayout.CENTER );
    }

    /** Hook for re-loading. */
    public void load3d() {
        load3dSwingWorker = new Load3dSwingWorker( mip3d, filename ) {
            public void filenameSufficient() {
                Hud3DController.this.restoreMip3dToUi();
            }
            public void filenameUnavailable() {
                Hud3DController.this.no3DAvailable();
            }
        };
        load3dSwingWorker.execute();
    }

    /** Hook for seeing the reload-result. */
    public Boolean isDirty() throws Exception {
        if ( load3dSwingWorker != null ) {
            return load3dSwingWorker.get();
        }
        else {
            // Will return false, indicating not-dirty.
            return false;
        }
    }

    /**
     * This sets the UI busy, regardless whether invoked within event-displatch, or whether 'busy' has been done
     * before this point in time.
     */
    public void setUiBusyMode() {
        if ( SwingUtilities.isEventDispatchThread() ) {
            markBusy();
        }
        else {
            Runnable r = new Runnable() {
                public void run() {
                    markBusy();
                }
            };
            try {
                SwingUtilities.invokeAndWait( r );
            } 
            catch ( Exception ex ) {
                // Showing the exception.  Not alerting user that set-to-busy actually failed.
                log.error("Error marking the UI as busy",ex);
            }
        }
    }
    
    public void entityUpdate() {
        filename = hud.getFast3dFile();
        hud.set3dModeEnabled(filename != null);
        hud.handleRenderSelection();
    }

    public boolean is3DReady() {
        return filename != null;
    }

    private void restoreMip3dToUi() {
        // THIS verifies same performance in this environment as standalone. LLF
        //filename = "/Volumes/jacsData/brunsTest/3d_test_images/ConsolidatedSignal2_25.v3dpbd";
        if ( filename != null ) {
            hud.remove(busyLabel);
            hud.add(mip3d, BorderLayout.CENTER);
            hud.validate();
            hud.repaint();
        }
        else {
            no3DAvailable();
        }
    }

    private void no3DAvailable() {
        JOptionPane.showMessageDialog(hud, "No 3D file found. Reverting to 2D.");
        hud.set3dModeEnabled(false);
        hud.handleRenderSelection();
    }

    private void markBusy() {
        // Testing existance, and removing the busy label here implies this method can be called state-ignorant.
        if ( busyLabel == null ) {
            busyLabel = new JLabel(Icons.getLoadingIcon());
        }
        hud.remove(busyLabel);
        hud.remove(mip3d);
        busyLabel.setPreferredSize(hud.getSize());
        busyLabel.setSize( hud.getSize() );
        hud.add(busyLabel, BorderLayout.CENTER);
        //Q: pack moves to background-Z? hud.pack();
        hud.validate();
        hud.repaint();
    }

}
