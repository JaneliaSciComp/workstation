package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.janelia.it.workstation.cache.large_volume.EHCacheFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 *
 * @author fosterl
 */
public class Cache3DSettings {
    public void prompt() {
        final JDialog popup = new JDialog(); // Get parent;
        popup.setSize( 400, 500 );
        centerOnScreen( popup, true );

        final JTextField neighborhoodSize = new JTextField(10);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                boolean valueChosen = false;
                while (! valueChosen) {
                    try {
                        Integer size = Integer.parseInt(neighborhoodSize.getText().trim());
                        SessionMgr.getSessionMgr().setModelProperty(EHCacheFacade.CACHE_NAME, size);
                        popup.setVisible(false);
                        
                        valueChosen = true;
                    } catch (NumberFormatException nfe) {
                        neighborhoodSize.setText("");
                    }
                }
            }            
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                popup.setVisible(false);
            }
        });
        
        buttonPanel.add( okButton, BorderLayout.CENTER );
        buttonPanel.add( cancelButton, BorderLayout.WEST );
        
        popup.setLayout( new BorderLayout() );
        popup.add( buttonPanel, BorderLayout.SOUTH );
        popup.add( neighborhoodSize, BorderLayout.CENTER );
        
        popup.setVisible(true);
    }
    
    /**
     * See http://stackoverflow.com/questions/213266/how-do-i-center-a-jdialog-on-screen
     * @param c what to center.
     * @param absolute right at center, or 25% from corner.
     */
    public void centerOnScreen(final JDialog c, final boolean absolute) {
        final int width = c.getWidth();
        final int height = c.getHeight();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width / 2) - (width / 2);
        int y = (screenSize.height / 2) - (height / 2);
        if (!absolute) {
            x /= 2;
            y /= 2;
        }
        c.setLocation(x, y);
    }    
}
